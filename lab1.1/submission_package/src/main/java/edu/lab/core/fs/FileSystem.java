package edu.lab.core.fs;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

/**
 * 文件系统抽象。
 * <p>
 * 将文件 I/O 操作封装在接口内，便于测试替换或扩展不同的文件系统实现。
 */
public interface FileSystem {
    /**
     * @return path 是否存在
     */
    boolean exists(Path path);

    /**
     * @return path 是否为目录
     */
    boolean isDirectory(Path path);

    /**
     * 列出目录下的直接子项。
     */
    List<Path> list(Path dir) throws IOException;

    List<String> readAllLines(Path file, Charset charset) throws IOException;

    void writeString(Path file, String content, Charset charset) throws IOException;

    String readString(Path file, Charset charset) throws IOException;

    void createDirectories(Path dir) throws IOException;

    Path normalize(Path path);
}
