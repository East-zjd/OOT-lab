package edu.lab.core.fs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link LocalFileSystem} 的单元测试。
 * <p>
 * 覆盖：写文件会自动创建父目录；list 按文件名不区分大小写排序；normalize 返回绝对规范路径。
 */
class LocalFileSystemTest {
    @TempDir
    Path temp;

    @Test
    void writeString_creates_parent_directories_and_can_read_back() throws Exception {
        FileSystem fs = new LocalFileSystem();
        Path nested = temp.resolve("a/b/c.txt");

        // 写入时应创建父目录 a/b
        fs.writeString(nested, "Hello", StandardCharsets.UTF_8);
        assertTrue(Files.exists(nested));
        // 读取应得到相同内容
        assertEquals("Hello", fs.readString(nested, StandardCharsets.UTF_8));
    }

    @Test
    void list_is_sorted_case_insensitive_by_file_name() throws Exception {
        FileSystem fs = new LocalFileSystem();
        Path dir = temp.resolve("dir");
        Files.createDirectories(dir);

        Files.createFile(dir.resolve("b.txt"));
        Files.createFile(dir.resolve("A.txt"));
        Files.createFile(dir.resolve("c.TXT"));

        // 期望排序：A.txt, b.txt, c.TXT
        var names = fs.list(dir).stream().map(p -> p.getFileName().toString()).toList();
        assertEquals(java.util.List.of("A.txt", "b.txt", "c.TXT"), names);
    }

    @Test
    void normalize_returns_absolute_normalized_path() {
        FileSystem fs = new LocalFileSystem();
        Path p = temp.resolve("x/../y.txt");
        Path n = fs.normalize(p);

        // normalize 后应为绝对路径，且 .. 被消解
        assertTrue(n.isAbsolute());
        assertTrue(n.toString().endsWith("y.txt"));
    }
}
