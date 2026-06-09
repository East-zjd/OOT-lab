package edu.lab.core.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link TextEscapes} 的单元测试。
 * <p>
 * 只覆盖编辑类命令支持的最小转义集合：\n、\t、\"、\\。
 */
class TextEscapesTest {
    @Test
    void unescape_returns_input_for_null_or_empty() {
        assertNull(TextEscapes.unescape(null));
        assertEquals("", TextEscapes.unescape(""));
    }

    @Test
    void unescape_decodes_supported_escapes() {
        assertEquals("a\nb", TextEscapes.unescape("a\\nb"));
        assertEquals("a\tb", TextEscapes.unescape("a\\tb"));
        assertEquals("\"", TextEscapes.unescape("\\\""));
        assertEquals("\\", TextEscapes.unescape("\\\\"));
    }

    @Test
    void unescape_keeps_unknown_escapes_as_is() {
        assertEquals("\\q", TextEscapes.unescape("\\q"));
        assertEquals("x\\y", TextEscapes.unescape("x\\y"));
    }

    @Test
    void unescape_keeps_trailing_backslash() {
        assertEquals("\\", TextEscapes.unescape("\\"));
        assertEquals("a\\", TextEscapes.unescape("a\\"));
    }
}
