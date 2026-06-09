package edu.lab.core.workspace;

import edu.lab.core.editor.Editor;

import java.nio.file.Path;

/**
 * editor-list 基础标签格式（不含编辑时长）。
 */
public final class DefaultEditorLabelFormatter implements EditorLabelFormatter {
    @Override
    public String format(Path file, Editor editor, boolean active) {
        StringBuilder sb = new StringBuilder();
        sb.append(active ? "* " : "  ");
        sb.append(file.getFileName());
        if (editor.isModified()) {
            sb.append(" [modified]");
        }
        return sb.toString();
    }
}
