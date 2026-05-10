package edu.lab.core.editor;

import edu.lab.core.workspace.LineCol;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link TextEditor} 的单元测试。
 * <p>
 * 覆盖：追加/插入/删除/替换，以及撤销/重做的正确性与边界行为。
 */
class TextEditorTest {
    @Test
    void append_insert_delete_replace_and_undo_redo() {
        // 初始内容为单行 "abc"，并标记为已保存
        Editor ed = new TextEditor(Path.of("a.txt"), List.of("abc"), true);

        assertEquals("ok", ed.append("def"));
        assertEquals(List.of("abc", "def"), ed.lines());

        // 在 1:2 插入 "X" => aXbc
        assertEquals("ok", ed.insert(new LineCol(1, 2), "X"));
        assertEquals(List.of("aXbc", "def"), ed.lines());

        // 从 1:2 删除 2 个字符 => ac
        assertEquals("ok", ed.delete(new LineCol(1, 2), 2));
        assertEquals(List.of("ac", "def"), ed.lines());

        // 替换第二行前 3 个字符为 "ZZ"
        assertEquals("ok", ed.replace(new LineCol(2, 1), 3, "ZZ"));
        assertEquals(List.of("ac", "ZZ"), ed.lines());

        // 撤销后内容应回到替换前
        assertTrue(ed.canUndo());
        assertEquals("(undo) ok", ed.undo());
        assertEquals(List.of("ac", "def"), ed.lines());

        // 重做后内容应回到替换后
        assertTrue(ed.canRedo());
        assertEquals("(redo) ok", ed.redo());
        assertEquals(List.of("ac", "ZZ"), ed.lines());
    }

    @Test
    void insert_supports_newlines_split_into_multiple_lines() {
        // 插入文本包含 \n 时，编辑器应拆分为多行
        Editor ed = new TextEditor(Path.of("a.txt"), List.of("abcd"), true);
        assertEquals("ok", ed.insert(new LineCol(1, 3), "X\nY"));
        assertEquals(List.of("abX", "Ycd"), ed.lines());
    }

    @Test
    void delete_cannot_go_past_line_end() {
        // 删除长度超过行尾应抛出 EditorException
        Editor ed = new TextEditor(Path.of("a.txt"), List.of("abc"), true);
        var ex = assertThrows(TextEditor.EditorException.class, () -> ed.delete(new LineCol(1, 2), 5));
        assertEquals("删除长度超出行尾", ex.getMessage());
    }

    @Test
    void decorators_add_log_and_spellcheck_capabilities() {
        Editor ed = new SpellCheckEditorDecorator(
                new LoggableEditorDecorator(new TextEditor(Path.of("a.txt"), List.of("helo world"), true)),
                TextEditor.SpellChecker.defaultEnglish()
        );

        assertFalse(ed.isLogEnabled());
        ed.setLogEnabled(true);
        assertTrue(ed.isLogEnabled());

        String out = ed.spellCheck();
        assertTrue(out.contains("1:1 helo"));
    }
}
