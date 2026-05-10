package edu.lab.core.persistence;

import edu.lab.core.workspace.WorkspaceSnapshot;

/**
 * 工作区状态持久化接口。
 * <p>
 * 负责保存/加载 {@link WorkspaceSnapshot}，用于下次启动恢复工作区。
 */
public interface WorkspacePersistence {
    /**
     * 保存工作区快照。
     */
    void save(WorkspaceSnapshot snapshot);

    /**
     * 加载工作区快照；若不存在或失败则返回空快照。
     */
    WorkspaceSnapshot loadOrEmpty();
}
