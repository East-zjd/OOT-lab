package edu.lab.core.commands;

import java.util.ArrayList;
import java.util.List;

/**
 * 命令行分词器。
 * <p>
 * 将一行用户输入拆分成 token，支持双引号包裹的参数，并支持在引号内对 \" 与 \\ 进行转义。
 */
final class CommandLineTokenizer {
    private CommandLineTokenizer() {
    }

    /**
     * 解析原始命令行，返回命令名与参数。
     */
    static ParsedCommand parse(String rawLine) {
        List<String> tokens = tokenize(rawLine);
        if (tokens.isEmpty()) {
            return new ParsedCommand("", List.of());
        }
        return new ParsedCommand(tokens.get(0), tokens.subList(1, tokens.size()));
    }

    /**
     * 将命令行拆成 token。
     * <p>
     * 规则：空白分隔；双引号内允许空白；引号内支持 \" 与 \\。
     */
    static List<String> tokenize(String rawLine) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        boolean wasQuotedToken = false;

        for (int i = 0; i < rawLine.length(); i++) {
            char c = rawLine.charAt(i);
            if (inQuotes) {
                // 引号内：只处理转义和结束引号
                if (c == '\\' && i + 1 < rawLine.length()) {
                    char n = rawLine.charAt(i + 1);
                    if (n == '"') {
                        cur.append('"');
                        i++;
                        continue;
                    }
                    if (n == '\\') {
                        cur.append('\\');
                        i++;
                        continue;
                    }
                }
                if (c == '"') {
                    inQuotes = false;
                    wasQuotedToken = true;
                    continue;
                }
                cur.append(c);
                continue;
            }

            if (Character.isWhitespace(c)) {
                // 空白：结束当前 token（注意：纯引号 token 也应该被加入）
                if (cur.length() != 0 || wasQuotedToken) {
                    out.add(cur.toString());
                    cur.setLength(0);
                    wasQuotedToken = false;
                }
                continue;
            }

            if (c == '"') {
                // 进入引号模式
                inQuotes = true;
                continue;
            }

            cur.append(c);
        }

        if (inQuotes) {
            // 引号未闭合属于语法错误
            throw new IllegalArgumentException("unterminated quote");
        }

        if (cur.length() != 0 || wasQuotedToken) {
            // 收尾：把最后一个 token 加入输出
            out.add(cur.toString());
        }

        return out;
    }
}
