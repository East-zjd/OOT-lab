package edu.lab.core.spell;

/**
 * 拼写问题描述。
 */
public record SpellCheckIssue(int line, int col, String word, String suggestion) {
}
