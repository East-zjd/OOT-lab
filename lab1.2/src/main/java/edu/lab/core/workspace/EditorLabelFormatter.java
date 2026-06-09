package edu.lab.core.workspace;

import edu.lab.core.editor.Editor;

import java.nio.file.Path;

/**
 * 编辑器列表（editor-list）单行标签格式化器。
 */
public interface EditorLabelFormatter {
    /**
     * @param file   文件路径
     * @param editor 对应编辑器
     * @param active 是否为当前活动文件
     * @return 用于 editor-list 的单行标签（不包含末尾换行）
     */
    String format(Path file, Editor editor, boolean active);
}
