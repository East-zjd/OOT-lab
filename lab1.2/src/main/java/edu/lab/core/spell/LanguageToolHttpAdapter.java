package edu.lab.core.spell;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通过 LanguageTool 公共 HTTP API 的拼写检查适配器。
 */
public final class LanguageToolHttpAdapter implements SpellCheckService {
    private static final Pattern MATCH_PATTERN = Pattern.compile(
            "\"offset\"\\s*:\\s*(\\d+).*?\"length\"\\s*:\\s*(\\d+).*?\"replacements\"\\s*:\\s*\\[(.*?)\\]",
            Pattern.DOTALL
    );
    private static final Pattern REPLACEMENT_PATTERN = Pattern.compile("\"value\"\\s*:\\s*\"(.*?)\"");

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

    private static List<SpellCheckIssue> parseIssues(String text, String json) {
        List<SpellCheckIssue> issues = new ArrayList<>();
        Matcher matcher = MATCH_PATTERN.matcher(json);
        while (matcher.find()) {
            int offset = Integer.parseInt(matcher.group(1));
            int length = Integer.parseInt(matcher.group(2));
            String replacements = matcher.group(3);
            String suggestion = firstReplacementOrEmpty(replacements);
            String word = safeSubstring(text, offset, length);
            LineCol lc = offsetToLineCol(text, offset);
            issues.add(new SpellCheckIssue(lc.line, lc.col, word, suggestion));
        }
        return issues;
    }

    private static String firstReplacementOrEmpty(String replacements) {
        Matcher matcher = REPLACEMENT_PATTERN.matcher(replacements);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
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

    private record LineCol(int line, int col) {
    }
}
