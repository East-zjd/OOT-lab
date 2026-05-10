package edu.lab.core.workspace;

import java.nio.file.Path;

/**
 * 工作区抽象。
 * <p>
 * 工作区负责管理多个打开的编辑器（文件），并对外提供命令所需的操作：加载/保存/关闭、编辑、
 * 撤销/重做、查看内容、拼写检查、目录树、日志开关与状态持久化等。
 */
public interface Workspace {
    /**
     * 从持久化中恢复上次的工作区状态。
     */
    void restore();

    String load(Path file);

    String init(Path file, boolean withLog);

    String saveActive();

    String save(Path file);

    String saveAll();

    String closeActive();

    String close(Path file);

    String edit(Path file);

    String listEditors();

    String dirTree(Path pathOrNull);

    String undo();

    String redo();

    /**
        * 退出工作区。
        * <p>
        * 会触发必要的交互提示（例如询问是否保存已修改文件），并将状态持久化到磁盘。
     */
    String exit();

    Path activeFileOrNull();

    boolean isOpen(Path file);

    boolean isModified(Path file);

    boolean isLogEnabled(Path file);

    String logOn(Path fileOrNull);

    String logOff(Path fileOrNull);

    String logShow(Path fileOrNull);

    String append(String text);

    String insert(LineCol pos, String text);

    String delete(LineCol pos, int len);

    String replace(LineCol pos, int len, String text);

    String show(Integer startLineOrNull, Integer endLineOrNull);

    String spellCheck();
}
