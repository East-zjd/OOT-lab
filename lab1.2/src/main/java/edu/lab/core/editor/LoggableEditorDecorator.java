package edu.lab.core.editor;

/**
 * 为 Editor 增加“日志开关状态”的装饰器。
 */
public final class LoggableEditorDecorator extends EditorDecorator {
    private boolean logEnabled;

    public LoggableEditorDecorator(Editor delegate) {
        super(delegate);
    }

    @Override
    public boolean isLogEnabled() {
        return logEnabled;
    }

    @Override
    public void setLogEnabled(boolean enabled) {
        this.logEnabled = enabled;
    }
}
