package edu.lab.core.editor;

import edu.lab.core.spell.SpellCheckIssue;
import edu.lab.core.spell.SpellCheckService;

import java.util.List;

/**
 * 为 Editor 增加拼写检查能力的装饰器。
 */
public final class SpellCheckEditorDecorator extends EditorDecorator {
    private final SpellCheckService spellCheckService;

    public SpellCheckEditorDecorator(Editor delegate, SpellCheckService spellCheckService) {
        super(delegate);
        this.spellCheckService = spellCheckService;
    }

    @Override
    public String spellCheck() {
        List<SpellCheckIssue> issues = spellCheckService.checkLines(lines());
        if (issues.isEmpty()) {
            return "(spell-check) OK";
        }
        StringBuilder sb = new StringBuilder("拼写检查结果:\n");
        for (SpellCheckIssue issue : issues) {
            sb.append("第").append(issue.line())
                    .append("行，第").append(issue.col())
                    .append("列: \"").append(issue.word())
                    .append("\" -> 建议: ").append(issue.suggestion())
                    .append('\n');
        }
        return sb.toString().trim();
    }
}
