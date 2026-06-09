package edu.lab.core.editor;

import edu.lab.core.workspace.LineCol;

import java.util.List;

/**
 * 自动维护修改状态的装饰器。
 */
public final class ModifiedEditorDecorator extends EditorDecorator {
    private boolean modified;

    public ModifiedEditorDecorator(Editor delegate, boolean modified) {
        super(delegate);
        this.modified = modified;
    }

    @Override
    public boolean isModified() {
        return modified;
    }

    @Override
    public void setLines(List<String> lines, boolean markSaved) {
        delegate.setLines(lines, markSaved);
        modified = !markSaved;
    }

    @Override
    public void markSaved() {
        delegate.markSaved();
        modified = false;
    }

    @Override
    public String append(String text) {
        return markModifiedIfOk(delegate.append(text));
    }

    @Override
    public String insert(LineCol pos, String text) {
        return markModifiedIfOk(delegate.insert(pos, text));
    }

    @Override
    public String delete(LineCol pos, int len) {
        return markModifiedIfOk(delegate.delete(pos, len));
    }

    @Override
    public String replace(LineCol pos, int len, String text) {
        return markModifiedIfOk(delegate.replace(pos, len, text));
    }

    @Override
    public String insertBefore(String tagName, String newId, String targetId, String textOrNull) {
        return markModifiedIfOk(delegate.insertBefore(tagName, newId, targetId, textOrNull));
    }

    @Override
    public String appendChild(String tagName, String newId, String parentId, String textOrNull) {
        return markModifiedIfOk(delegate.appendChild(tagName, newId, parentId, textOrNull));
    }

    @Override
    public String editId(String oldId, String newId) {
        return markModifiedIfOk(delegate.editId(oldId, newId));
    }

    @Override
    public String editText(String elementId, String textOrNull) {
        return markModifiedIfOk(delegate.editText(elementId, textOrNull));
    }

    @Override
    public String deleteElement(String elementId) {
        return markModifiedIfOk(delegate.deleteElement(elementId));
    }

    private String markModifiedIfOk(String out) {
        if ("ok".equals(out)) {
            modified = true;
        }
        return out;
    }
}
