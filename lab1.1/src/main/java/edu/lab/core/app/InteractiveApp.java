package edu.lab.core.app;

import edu.lab.core.commands.CommandRegistry;
import edu.lab.core.commands.ExecutionResult;
import edu.lab.core.console.Console;
import edu.lab.core.workspace.Workspace;

/**
 * 交互式命令行应用（REPL）。
 * <p>
 * 启动后会：恢复工作区状态 -> 提示用户输入命令 -> 执行命令 -> 输出结果 -> 循环。
 */
public final class InteractiveApp implements App {
    private final Console console;
    private final Workspace workspace;
    private final CommandRegistry registry;

    /**
     * 构造一个交互式应用。
     *
     * @param console  控制台输入输出
     * @param workspace 工作区（编辑器集合/文件操作/撤销重做等）
     * @param registry 命令注册表（解析并执行命令）
     */
    public InteractiveApp(Console console, Workspace workspace, CommandRegistry registry) {
        this.console = console;
        this.workspace = workspace;
        this.registry = registry;
    }

    @Override
    public ExitCode runInteractive() {
        // 启动时尝试恢复上一次的工作区状态（已打开文件、活动文件、日志开关等）
        workspace.restore();
        console.println("Lab1 CLI Editor (type 'exit' to quit)");

        while (true) {
            // 提示符
            console.print("> ");
            String line = console.readLine();
            if (line == null) {
                // 输入流关闭：按“正常退出”处理
                workspace.exit();
                return ExitCode.ok();
            }
            line = line.trim();
            if (line.isEmpty()) {
                // 空行直接忽略
                continue;
            }

            // 执行命令，并把输出打印到控制台
            ExecutionResult result = registry.execute(line);
            if (!result.output().isEmpty()) {
                console.println(result.output());
            }
            if (result.shouldExit()) {
                // 某些命令（如 exit）会要求退出 REPL
                return ExitCode.ok();
            }
        }
    }
}
