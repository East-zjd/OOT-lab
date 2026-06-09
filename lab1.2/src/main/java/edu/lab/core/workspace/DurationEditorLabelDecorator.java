package edu.lab.core.workspace;

import edu.lab.core.editor.Editor;
import edu.lab.core.stats.StatisticsService;

import java.nio.file.Path;

/**
 * 为 editor-list 标签追加会话编辑时长的装饰器。
 */
public final class DurationEditorLabelDecorator implements EditorLabelFormatter {
    private final EditorLabelFormatter delegate;
    private final StatisticsService statistics;

    public DurationEditorLabelDecorator(EditorLabelFormatter delegate, StatisticsService statistics) {
        this.delegate = delegate;
        this.statistics = statistics;
    }

    @Override
    public String format(Path file, Editor editor, boolean active) {
        String base = delegate.format(file, editor, active);
        return base + " (" + statistics.formatDuration(file) + ")";
    }
}
