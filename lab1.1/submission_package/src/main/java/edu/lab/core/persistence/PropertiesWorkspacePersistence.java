package edu.lab.core.persistence;

import edu.lab.core.fs.FileSystem;
import edu.lab.core.workspace.WorkspaceSnapshot;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 基于 {@link Properties} 的工作区状态持久化实现。
 * <p>
 * 将打开的文件列表、活动文件、日志开关等序列化到一个 .properties 文件中。
 */
public final class PropertiesWorkspacePersistence implements WorkspacePersistence {
    private final FileSystem fileSystem;
    private final Path stateFile;

    public PropertiesWorkspacePersistence(FileSystem fileSystem, Path stateFile) {
        this.fileSystem = fileSystem;
        this.stateFile = stateFile;
    }

    @Override
    public void save(WorkspaceSnapshot snapshot) {
        try {
            Properties p = new Properties();
            if (snapshot.activeFile() != null) {
                p.setProperty("active", snapshot.activeFile().toString());
            }
            p.setProperty("logGlobal", Boolean.toString(snapshot.globalLogEnabled()));
            p.setProperty("open.count", Integer.toString(snapshot.openEditors().size()));

            for (int i = 0; i < snapshot.openEditors().size(); i++) {
                var e = snapshot.openEditors().get(i);
                // 使用 open.i.* 的方式保存第 i 个编辑器的状态
                p.setProperty("open." + i + ".path", e.path().toString());
                p.setProperty("open." + i + ".modified", Boolean.toString(e.modified()));
                p.setProperty("open." + i + ".log", Boolean.toString(e.logEnabled()));
            }

            StringWriter sw = new StringWriter();
            p.store(sw, "workspace state");
            fileSystem.writeString(stateFile, sw.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // 持久化失败不应影响主流程
        }
    }

    @Override
    public WorkspaceSnapshot loadOrEmpty() {
        try {
            if (!fileSystem.exists(stateFile)) {
                return WorkspaceSnapshot.empty();
            }
            String content = fileSystem.readString(stateFile, StandardCharsets.UTF_8);
            Properties p = new Properties();
            p.load(new StringReader(content));

            String active = p.getProperty("active", null);
            boolean globalLog = Boolean.parseBoolean(p.getProperty("logGlobal", "false"));
            int count = Integer.parseInt(p.getProperty("open.count", "0"));

            List<WorkspaceSnapshot.EditorSnapshot> editors = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String path = p.getProperty("open." + i + ".path", null);
                if (path == null) {
                    continue;
                }
                boolean modified = Boolean.parseBoolean(p.getProperty("open." + i + ".modified", "false"));
                boolean log = Boolean.parseBoolean(p.getProperty("open." + i + ".log", "false"));
                editors.add(new WorkspaceSnapshot.EditorSnapshot(Path.of(path), modified, log));
            }

            return new WorkspaceSnapshot(editors, active == null ? null : Path.of(active), globalLog);
        } catch (Exception e) {
            // 读取/解析失败统一回退到空快照
            return WorkspaceSnapshot.empty();
        }
    }
}
