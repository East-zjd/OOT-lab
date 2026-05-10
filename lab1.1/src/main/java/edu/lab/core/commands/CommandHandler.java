package edu.lab.core.commands;

/**
 * 单个命令处理器。
 * <p>
 * 同一个命令名会绑定一个 handler；handler 收到原始输入与已解析的参数列表。
 */
public interface CommandHandler {
    /**
     * 处理命令。
     *
     * @param rawLine 用户输入的原始命令行（用于日志等场景）
     * @param command 已解析的命令名与参数
     * @return 执行结果
     */
    ExecutionResult handle(String rawLine, ParsedCommand command);
}
