package edu.lab.core.fs;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 本地文件系统实现。
 * <p>
 * 基于 {@link java.nio.file.Files} 提供真实磁盘读写能力。
 */
public final class LocalFileSystem implements FileSystem {
    @Override
    public boolean exists(Path path) {
        return Files.exists(path);
    }

    @Override
    public boolean isDirectory(Path path) {
        return Files.isDirectory(path);
    }

    @Override
    public List<Path> list(Path dir) throws IOException {
        try (var stream = Files.list(dir)) {
            // 统一按文件名（忽略大小写）排序，保证输出稳定
            return stream.sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<String> readAllLines(Path file, Charset charset) throws IOException {
        return Files.readAllLines(file, charset);
    }

    @Override
    public void writeString(Path file, String content, Charset charset) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            // 写文件前确保父目录存在
            Files.createDirectories(parent);
        }
        Files.writeString(file, content, charset);
    }

    @Override
    public String readString(Path file, Charset charset) throws IOException {
        return Files.readString(file, charset);
    }

    @Override
    public void createDirectories(Path dir) throws IOException {
        Files.createDirectories(dir);
    }

    @Override
    public Path normalize(Path path) {
        // 规范化并转换成绝对路径，便于 map/set 中一致比较
        return path.normalize().toAbsolutePath();
    }
}
