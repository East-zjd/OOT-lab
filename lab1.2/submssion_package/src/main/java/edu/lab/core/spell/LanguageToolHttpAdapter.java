package edu.lab.core.spell;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 通过 LanguageTool 公共 HTTP API 的拼写检查适配器。
 */
public final class LanguageToolHttpAdapter implements SpellCheckService {
    private final URI endpoint;
    private final HttpClient client;
    private final String language;

    public LanguageToolHttpAdapter(URI endpoint, HttpClient client, String language) {
        this.endpoint = endpoint;
        this.client = client;
        this.language = language;
    }

    @Override
    public List<SpellCheckIssue> checkLines(List<String> lines) {
        String text = String.join("\n", lines);
        if (text.isBlank()) {
            return List.of();
        }
        String body = "language=" + encode(language) + "&text=" + encode(text);
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return List.of();
            }
            return parseIssues(text, response.body());
        } catch (IOException | InterruptedException e) {
            return List.of();
        }
    }

    static List<SpellCheckIssue> parseIssues(String text, String json) {
        List<SpellCheckIssue> issues = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return issues;
        }

        JsonElement rootEl;
        try {
            rootEl = JsonParser.parseString(json);
        } catch (Exception e) {
            return issues;
        }
        if (!rootEl.isJsonObject()) {
            return issues;
        }

        JsonObject root = rootEl.getAsJsonObject();
        JsonArray matches = root.getAsJsonArray("matches");
        if (matches == null) {
            return issues;
        }

        for (JsonElement matchEl : matches) {
            if (!matchEl.isJsonObject()) {
                continue;
            }
            JsonObject match = matchEl.getAsJsonObject();

            // 仅报告 misspelling（若服务端给出了 issueType）。
            JsonObject rule = match.has("rule") && match.get("rule").isJsonObject() ? match.getAsJsonObject("rule") : null;
            if (rule != null && rule.has("issueType")) {
                String issueType = safeGetString(rule.get("issueType"));
                if (issueType != null && !"misspelling".equalsIgnoreCase(issueType)) {
                    continue;
                }
            }

            Integer offset = safeGetInt(match.get("offset"));
            Integer length = safeGetInt(match.get("length"));
            if (offset == null || length == null) {
                continue;
            }

            String suggestion = firstReplacementOrEmpty(match.get("replacements"));
            String word = safeSubstring(text, offset, length);
            LineCol lc = offsetToLineCol(text, offset);
            issues.add(new SpellCheckIssue(lc.line, lc.col, word, suggestion));
        }
        return issues;
    }

    private static String firstReplacementOrEmpty(JsonElement replacementsEl) {
        if (replacementsEl == null || !replacementsEl.isJsonArray()) {
            return "";
        }
        JsonArray arr = replacementsEl.getAsJsonArray();
        if (arr.isEmpty()) {
            return "";
        }
        JsonElement first = arr.get(0);
        if (!first.isJsonObject()) {
            return "";
        }
        JsonObject obj = first.getAsJsonObject();
        return safeGetString(obj.get("value")) == null ? "" : safeGetString(obj.get("value"));
    }

    private static String safeSubstring(String text, int offset, int length) {
        if (offset < 0 || offset >= text.length() || length <= 0) {
            return "";
        }
        int end = Math.min(text.length(), offset + length);
        return text.substring(offset, end);
    }

    private static LineCol offsetToLineCol(String text, int offset) {
        int line = 1;
        int col = 1;
        for (int i = 0; i < text.length() && i < offset; i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                line++;
                col = 1;
            } else {
                col++;
            }
        }
        return new LineCol(line, col);
    }

    private static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    private static Integer safeGetInt(JsonElement el) {
        try {
            if (el == null) {
                return null;
            }
            return el.getAsInt();
        } catch (Exception e) {
            return null;
        }
    }

    private static String safeGetString(JsonElement el) {
        try {
            if (el == null) {
                return null;
            }
            return el.getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private record LineCol(int line, int col) {
    }
}
