package edu.lab.core.spell;

import java.util.List;

/**
 * 拼写检查服务抽象。
 */
public interface SpellCheckService {
    /**
     * 对按行文本进行拼写检查。
     */
    List<SpellCheckIssue> checkLines(List<String> lines);

    /**
     * 对单段文本进行拼写检查。
     */
    default List<SpellCheckIssue> checkText(String text) {
        return checkLines(List.of(text));
    }
}
