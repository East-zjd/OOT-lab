package edu.lab.core.app;

/**
 * 应用抽象。
 * <p>
 * 本实验中应用以“交互式命令行”形式运行，并通过返回退出码表示运行结果。
 */
public interface App {
    /**
     * 启动交互式运行（通常会阻塞直到用户退出）。
     *
     * @return 退出码
     */
    ExitCode runInteractive();

    /**
     * 进程退出码。
     * <p>
     * 约定：0 表示正常退出，其它值可表示异常/失败。
     */
    record ExitCode(int code) {
        /**
         * 正常退出（exit code = 0）。
         */
        public static ExitCode ok() {
            return new ExitCode(0);
        }
    }
}
