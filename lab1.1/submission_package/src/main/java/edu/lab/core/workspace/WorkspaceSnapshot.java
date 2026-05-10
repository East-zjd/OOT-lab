package edu.lab.core.workspace;

import java.nio.file.Path;
import java.util.List;

/**
 * 工作区状态快照（备忘录模式）。
 * <p>
 * 用于持久化：当前打开的编辑器列表、活动文件、全局日志开关等。
 */
public record WorkspaceSnapshot(
        List<EditorSnapshot> openEditors,
        Path activeFile,
        boolean globalLogEnabled
) {
    /**
     * 单个编辑器的持久化信息。
     *
     * @param path       文件路径
     * @param modified   是否为“已修改未保存”状态
     * @param logEnabled 是否开启日志
     */
    public record EditorSnapshot(Path path, boolean modified, boolean logEnabled) {
    }

    /**
     * @return 一个空的快照（用于首次启动或读取失败时兜底）
     */
    public static WorkspaceSnapshot empty() {
        return new WorkspaceSnapshot(List.of(), null, true);
    }
}
