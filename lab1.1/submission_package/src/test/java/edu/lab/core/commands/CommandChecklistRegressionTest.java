package edu.lab.core.commands;

import edu.lab.core.events.EventBus;
import edu.lab.core.events.SimpleEventBus;
import edu.lab.core.fs.LocalFileSystem;
import edu.lab.core.logging.LogService;
import edu.lab.core.logging.WorkspaceLogService;
import edu.lab.core.persistence.PropertiesWorkspacePersistence;
import edu.lab.core.persistence.WorkspacePersistence;
import edu.lab.core.workspace.Workspace;
import edu.lab.core.workspace.WorkspaceService;
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
 * 命令清单回归测试。
 * <p>
 * 目的：保证文档中列出的命令都已注册，并且在真实工作区中可以串起来跑通一遍。
 */
class CommandChecklistRegressionTest {
    @TempDir
    Path temp;

    @Test
    void all_documented_commands_are_registered_and_runnable() throws Exception {
        // 组装一套“接近真实”的运行环境（本地文件系统 + 事件总线 + 日志 + 持久化 + 工作区 + 命令注册表）
        var fs = new LocalFileSystem();
        FakeConsole console = new FakeConsole().addInputs("n", "n", "n", "n", "n");
        FakeClock clock = new FakeClock(LocalDateTime.of(2025, 10, 24, 9, 41, 33));
        EventBus bus = new SimpleEventBus();
        LogService log = new WorkspaceLogService(fs, clock, console);
        WorkspacePersistence persistence = new PropertiesWorkspacePersistence(fs, temp.resolve(".state.properties"));

        Workspace ws = new WorkspaceService(fs, persistence, bus, log, console);
        CommandRegistry reg = new DefaultCommandRegistry(ws, bus);

        Path file1 = temp.resolve("a.txt");
        Path file2 = temp.resolve("b.txt");
        Files.writeString(file2, "Hello", StandardCharsets.UTF_8);

        // init + with-log：新建文件并开启日志
        assertOk(reg.execute("init \"" + file1 + "\" with-log"));

        // 基本编辑命令
        assertOk(reg.execute("append \"Hello\""));
        assertOk(reg.execute("insert 1:1 \"X\""));
        assertOk(reg.execute("delete 2:1 1"));
        assertOk(reg.execute("replace 2:1 1 \"H\""));

        // 撤销/重做
        assertOk(reg.execute("undo"));
        assertOk(reg.execute("redo"));

        ExecutionResult show = reg.execute("show");
        assertFalse(show.output().isEmpty());

        ExecutionResult spell = reg.execute("spell-check");
        assertFalse(spell.output().isEmpty());

        // 保存命令：保存当前、保存全部、保存指定
        assertOk(reg.execute("save"));
        assertOk(reg.execute("save all"));
        assertOk(reg.execute("save \"" + file1 + "\""));

        // 文件切换
        assertOk(reg.execute("load \"" + file2 + "\""));
        assertOk(reg.execute("edit \"" + file1 + "\""));

        ExecutionResult list = reg.execute("editor-list");
        assertFalse(list.output().isEmpty());

        // 目录树
        ExecutionResult tree = reg.execute("dir-tree \"" + temp + "\"");
        assertFalse(tree.output().isEmpty());
        assertFalse(tree.output().startsWith("(error)"));

        // 日志开关与查看
        assertOk(reg.execute("log-off"));
        assertOk(reg.execute("log-on"));

        ExecutionResult logShow = reg.execute("log-show");
        assertFalse(logShow.output().isEmpty());

        // close 与 exit
        assertOk(reg.execute("close"));

        ExecutionResult exit = reg.execute("exit");
        assertTrue(exit.shouldExit());
        assertFalse(exit.output().isEmpty());
    }

    private static void assertOk(ExecutionResult r) {
        // 统一的“成功断言”：结果非空、不要求退出、输出不以 (error) 开头
        assertNotNull(r);
        assertFalse(r.shouldExit());
        assertFalse(r.output().startsWith("(error)"), r.output());
    }
}
