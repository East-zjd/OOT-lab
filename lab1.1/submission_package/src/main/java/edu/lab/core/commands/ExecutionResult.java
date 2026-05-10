package edu.lab.core.commands;

/**
 * 命令执行结果。
 *
 * @param output     要输出到控制台的文本（永不为 null）
 * @param shouldExit 是否要求应用退出（例如执行了 exit 命令）
 */
public record ExecutionResult(String output, boolean shouldExit) {
    /**
     * 正常执行完成，不退出应用。
     */
    public static ExecutionResult ok(String output) {
        return new ExecutionResult(output == null ? "" : output, false);
    }

    /**
     * 执行完成并请求退出应用。
     */
    public static ExecutionResult exit(String output) {
        return new ExecutionResult(output == null ? "" : output, true);
    }
}
