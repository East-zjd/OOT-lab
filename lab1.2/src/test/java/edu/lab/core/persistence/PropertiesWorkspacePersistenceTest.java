package edu.lab.core.persistence;

import edu.lab.core.fs.FileSystem;
import edu.lab.core.fs.LocalFileSystem;
import edu.lab.core.workspace.WorkspaceSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PropertiesWorkspacePersistence} 的单元测试。
 * <p>
 * 覆盖：snapshot 保存后可完整读取还原；状态文件不存在时 loadOrEmpty 返回空快照。
 */
class PropertiesWorkspacePersistenceTest {
    @TempDir
    Path temp;

    @Test
    void save_and_load_roundtrips_snapshot() {
        FileSystem fs = new LocalFileSystem();
        Path state = temp.resolve(".state.properties");
        WorkspacePersistence p = new PropertiesWorkspacePersistence(fs, state);

        Path a = temp.resolve("a.txt").toAbsolutePath().normalize();
        Path b = temp.resolve("b.txt").toAbsolutePath().normalize();
        // 构造一个包含两个打开文件的快照
        WorkspaceSnapshot snap = new WorkspaceSnapshot(
                List.of(
                        new WorkspaceSnapshot.EditorSnapshot(a, true, true),
                        new WorkspaceSnapshot.EditorSnapshot(b, false, false)
                ),
                b,
                false
        );

        // 保存并读取
        p.save(snap);
        WorkspaceSnapshot loaded = p.loadOrEmpty();

        // 断言关键字段一致
        assertEquals(snap.activeFile(), loaded.activeFile());
        assertEquals(snap.globalLogEnabled(), loaded.globalLogEnabled());
        assertEquals(snap.openEditors().size(), loaded.openEditors().size());
        assertEquals(snap.openEditors().get(0), loaded.openEditors().get(0));
        assertEquals(snap.openEditors().get(1), loaded.openEditors().get(1));
    }

    @Test
    void loadOrEmpty_returns_empty_when_file_missing() {
        FileSystem fs = new LocalFileSystem();
        Path state = temp.resolve("missing.properties");
        WorkspacePersistence p = new PropertiesWorkspacePersistence(fs, state);

        // 文件缺失时不应抛异常，而是返回 empty snapshot
        WorkspaceSnapshot loaded = p.loadOrEmpty();
        assertNotNull(loaded);
        assertTrue(loaded.openEditors().isEmpty());
    }
}
