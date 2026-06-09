package edu.lab.core.spell;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LanguageToolHttpAdapterTest {
    @Test
    void parseIssues_reports_only_misspellings_and_maps_offsets_to_linecol() {
        String text = "Helol world\nThis is fine";
        // offset 0 => line 1 col 1 (Helol)
        // offset 6 => line 1 col 7 (world)
        String json = "{" +
                "\"matches\":[" +
                "{" +
                "  \"offset\":0,\"length\":5," +
                "  \"replacements\":[{\"value\":\"Hello\"}]," +
                "  \"rule\":{\"issueType\":\"misspelling\"}" +
                "}," +
                "{" +
                "  \"offset\":6,\"length\":5," +
                "  \"replacements\":[{\"value\":\"earth\"}]," +
                "  \"rule\":{\"issueType\":\"grammar\"}" +
                "}" +
                "]" +
                "}";

        List<SpellCheckIssue> issues = LanguageToolHttpAdapter.parseIssues(text, json);
        assertEquals(1, issues.size());
        SpellCheckIssue issue = issues.get(0);
        assertEquals(1, issue.line());
        assertEquals(1, issue.col());
        assertEquals("Helol", issue.word());
        assertEquals("Hello", issue.suggestion());
    }
}
