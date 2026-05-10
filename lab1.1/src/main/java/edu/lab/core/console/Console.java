package edu.lab.core.console;

/**
 * 控制台抽象。
 * <p>
 * 用于隔离 I/O，便于测试时替换为假控制台。
 */
public interface Console {
    /**
     * 输出但不换行。
     */
    void print(String s);

    /**
     * 输出并换行。
     */
    void println(String s);

    /**
     * 读取一行输入。
     *
     * @return 不包含行尾换行符的文本；若输入被关闭则返回 null
     */
    String readLine();
}
