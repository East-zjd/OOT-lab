package edu.lab.core.app;

import edu.lab.core.commands.CommandRegistry;
import edu.lab.core.commands.ExecutionResult;
import edu.lab.core.console.Console;
import edu.lab.core.workspace.LineCol;
import edu.lab.core.workspace.Workspace;
import edu.lab.testkit.FakeConsole;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link InteractiveApp} 的行为测试。
 * <p>
 * 通过注入 FakeConsole/FakeWorkspace/自定义 CommandRegistry，验证 REPL 循环的退出条件与输出。
 */
class InteractiveAppTest {
    @Test
    void runInteractive_exits_when_registry_requests_exit() {
        // 准备：控制台先输入 help 再输入 exit
        FakeConsole console = new FakeConsole().addInputs("help", "exit");
        FakeWorkspace ws = new FakeWorkspace();

        // 命令注册表：遇到 exit 则请求退出，否则回显 ok:<命令>
        CommandRegistry reg = rawLine -> {
            if ("exit".equals(rawLine.trim())) {
                return ExecutionResult.exit("bye");
            }
            return ExecutionResult.ok("ok:" + rawLine.trim());
        };

        App app = new InteractiveApp(console, ws, reg);
        App.ExitCode code = app.runInteractive();

        // 启动时必须先 restore；退出码应为 0
        assertTrue(ws.restoreCalled);
        assertEquals(0, code.code());

        // 输出应包含欢迎语、help 的输出、exit 的输出
        String out = console.joinedOutput();
        assertTrue(out.contains("Lab1 CLI Editor"));
        assertTrue(out.contains("ok:help"));
        assertTrue(out.contains("bye"));
    }

    @Test
    void runInteractive_on_eof_calls_workspace_exit() {
        // EOF 控制台：readLine 直接返回 null
        EofConsole console = new EofConsole();
        FakeWorkspace ws = new FakeWorkspace();

        CommandRegistry reg = ignored -> ExecutionResult.ok("should-not-run");
        App app = new InteractiveApp(console, ws, reg);
        App.ExitCode code = app.runInteractive();

        // EOF 情况下：仍会 restore，然后调用 workspace.exit 并正常退出
        assertTrue(ws.restoreCalled);
        assertTrue(ws.exitCalled);
        assertEquals(0, code.code());
    }

    /**
     * 模拟 EOF 的控制台实现：readLine 返回 null。
     */
    private static final class EofConsole implements Console {
        @Override public void print(String s) { }
        @Override public void println(String s) { }
        @Override public String readLine() { return null; }
    }

    /**
     * 只实现 restore/exit 的最小 Workspace 假对象。
     * 其余方法不应在本测试中被调用。
     */
    private static final class FakeWorkspace implements Workspace {
        boolean restoreCalled;
        boolean exitCalled;

        @Override public void restore() { restoreCalled = true; }
        @Override public String exit() { exitCalled = true; return "bye"; }

        @Override public String load(Path file) { throw new UnsupportedOperationException(); }
        @Override public String init(Path file, boolean withLog) { throw new UnsupportedOperationException(); }
        @Override public String saveActive() { throw new UnsupportedOperationException(); }
        @Override public String save(Path file) { throw new UnsupportedOperationException(); }
        @Override public String saveAll() { throw new UnsupportedOperationException(); }
        @Override public String closeActive() { throw new UnsupportedOperationException(); }
        @Override public String close(Path file) { throw new UnsupportedOperationException(); }
        @Override public String edit(Path file) { throw new UnsupportedOperationException(); }
        @Override public String listEditors() { throw new UnsupportedOperationException(); }
        @Override public String dirTree(Path pathOrNull) { throw new UnsupportedOperationException(); }
        @Override public String undo() { throw new UnsupportedOperationException(); }
        @Override public String redo() { throw new UnsupportedOperationException(); }
        @Override public Path activeFileOrNull() { return null; }
        @Override public boolean isOpen(Path file) { return false; }
        @Override public boolean isModified(Path file) { return false; }
        @Override public boolean isLogEnabled(Path file) { return false; }
        @Override public String logOn(Path fileOrNull) { throw new UnsupportedOperationException(); }
        @Override public String logOff(Path fileOrNull) { throw new UnsupportedOperationException(); }
        @Override public String logShow(Path fileOrNull) { throw new UnsupportedOperationException(); }
        @Override public String append(String text) { throw new UnsupportedOperationException(); }
        @Override public String insert(LineCol pos, String text) { throw new UnsupportedOperationException(); }
        @Override public String delete(LineCol pos, int len) { throw new UnsupportedOperationException(); }
        @Override public String replace(LineCol pos, int len, String text) { throw new UnsupportedOperationException(); }
        @Override public String show(Integer startLineOrNull, Integer endLineOrNull) { throw new UnsupportedOperationException(); }
        @Override public String spellCheck() { throw new UnsupportedOperationException(); }
    }
}
