package edu.lab.core.logging;

import edu.lab.core.fs.FileSystem;
import edu.lab.core.fs.LocalFileSystem;
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
 * {@link WorkspaceLogService} 的单元测试。
 * <p>
 * 覆盖：启用日志的文件会写入 session header（仅一次）并追加命令；未启用则不创建日志文件。
 */
class WorkspaceLogServiceTest {
    @TempDir
    Path temp;

    @Test
    void enabled_file_writes_session_header_once_and_appends_commands() throws Exception {
        FileSystem fs = new LocalFileSystem();
        FakeConsole console = new FakeConsole();
        FakeClock clock = new FakeClock(LocalDateTime.of(2025, 10, 24, 9, 41, 33));
        LogService log = new WorkspaceLogService(fs, clock, console);

        Path file = temp.resolve("lab.txt");
        Files.writeString(file, "# log\nHello", StandardCharsets.UTF_8);

        log.enable(file);
        // 第一次写入：应包含 session start
        log.logCommand(file, "load \"" + file + "\"");

        // 第二次写入：应追加命令且不重复 session start
        clock.setNow(LocalDateTime.of(2025, 10, 24, 9, 41, 40));
        log.logCommand(file, "append \"x\"");

        Path logFile = temp.resolve(".lab.txt.log");
        assertTrue(Files.exists(logFile));

        String content = Files.readString(logFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("session start at 20251024 09:41:33"));
        assertTrue(content.contains("20251024 09:41:33 load"));
        assertTrue(content.contains("20251024 09:41:40 append \"x\""));

        long sessionHeaders = content.lines().filter(l -> l.startsWith("session start at")).count();
        assertEquals(1, sessionHeaders);
    }

    @Test
    void disabled_file_does_not_create_log() throws Exception {
        FileSystem fs = new LocalFileSystem();
        FakeConsole console = new FakeConsole();
        FakeClock clock = new FakeClock(LocalDateTime.of(2025, 10, 24, 9, 41, 33));
        LogService log = new WorkspaceLogService(fs, clock, console);

        Path file = temp.resolve("a.txt");
        Files.writeString(file, "Hello", StandardCharsets.UTF_8);

        // 未 enable 的情况下写入不应创建日志文件
        log.logCommand(file, "append \"x\"");

        Path logFile = temp.resolve(".a.txt.log");
        // show 也应返回 (no log)
        assertFalse(Files.exists(logFile));
        assertEquals("(no log)", log.show(file));
    }
}
