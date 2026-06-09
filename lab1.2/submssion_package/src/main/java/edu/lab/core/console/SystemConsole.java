package edu.lab.core.console;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * 基于系统标准输入/输出的 {@link Console} 实现。
 */
public final class SystemConsole implements Console {
    // 用 BufferedReader 读取标准输入，提高按行读取的便利性
    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    @Override
    public void print(String s) {
        System.out.print(s);
    }

    @Override
    public void println(String s) {
        System.out.println(s);
    }

    @Override
    public String readLine() {
        try {
            return reader.readLine();
        } catch (Exception e) {
            // 读取失败或输入关闭时，按 null 处理
            return null;
        }
    }
}
