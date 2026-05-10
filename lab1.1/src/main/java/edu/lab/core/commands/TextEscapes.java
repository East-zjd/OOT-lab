package edu.lab.core.commands;

import java.util.Map;

final class TextEscapes {
    private TextEscapes() {
    }

    /**
     * 文本转义映射表：只对“编辑文本参数”支持的转义做替换。
     * <p>
     * 不认识的转义会原样保留（例如："\\q" -> "\\q"）。
     */
    private static final Map<Character, Character> ESCAPES = Map.of(
            'n', '\n',
            't', '\t',
            '"', '"',
            '\\', '\\'
    );

    /**
     * 解码编辑类命令中的文本转义。
     * <p>
     * 支持：\n、\t、\"、\\。
     */
    static String unescape(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                Character repl = ESCAPES.get(n);
                if (repl != null) {
                    out.append(repl);
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }
}
