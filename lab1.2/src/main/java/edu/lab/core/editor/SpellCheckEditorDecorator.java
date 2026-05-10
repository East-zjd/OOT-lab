package edu.lab.core.editor;

import java.util.Set;

/**
 * 为 Editor 增加拼写检查能力的装饰器。
 */
public final class SpellCheckEditorDecorator extends EditorDecorator {
    private final TextEditor.SpellChecker spellChecker;

    public SpellCheckEditorDecorator(Editor delegate, TextEditor.SpellChecker spellChecker) {
        super(delegate);
        this.spellChecker = spellChecker;
    }

    @Override
    public String spellCheck() {
        Set<TextEditor.SpellIssue> issues = spellChecker.check(lines());
        if (issues.isEmpty()) {
            return "(spell-check) OK";
        }
        StringBuilder sb = new StringBuilder();
        for (TextEditor.SpellIssue issue : issues) {
            sb.append(issue.line()).append(':').append(issue.col()).append(' ').append(issue.word()).append('\n');
        }
        return sb.toString().trim();
    }
}
