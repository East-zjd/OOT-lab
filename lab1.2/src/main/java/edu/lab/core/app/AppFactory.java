package edu.lab.core.app;

import edu.lab.core.commands.CommandRegistry;
import edu.lab.core.commands.DefaultCommandRegistry;
import edu.lab.core.console.Console;
import edu.lab.core.console.SystemConsole;
import edu.lab.core.events.EventBus;
import edu.lab.core.events.SimpleEventBus;
import edu.lab.core.fs.FileSystem;
import edu.lab.core.fs.LocalFileSystem;
import edu.lab.core.logging.LogService;
import edu.lab.core.logging.WorkspaceLogService;
import edu.lab.core.persistence.WorkspacePersistence;
import edu.lab.core.persistence.PropertiesWorkspacePersistence;
import edu.lab.core.time.Clock;
import edu.lab.core.time.SystemClock;
import edu.lab.core.workspace.Workspace;
import edu.lab.core.workspace.WorkspaceService;

import java.nio.file.Path;

/**
 * 应用工厂。
 * <p>
 * 负责“装配”应用的各个依赖：控制台、文件系统、持久化、日志、工作区、命令注册表等。
 */
public final class AppFactory {
    private AppFactory() {
    }

    /**
     * 创建一个默认配置的应用实例。
     *
     * @return 可直接运行的 {@link App}
     */
    public static App defaultApp() {
        // 控制台：负责读写标准输入输出
        Console console = new SystemConsole();
        // 时钟：用于日志时间戳
        Clock clock = new SystemClock();
        // 事件总线：用于命令执行事件广播（日志等可订阅）
        EventBus eventBus = new SimpleEventBus();
        // 文件系统：封装对本地文件系统的访问
        FileSystem fileSystem = new LocalFileSystem();

        // 工作区状态持久化（打开的文件、活动文件、日志开关等）
        WorkspacePersistence persistence = new PropertiesWorkspacePersistence(fileSystem, Path.of(".workspace.state.properties"));
        // 日志服务（记录命令历史到隐藏的 .xxx.log 文件）
        LogService logService = new WorkspaceLogService(fileSystem, clock, console);

        // 工作区：编辑器集合 + 文件/撤销重做/日志等
        Workspace workspace = new WorkspaceService(fileSystem, persistence, eventBus, logService, console);
        // 命令注册表：将字符串命令分发到具体 handler
        CommandRegistry registry = new DefaultCommandRegistry(workspace, eventBus);

        // 交互式应用：负责 REPL 主循环
        return new InteractiveApp(console, workspace, registry);
    }
}
