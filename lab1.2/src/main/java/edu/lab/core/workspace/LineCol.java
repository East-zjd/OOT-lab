package edu.lab.core.workspace;

/**
 * 行列位置（1-based）。
 *
 * @param line 行号（从 1 开始）
 * @param col  列号（从 1 开始）
 */
public record LineCol(int line, int col) {
    public LineCol {
        if (line < 1 || col < 1) {
            throw new IllegalArgumentException("line/col must be >= 1");
        }
    }

    /**
     * 解析形如 "line:col" 的字符串。
     */
    public static LineCol parse(String s) {
        String[] parts = s.split(":", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("invalid line:col: " + s);
        }
        return new LineCol(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}
