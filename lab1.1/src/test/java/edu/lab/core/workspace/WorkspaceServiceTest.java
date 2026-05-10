package edu.lab.core.workspace;

import edu.lab.core.commands.CommandRegistry;
import edu.lab.core.commands.DefaultCommandRegistry;
import edu.lab.core.commands.ExecutionResult;
import edu.lab.core.events.EventBus;
import edu.lab.core.events.SimpleEventBus;
import edu.lab.core.fs.LocalFileSystem;
import edu.lab.core.logging.LogService;
import edu.lab.core.logging.WorkspaceLogService;
import edu.lab.core.persistence.PropertiesWorkspacePersistence;
import edu.lab.core.persistence.WorkspacePersistence;
import edu.lab.testkit.FakeClock;
import edu.lab.testkit.FakeConsole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link WorkspaceService} 的集成式测试。
 * <p>
 * 通过真实文件系统 + 临时目录，验证 load/append/show/save/exit 持久化与 restore 恢复。
 */
class WorkspaceServiceTest {
    @TempDir
    Path temp;

    @Test
    void load_append_show_save_and_persist_restore() throws Exception {
        // 组装真实运行环境（与 AppFactory 类似，但将状态文件放在临时目录）
        var fs = new LocalFileSystem();
        FakeConsole console = new FakeConsole().addInputs("n", "n", "n");
        FakeClock clock = new FakeClock(LocalDateTime.of(2025, 10, 24, 9, 41, 33));
        EventBus bus = new SimpleEventBus();
        LogService log = new WorkspaceLogService(fs, clock, console);
        WorkspacePersistence persistence = new PropertiesWorkspacePersistence(fs, temp.resolve(".state.properties"));

        Workspace ws1 = new WorkspaceService(fs, persistence, bus, log, console);
        CommandRegistry reg1 = new DefaultCommandRegistry(ws1, bus);

        Path file = temp.resolve("lab.txt");
        Files.writeString(file, "# log\nHello", StandardCharsets.UTF_8);

        // load 后 append 一行
        assertEquals("ok", reg1.execute("load \"" + file + "\"").output());
        assertEquals("ok", reg1.execute("append \"New line\"").output());

        // show 输出应包含行号与内容
        String show = reg1.execute("show").output();
        assertTrue(show.contains("1: # log"));
        assertTrue(show.contains("2: Hello"));
        assertTrue(show.contains("3: New line"));

        // 保存到磁盘
        assertEquals("ok", reg1.execute("save").output());

        // exit 会持久化工作区快照
        ExecutionResult exit = reg1.execute("exit");
        assertTrue(exit.shouldExit());

        // Restore into a new workspace instance
        FakeConsole console2 = new FakeConsole().addInputs("n", "n", "n");
        LogService log2 = new WorkspaceLogService(fs, clock, console2);
        Workspace ws2 = new WorkspaceService(fs, persistence, bus, log2, console2);
        ws2.restore();

        // restore 后应自动打开并选中活动文件
        assertNotNull(ws2.activeFileOrNull());
        assertTrue(ws2.isOpen(file));

        // 日志文件应存在并包含命令行（由事件总线触发写入）
        // Log file should exist and include command lines
        Path logFile = temp.resolve(".lab.txt.log");
        assertTrue(Files.exists(logFile));
        String logContent = Files.readString(logFile, StandardCharsets.UTF_8);
        assertTrue(logContent.contains("session start at"));
        assertTrue(logContent.contains("append \"New line\""));
    }

    @Test
    void close_modified_prompts_and_can_skip_save() throws Exception {
        var fs = new LocalFileSystem();
        // 输入 n：表示关闭时遇到修改不保存
        FakeConsole console = new FakeConsole().addInputs("n");
        FakeClock clock = new FakeClock(LocalDateTime.of(2025, 10, 24, 9, 41, 33));
        EventBus bus = new SimpleEventBus();
        LogService log = new WorkspaceLogService(fs, clock, console);
        WorkspacePersistence persistence = new PropertiesWorkspacePersistence(fs, temp.resolve(".state.properties"));

        Workspace ws = new WorkspaceService(fs, persistence, bus, log, console);
        CommandRegistry reg = new DefaultCommandRegistry(ws, bus);

        Path file = temp.resolve("a.txt");
        Files.writeString(file, "Hello", StandardCharsets.UTF_8);

        reg.execute("load \"" + file + "\"");
        reg.execute("append \"X\"");

        // close 关闭后文件不应仍处于打开状态
        assertEquals("ok", reg.execute("close").output());
        assertFalse(ws.isOpen(file));
    }

    @Test
    void close_is_logged_for_closed_file_when_logging_enabled() throws Exception {
        var fs = new LocalFileSystem();
        FakeConsole console = new FakeConsole();
        FakeClock clock = new FakeClock(LocalDateTime.of(2025, 10, 24, 9, 41, 33));
        EventBus bus = new SimpleEventBus();
        LogService log = new WorkspaceLogService(fs, clock, console);
        WorkspacePersistence persistence = new PropertiesWorkspacePersistence(fs, temp.resolve(".state.properties"));

        Workspace ws = new WorkspaceService(fs, persistence, bus, log, console);
        CommandRegistry reg = new DefaultCommandRegistry(ws, bus);

        Path file = temp.resolve("lab.txt");
        Files.writeString(file, "# log\nHello", StandardCharsets.UTF_8);

        // load 会自动启用日志；close 应记录到同一个文件的日志中
        assertEquals("ok", reg.execute("load \"" + file + "\"").output());
        assertEquals("ok", reg.execute("close").output());

        Path logFile = temp.resolve(".lab.txt.log");
        assertTrue(Files.exists(logFile));
        String content = Files.readString(logFile, StandardCharsets.UTF_8);
        assertTrue(content.contains(" load \"" + file + "\""), "日志应包含 load 命令");
        assertTrue(content.contains(" close"), "日志应包含 close 命令");
    }

    @Test
    void editor_list_keeps_two_space_prefix_for_non_active_first_editor() throws Exception {
        var fs = new LocalFileSystem();
        FakeConsole console = new FakeConsole().addInputs("n", "n");
        FakeClock clock = new FakeClock(LocalDateTime.of(2025, 10, 24, 9, 41, 33));
        EventBus bus = new SimpleEventBus();
        LogService log = new WorkspaceLogService(fs, clock, console);
        WorkspacePersistence persistence = new PropertiesWorkspacePersistence(fs, temp.resolve(".state.properties"));

        Workspace ws = new WorkspaceService(fs, persistence, bus, log, console);
        CommandRegistry reg = new DefaultCommandRegistry(ws, bus);

        Path file1 = temp.resolve("lab.txt");
        Path file2 = temp.resolve("lab1.txt");
        Files.writeString(file1, "A", StandardCharsets.UTF_8);
        Files.writeString(file2, "B", StandardCharsets.UTF_8);

        // 依次打开两个文件；最后打开的会成为活动编辑器
        assertEquals("ok", reg.execute("load \"" + file1 + "\"").output());
        assertEquals("ok", reg.execute("load \"" + file2 + "\"").output());

        // 活动编辑器为第二个文件时，第一个文件前仍应保留两个空格前缀
        String out = reg.execute("editor-list").output();
        assertEquals("  lab.txt\n* lab1.txt", out);
    }

    @Test
    void restore_keeps_modified_flag_from_snapshot() throws Exception {
        var fs = new LocalFileSystem();
        FakeConsole console = new FakeConsole().addInputs("n", "n", "n");
        FakeClock clock = new FakeClock(LocalDateTime.of(2025, 10, 24, 9, 41, 33));
        EventBus bus = new SimpleEventBus();
        LogService log = new WorkspaceLogService(fs, clock, console);
        WorkspacePersistence persistence = new PropertiesWorkspacePersistence(fs, temp.resolve(".state.properties"));

        Workspace ws1 = new WorkspaceService(fs, persistence, bus, log, console);
        CommandRegistry reg1 = new DefaultCommandRegistry(ws1, bus);

        Path file = temp.resolve("dirty.txt");
        Files.writeString(file, "Hello", StandardCharsets.UTF_8);

        assertEquals("ok", reg1.execute("load \"" + file + "\"").output());
        assertEquals("ok", reg1.execute("append \"X\"").output());

        // 不保存直接退出：快照应记录 modified=true
        ExecutionResult exit = reg1.execute("exit");
        assertTrue(exit.shouldExit());

        // 新实例恢复后，文件仍应保持 modified 状态
        FakeConsole console2 = new FakeConsole().addInputs("n", "n", "n");
        LogService log2 = new WorkspaceLogService(fs, clock, console2);
        Workspace ws2 = new WorkspaceService(fs, persistence, bus, log2, console2);
        ws2.restore();

        assertTrue(ws2.isOpen(file));
        assertTrue(ws2.isModified(file), "restore 后应保持 modified=true");
    }

    @Test
    void restore_keeps_modified_flag_for_empty_content_file() throws Exception {
        var fs = new LocalFileSystem();
        // 退出时会询问是否保存 modified 文件，这里输入 n
        FakeConsole console = new FakeConsole().addInputs("n");
        FakeClock clock = new FakeClock(LocalDateTime.of(2025, 10, 24, 9, 41, 33));
        EventBus bus = new SimpleEventBus();
        LogService log = new WorkspaceLogService(fs, clock, console);
        WorkspacePersistence persistence = new PropertiesWorkspacePersistence(fs, temp.resolve(".state.properties"));

        Workspace ws1 = new WorkspaceService(fs, persistence, bus, log, console);
        CommandRegistry reg1 = new DefaultCommandRegistry(ws1, bus);

        // 加载一个不存在的文件：根据 lab1.md，应创建空文件并标记为 modified
        Path file = temp.resolve("empty.txt");
        assertEquals("ok", reg1.execute("load \"" + file + "\"").output());

        ExecutionResult exit = reg1.execute("exit");
        assertTrue(exit.shouldExit());

        FakeConsole console2 = new FakeConsole().addInputs("n");
        LogService log2 = new WorkspaceLogService(fs, clock, console2);
        Workspace ws2 = new WorkspaceService(fs, persistence, bus, log2, console2);
        ws2.restore();

        assertTrue(ws2.isOpen(file));
        assertTrue(ws2.isModified(file), "空内容文件也应能恢复 modified=true");
    }
}
