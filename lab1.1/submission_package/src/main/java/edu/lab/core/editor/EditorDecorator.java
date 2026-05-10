package edu.lab.core.editor;

import edu.lab.core.workspace.LineCol;

import java.nio.file.Path;
import java.util.List;

/**
 * Editor 装饰器基类。
 * <p>
 * 默认将所有行为委托给被包装的 editor，子类按需覆写扩展点。
 */
public abstract class EditorDecorator implements Editor {
    protected final Editor delegate;

    protected EditorDecorator(Editor delegate) {
        this.delegate = delegate;
    }

    @Override
    public Path file() {
        return delegate.file();
    }

    @Override
    public boolean isModified() {
        return delegate.isModified();
    }

    @Override
    public boolean isLogEnabled() {
        return delegate.isLogEnabled();
    }

    @Override
    public void setLogEnabled(boolean enabled) {
        delegate.setLogEnabled(enabled);
    }

    @Override
    public List<String> lines() {
        return delegate.lines();
    }

    @Override
    public void setLines(List<String> lines, boolean markSaved) {
        delegate.setLines(lines, markSaved);
    }

    @Override
    public void markSaved() {
        delegate.markSaved();
    }

    @Override
    public String append(String text) {
        return delegate.append(text);
    }

    @Override
    public String insert(LineCol pos, String text) {
        return delegate.insert(pos, text);
    }

    @Override
    public String delete(LineCol pos, int len) {
        return delegate.delete(pos, len);
    }

    @Override
    public String replace(LineCol pos, int len, String text) {
        return delegate.replace(pos, len, text);
    }

    @Override
    public String show(Integer startLineOrNull, Integer endLineOrNull) {
        return delegate.show(startLineOrNull, endLineOrNull);
    }

    @Override
    public String spellCheck() {
        return delegate.spellCheck();
    }

    @Override
    public boolean canUndo() {
        return delegate.canUndo();
    }

    @Override
    public boolean canRedo() {
        return delegate.canRedo();
    }

    @Override
    public String undo() {
        return delegate.undo();
    }

    @Override
    public String redo() {
        return delegate.redo();
    }
}
