package edu.lab.core.commands;

/**
 * 命令注册表。
 * <p>
 * 接收用户输入的原始命令行，负责解析并分发到对应的命令处理器。
 */
public interface CommandRegistry {
    /**
     * 执行一条命令行。
     *
     * @param rawLine 用户输入的原始文本
     * @return 执行结果（输出文本 + 是否需要退出）
     */
    ExecutionResult execute(String rawLine);
}
