package edu.lab.core.editor;

import edu.lab.core.workspace.LineCol;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于“按行存储”的文本编辑器实现。
 * <p>
 * 关键特性：
 * <ul>
 *   <li>内容按行保存，支持插入/删除/替换/追加</li>
 *   <li>维护保存基线，用于判断是否已修改</li>
 *   <li>通过保存 before/after 快照实现撤销/重做</li>
 *   <li>内置简单英文词典的拼写检查</li>
 * </ul>
 */
public final class TextEditor implements Editor {
    // 单词匹配：仅匹配英文字母单词，允许包含一次撇号（如 don't）
    private static final Pattern WORD = Pattern.compile("[A-Za-z]+(?:'[A-Za-z]+)?");

    private static final String ERR_OOB = "行号或列号越界";
    private static final String ERR_DELETE_LEN_INVALID = "删除长度不合法";
    private static final String ERR_DELETE_PAST_END = "删除长度超出行尾";

    private final java.nio.file.Path file;
    private List<String> lines;
    private List<String> savedLines;
    private boolean savedBaselineInitialized;

    private final Deque<SnapshotEdit> undo = new ArrayDeque<>();
    private final Deque<SnapshotEdit> redo = new ArrayDeque<>();

    /**
     * @param file         目标文件路径
     * @param initialLines 初始内容（按行）
     * @param markAsSaved  是否将初始内容视为“已保存”
     */
    public TextEditor(java.nio.file.Path file, List<String> initialLines, boolean markAsSaved) {
        this.file = file;
        this.lines = new ArrayList<>(initialLines);
        this.savedBaselineInitialized = markAsSaved;
        this.savedLines = new ArrayList<>(markAsSaved ? initialLines : List.of());
    }

    @Override
    public java.nio.file.Path file() {
        return file;
    }

    @Override
    public boolean isModified() {
        // 未初始化保存基线，或当前内容与保存快照不一致，都视为已修改
        return !savedBaselineInitialized || !lines.equals(savedLines);
    }

    @Override
    public boolean isLogEnabled() {
        return false;
    }

    @Override
    public void setLogEnabled(boolean enabled) {
        // no-op: 日志能力由装饰器提供
    }

    @Override
    public List<String> lines() {
        return List.copyOf(lines);
    }

    @Override
    public void setLines(List<String> lines, boolean markSaved) {
        // 直接替换内容，同时清空撤销/重做栈
        this.lines = new ArrayList<>(lines);
        if (markSaved) {
            this.savedBaselineInitialized = true;
            this.savedLines = new ArrayList<>(lines);
        }
        undo.clear();
        redo.clear();
    }

    @Override
    public void markSaved() {
        // 更新保存基线为当前内容
        this.savedBaselineInitialized = true;
        this.savedLines = new ArrayList<>(lines);
    }

    @Override
    public String append(String text) {
        // 追加：将输入拆成多行并添加到末尾
        return applyEdit(before -> {
            List<String> toAppend = splitToLines(text);
            before.addAll(toAppend);
        });
    }

    @Override
    public String insert(LineCol pos, String text) {
        return applyEdit(before -> {
            if (before.isEmpty()) {
                // 空文件：只允许在 1:1 插入
                require(pos.line() == 1 && pos.col() == 1, "空文件只能在1:1位置插入");
                before.addAll(splitToLines(text));
                return;
            }

            int lineIdx = pos.line() - 1;
            require(lineIdx >= 0 && lineIdx < before.size(), ERR_OOB);
            String line = before.get(lineIdx);
            int colIdx = pos.col() - 1;
            require(colIdx >= 0 && colIdx <= line.length(), ERR_OOB);

            List<String> insertLines = splitToLines(text);
            if (insertLines.size() == 1) {
                // 单行插入：直接在原行中拼接
                String newLine = line.substring(0, colIdx) + insertLines.get(0) + line.substring(colIdx);
                before.set(lineIdx, newLine);
                return;
            }

            // 多行插入：将原行在 colIdx 处分成前后两段
            String first = line.substring(0, colIdx) + insertLines.get(0);
            String last = insertLines.get(insertLines.size() - 1) + line.substring(colIdx);

            before.set(lineIdx, first);
            for (int i = 1; i < insertLines.size() - 1; i++) {
                before.add(lineIdx + i, insertLines.get(i));
            }
            before.add(lineIdx + insertLines.size() - 1, last);
        });
    }

    @Override
    public String delete(LineCol pos, int len) {
        return applyEdit(before -> {
            require(len >= 0, ERR_DELETE_LEN_INVALID);
            require(!before.isEmpty(), ERR_OOB);
            int lineIdx = pos.line() - 1;
            require(lineIdx >= 0 && lineIdx < before.size(), ERR_OOB);
            String line = before.get(lineIdx);
            int colIdx = pos.col() - 1;
            require(colIdx >= 0 && colIdx <= line.length(), ERR_OOB);
            require(colIdx + len <= line.length(), ERR_DELETE_PAST_END);
            // 删除：保留 prefix + suffix
            String newLine = line.substring(0, colIdx) + line.substring(colIdx + len);
            before.set(lineIdx, newLine);
        });
    }

    @Override
    public String replace(LineCol pos, int len, String text) {
        return applyEdit(before -> {
            if (before.isEmpty()) {
                // 空文件：只允许在 1:1 用 len=0 替换（等价于插入）
                require(pos.line() == 1 && pos.col() == 1 && len == 0, ERR_OOB);
                before.addAll(splitToLines(text));
                return;
            }

            int lineIdx = pos.line() - 1;
            require(lineIdx >= 0 && lineIdx < before.size(), ERR_OOB);
            String line = before.get(lineIdx);
            int colIdx = pos.col() - 1;
            require(colIdx >= 0 && colIdx <= line.length(), ERR_OOB);
            require(colIdx + len <= line.length(), ERR_DELETE_PAST_END);

            // 将原行分成三段：prefix + (被替换的区间) + suffix
            String prefix = line.substring(0, colIdx);
            String suffix = line.substring(colIdx + len);
            List<String> insertLines = splitToLines(text);
            if (insertLines.isEmpty()) {
                // 替换为空：相当于只删除
                before.set(lineIdx, prefix + suffix);
                return;
            }
            if (insertLines.size() == 1) {
                // 单行替换：prefix + new + suffix
                before.set(lineIdx, prefix + insertLines.get(0) + suffix);
                return;
            }

            // 多行替换：第一行接 prefix，最后一行接 suffix，中间行直接插入
            before.set(lineIdx, prefix + insertLines.get(0));
            for (int i = 1; i < insertLines.size() - 1; i++) {
                before.add(lineIdx + i, insertLines.get(i));
            }
            before.add(lineIdx + insertLines.size() - 1, insertLines.get(insertLines.size() - 1) + suffix);
        });
    }

    @Override
    public String show(Integer startLineOrNull, Integer endLineOrNull) {
        if (lines.isEmpty()) {
            return "(empty)";
        }

        int start = startLineOrNull == null ? 1 : startLineOrNull;
        int end = endLineOrNull == null ? lines.size() : endLineOrNull;
        if (start < 1 || end < start || end > lines.size()) {
            return "行号范围越界";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= end; i++) {
            sb.append(i).append(": ").append(lines.get(i - 1)).append('\n');
        }
        return sb.toString().trim();
    }

    @Override
    public String spellCheck() {
        return "(spell-check) unavailable";
    }

    @Override
    public boolean canUndo() {
        return !undo.isEmpty();
    }

    @Override
    public boolean canRedo() {
        return !redo.isEmpty();
    }

    @Override
    public String undo() {
        if (undo.isEmpty()) {
            return "(undo) nothing";
        }
        // 撤销：把当前状态回退到 edit.before，并把 edit 放入 redo 栈
        SnapshotEdit edit = undo.pop();
        redo.push(edit);
        lines = new ArrayList<>(edit.before());
        return "(undo) ok";
    }

    @Override
    public String redo() {
        if (redo.isEmpty()) {
            return "(redo) nothing";
        }
        // 重做：把状态推进到 edit.after，并把 edit 放回 undo 栈
        SnapshotEdit edit = redo.pop();
        undo.push(edit);
        lines = new ArrayList<>(edit.after());
        return "(redo) ok";
    }

    private String applyEdit(UnsafeListMutator mutator) {
        // 使用 before/after 快照记录一次原子编辑，以支持撤销/重做
        List<String> before = new ArrayList<>(lines);
        List<String> after = new ArrayList<>(lines);
        mutator.mutate(after);
        lines = after;
        undo.push(new SnapshotEdit(before, after));
        redo.clear();
        return "ok";
    }

    private static List<String> splitToLines(String text) {
        // 统一把输入按 \n 拆分为行；保留空行（split 的 -1 参数）
        if (text == null) {
            return List.of();
        }
        if (text.isEmpty()) {
            return List.of("");
        }
        String[] parts = text.split("\\n", -1);
        List<String> lines = new ArrayList<>(parts.length);
        for (String p : parts) {
            lines.add(p);
        }
        return lines;
    }

    public static boolean shouldAutoEnableLog(List<String> loadedLines) {
        // 文件第一行是 "# log" 时自动开启日志
        return !loadedLines.isEmpty() && "# log".equals(loadedLines.get(0).trim());
    }

    public interface SpellChecker {
        Set<SpellIssue> check(List<String> lines);

        /**
         * 创建一个默认的英文拼写检查器（小型内置词典）。
         */
        static SpellChecker defaultEnglish() {
            Set<String> dict = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            dict.addAll(Set.of(
                    "a", "an", "and", "are", "as", "at", "be", "brown", "code", "contains", "day", "dog",
                    "extra", "fox", "good", "hello", "is", "it", "jumps", "lazy", "line", "new", "of",
                    "over", "quick", "spaces", "test", "the", "this", "today", "world", "write", "writing"
            ));
            return new DictionarySpellChecker(dict);
        }
    }

    public record SpellIssue(int line, int col, String word) implements Comparable<SpellIssue> {
        @Override
        public int compareTo(SpellIssue o) {
            int c1 = Integer.compare(line, o.line);
            if (c1 != 0) return c1;
            int c2 = Integer.compare(col, o.col);
            if (c2 != 0) return c2;
            return word.compareToIgnoreCase(o.word);
        }
    }

    private static final class DictionarySpellChecker implements SpellChecker {
        private final Set<String> dictionary;

        private DictionarySpellChecker(Set<String> dictionary) {
            this.dictionary = dictionary;
        }

        @Override
        public Set<SpellIssue> check(List<String> lines) {
            Set<SpellIssue> issues = new TreeSet<>();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                Matcher m = WORD.matcher(line);
                while (m.find()) {
                    String word = m.group();
                    // 词典匹配不区分大小写：统一 lower 后判断
                    if (!dictionary.contains(word.toLowerCase(Locale.ROOT))) {
                        issues.add(new SpellIssue(i + 1, m.start() + 1, word));
                    }
                }
            }
            return issues;
        }
    }

    private record SnapshotEdit(List<String> before, List<String> after) {
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new EditorException(message);
        }
    }

    @FunctionalInterface
    private interface UnsafeListMutator {
        void mutate(List<String> lines);
    }

    public static final class EditorException extends RuntimeException {
        /**
         * 编辑器内部使用的可预期异常（如越界、非法参数等）。
         */
        public EditorException(String message) {
            super(message);
        }
    }
}
