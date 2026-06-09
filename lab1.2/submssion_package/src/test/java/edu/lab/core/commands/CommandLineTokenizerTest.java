package edu.lab.core.commands;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CommandLineTokenizer} 的单元测试。
 * <p>
 * 覆盖：空白分隔、引号参数、引号内转义、空引号 token、解析 name/args、未闭合引号异常。
 */
class CommandLineTokenizerTest {
    @Test
    void tokenize_splits_whitespace_and_quotes() {
        // 支持引号参数："a b.txt" 应被视为一个 token
        assertEquals(List.of("load", "a b.txt"), CommandLineTokenizer.tokenize("load \"a b.txt\""));
        assertEquals(List.of("append", "hello world"), CommandLineTokenizer.tokenize("append \"hello world\""));
    }

    @Test
    void tokenize_supports_escaped_quote_and_backslash_inside_quotes() {
        // 引号内支持 \" 与 \\ 的转义
        List<String> t1 = CommandLineTokenizer.tokenize("append \"he said \\\"hi\\\"\"");
        assertEquals(List.of("append", "he said \"hi\""), t1);

        List<String> t2 = CommandLineTokenizer.tokenize("append \"C:\\\\temp\\\\a.txt\"");
        assertEquals(List.of("append", "C:\\temp\\a.txt"), t2);
    }

    @Test
    void tokenize_keeps_empty_quoted_token() {
        // "" 应产生一个空字符串 token
        assertEquals(List.of("cmd", "", "x"), CommandLineTokenizer.tokenize("cmd \"\" x"));
    }

    @Test
    void parse_returns_name_and_args() {
        // parse：第一个 token 为命令名，其余为参数
        ParsedCommand c = CommandLineTokenizer.parse("save all");
        assertEquals("save", c.name());
        assertEquals(List.of("all"), c.args());

        // 全空白输入应解析为“空命令”
        ParsedCommand empty = CommandLineTokenizer.parse("   \t  ");
        assertEquals("", empty.name());
        assertEquals(List.of(), empty.args());
    }

    @Test
    void tokenize_throws_on_unterminated_quote() {
        // 未闭合引号应抛出 IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> CommandLineTokenizer.tokenize("load \"abc"));
    }
}
