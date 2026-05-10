package edu.lab.core.editor;

import edu.lab.core.workspace.LineCol;

import java.nio.file.Path;
import java.util.List;

/**
 * 文本编辑器抽象。
 * <p>
 * 一个 {@link Editor} 对应一个文件，维护文件内容（按行）、修改状态、日志开关以及撤销/重做历史。
 */
public interface Editor {
    /**
     * @return 当前编辑器对应的文件路径
     */
    Path file();

    /**
     * @return 是否存在未保存修改
     */
    boolean isModified();

    /**
     * @return 是否为该文件启用命令日志
     */
    boolean isLogEnabled();

    /**
     * 设置该文件的命令日志开关。
     */
    void setLogEnabled(boolean enabled);

    /**
     * @return 文件内容（按行的不可变快照）
     */
    List<String> lines();

    /**
     * 替换当前内容。
     *
     * @param lines     新内容（按行）
     * @param markSaved 是否将当前内容标记为“已保存基线”
     */
    void setLines(List<String> lines, boolean markSaved);

    /**
     * 将当前内容标记为已保存（更新保存基线）。
     */
    void markSaved();

    /**
     * 追加文本到末尾。
     */
    String append(String text);

    /**
     * 在指定位置插入文本。
     */
    String insert(LineCol pos, String text);

    /**
     * 从指定位置删除指定长度字符。
     */
    String delete(LineCol pos, int len);

    /**
     * 从指定位置删除 len 个字符，并插入 text。
     */
    String replace(LineCol pos, int len, String text);

    /**
     * 按行号区间显示内容。
     */
    String show(Integer startLineOrNull, Integer endLineOrNull);

    /**
     * 执行拼写检查，并返回问题列表。
     */
    String spellCheck();

    /**
     * @return 是否可以撤销
     */
    boolean canUndo();

    /**
     * @return 是否可以重做
     */
    boolean canRedo();

    /**
     * 撤销上一次编辑操作。
     */
    String undo();

    /**
     * 重做上一次被撤销的操作。
     */
    String redo();
}
