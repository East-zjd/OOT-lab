package edu.lab.core.commands;

import edu.lab.core.events.EventBus;
import edu.lab.core.events.SimpleEventBus;
import edu.lab.core.workspace.LineCol;
import edu.lab.core.workspace.Workspace;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link DefaultCommandRegistry} 的单元测试。
 * <p>
 * 覆盖：未知命令的错误提示；参数使用错误（usage）与解析错误（如未闭合引号）的错误输出格式。
 */
class DefaultCommandRegistryTest {
    @Test
    void execute_unknown_command_returns_error_message() {
        Workspace ws = new NoopWorkspace();
        EventBus bus = new SimpleEventBus();
        CommandRegistry reg = new DefaultCommandRegistry(ws, bus);

        // 未注册命令应返回 unknown command
        ExecutionResult r = reg.execute("does-not-exist");
        assertFalse(r.shouldExit());
        assertTrue(r.output().contains("unknown command"));
    }

    @Test
    void execute_usage_errors_are_reported_as_error_output() {
        Workspace ws = new NoopWorkspace();
        EventBus bus = new SimpleEventBus();
        CommandRegistry reg = new DefaultCommandRegistry(ws, bus);

        // 参数数量不对、usage 不匹配时，错误消息应以 (error) 开头
        assertEquals("(error) usage: load", reg.execute("load").output());
        assertEquals("(error) usage: save [file|all]", reg.execute("save a b").output());
        // 解析失败（未闭合引号）同样应转换为 (error) 输出
        assertEquals("(error) unterminated quote", reg.execute("load \"abc").output());
    }

    /**
     * 空实现 Workspace：本测试只关心命令解析/错误处理，不需要真正的工作区行为。
     */
    private static final class NoopWorkspace implements Workspace {
        @Override public void restore() { throw new UnsupportedOperationException(); }
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
        @Override public String exit() { throw new UnsupportedOperationException(); }
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
