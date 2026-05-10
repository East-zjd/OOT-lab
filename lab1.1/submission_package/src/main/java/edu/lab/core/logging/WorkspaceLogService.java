package edu.lab.core.logging;

import edu.lab.core.console.Console;
import edu.lab.core.fs.FileSystem;
import edu.lab.core.time.Clock;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/**
 * 工作区日志服务实现。
 * <p>
 * 约定：日志文件存放在同目录下，文件名为 ".<原文件名>.log"（以点号开头的隐藏文件风格）。
 */
public final class WorkspaceLogService implements LogService {
    // 时间戳格式：yyyyMMdd HH:mm:ss
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");

    private final FileSystem fileSystem;
    private final Clock clock;
    private final Console console;

    private final Set<Path> enabled = new HashSet<>();
    private final Set<Path> sessionStartedFor = new HashSet<>();

    public WorkspaceLogService(FileSystem fileSystem, Clock clock, Console console) {
        this.fileSystem = fileSystem;
        this.clock = clock;
        this.console = console;
    }

    @Override
    public void enable(Path file) {
        // 统一转成绝对规范路径，避免同一文件的不同表示导致重复
        enabled.add(file.toAbsolutePath().normalize());
    }

    @Override
    public void disable(Path file) {
        enabled.remove(file.toAbsolutePath().normalize());
    }

    @Override
    public boolean isEnabled(Path file) {
        return enabled.contains(file.toAbsolutePath().normalize());
    }

    @Override
    public String show(Path file) {
        try {
            Path logFile = logFilePath(file);
            if (!fileSystem.exists(logFile)) {
                return "(no log)";
            }
            return fileSystem.readString(logFile, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 读取日志失败不应影响主流程
            return "(warning) cannot read log: " + e.getMessage();
        }
    }

    @Override
    public void logCommand(Path file, String rawCommandLine) {
        try {
            Path normalized = file.toAbsolutePath().normalize();
            if (!enabled.contains(normalized)) {
                // 未开启日志则忽略
                return;
            }
            Path logFile = logFilePath(normalized);
            StringBuilder sb = new StringBuilder();
            if (!sessionStartedFor.contains(normalized)) {
                // 每个会话只写一次 session start
                sb.append("session start at ").append(clock.now().format(TS)).append('\n');
                sessionStartedFor.add(normalized);
            }
            sb.append(clock.now().format(TS)).append(' ').append(rawCommandLine).append('\n');

            String existing = fileSystem.exists(logFile) ? fileSystem.readString(logFile, StandardCharsets.UTF_8) : "";
            fileSystem.writeString(logFile, existing + sb, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 写日志失败也只给出 warning
            console.println("(warning) log failed: " + e.getMessage());
        }
    }

    private static Path logFilePath(Path file) {
        // 日志文件命名：在原文件名前加点，并加 .log 后缀
        String name = file.getFileName().toString();
        return file.getParent() == null
                ? Path.of("." + name + ".log")
                : file.getParent().resolve("." + name + ".log");
    }
}
