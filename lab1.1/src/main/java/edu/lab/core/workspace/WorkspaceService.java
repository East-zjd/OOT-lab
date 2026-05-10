package edu.lab.core.workspace;

import edu.lab.core.console.Console;
import edu.lab.core.editor.Editor;
import edu.lab.core.editor.LoggableEditorDecorator;
import edu.lab.core.editor.SpellCheckEditorDecorator;
import edu.lab.core.editor.TextEditor;
import edu.lab.core.events.CommandExecutedEvent;
import edu.lab.core.events.EventBus;
import edu.lab.core.fs.FileSystem;
import edu.lab.core.logging.LogService;
import edu.lab.core.persistence.WorkspacePersistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link Workspace} 的默认实现。
 * <p>
 * 负责：
 * <ul>
 *   <li>维护打开的编辑器集合与活动编辑器</li>
 *   <li>处理 load/init/save/close/edit 等工作区命令</li>
 *   <li>协调日志服务（按文件开关）与命令执行事件</li>
 *   <li>将工作区状态持久化/恢复</li>
 * </ul>
 */
public final class WorkspaceService implements Workspace {
    private final FileSystem fileSystem;
    private final WorkspacePersistence persistence;
    private final LogService logService;
    private final Console console;

    private final Map<Path, Editor> openEditors = new LinkedHashMap<>();
    private final Deque<Path> mru = new ArrayDeque<>();
    private Path active;

    // 预留的全局日志开关：会持久化到快照中（当前逻辑主要以单文件开关为主）
    private boolean globalLogEnabled = true;

    public WorkspaceService(FileSystem fileSystem,
                           WorkspacePersistence persistence,
                           EventBus eventBus,
                           LogService logService,
                           Console console) {
        this.fileSystem = fileSystem;
        this.persistence = persistence;
        this.logService = logService;
        this.console = console;

        // 订阅“命令已执行”事件：如果该文件开启日志，则追加一条命令到日志文件
        eventBus.subscribe(CommandExecutedEvent.class, ev -> {
            if (ev.editorFile() == null) {
                return;
            }
            Path file = normalize(ev.editorFile());
            // 不依赖 editor 仍在 openEditors 中：例如 close 命令执行后 editor 已移除，但仍应记录 close。
            if (logService.isEnabled(file)) {
                logService.logCommand(file, ev.rawCommandLine());
            }
        });
    }

    @Override
    public void restore() {
        // 从持久化加载快照；失败时返回空快照
        WorkspaceSnapshot snapshot = persistence.loadOrEmpty();
        globalLogEnabled = snapshot.globalLogEnabled();
        for (WorkspaceSnapshot.EditorSnapshot es : snapshot.openEditors()) {
            Path p = normalize(es.path());
            // 读取文件内容作为编辑器初始内容；如果读取失败则当作空文件
            List<String> lines = readFileOrEmpty(p);
            // 根据快照中的 modified 决定是否初始化保存基线：
            // modified=false => 视为已保存；modified=true => 视为存在未保存改动（哪怕内容为空）。
            Editor ed = decorate(new TextEditor(p, lines, !es.modified()));
            ed.setLogEnabled(es.logEnabled());
            if (es.logEnabled()) {
                logService.enable(p);
            }
            openEditors.put(p, ed);
            touchMru(p);
        }
        if (snapshot.activeFile() != null) {
            Path p = normalize(snapshot.activeFile());
            if (openEditors.containsKey(p)) {
                active = p;
            }
        }
        if (active == null && !openEditors.isEmpty()) {
            // 若快照中没有活动文件，则默认选择第一个打开的编辑器
            active = openEditors.keySet().iterator().next();
        }
    }

    @Override
    public String load(Path file) {
        Path p = normalize(file);
        if (openEditors.containsKey(p)) {
            // 已打开：只切换为活动编辑器即可
            active = p;
            touchMru(p);
            return "已打开: " + p;
        }

        boolean exists = fileSystem.exists(p);
        if (!exists) {
            try {
                // 若文件不存在：尽量创建一个空文件（失败也不阻塞加载，仍会创建缓冲区）
                fileSystem.writeString(p, "", StandardCharsets.UTF_8);
            } catch (IOException e) {
                // ignore; buffer still created
            }
        }

        List<String> lines = readFileOrEmpty(p);
        Editor ed = decorate(new TextEditor(p, lines, exists));
        boolean autoLog = TextEditor.shouldAutoEnableLog(lines);
        if (autoLog) {
            // 文件首行是 # log 时自动开启日志
            ed.setLogEnabled(true);
            logService.enable(p);
        }
        openEditors.put(p, ed);
        active = p;
        touchMru(p);
        return "ok";
    }

    @Override
    public String init(Path file, boolean withLog) {
        // 初始化一个新文件编辑器（默认不落盘；保存时才写入）
        Path p = normalize(file);
        List<String> lines = withLog ? List.of("# log") : List.of();
        Editor ed = decorate(new TextEditor(p, lines, false));
        if (withLog) {
            ed.setLogEnabled(true);
            logService.enable(p);
        }
        openEditors.put(p, ed);
        active = p;
        touchMru(p);
        return "ok";
    }

    @Override
    public String saveActive() {
        Editor ed = activeEditorOrNull();
        if (ed == null) {
            return "(error) no active editor";
        }
        return save(ed.file());
    }

    @Override
    public String save(Path file) {
        Path p = normalize(file);
        Editor ed = openEditors.get(p);
        if (ed == null) {
            return "(error) 文件未打开: " + file;
        }
        try {
            // 以 \n 拼接行并写回磁盘
            String content = String.join("\n", ed.lines());
            fileSystem.writeString(p, content, StandardCharsets.UTF_8);
            ed.markSaved();
            return "ok";
        } catch (IOException e) {
            return "(error) 保存失败: " + e.getMessage();
        }
    }

    @Override
    public String saveAll() {
        StringBuilder sb = new StringBuilder();
        for (Path p : openEditors.keySet()) {
            String r = save(p);
            if (!"ok".equals(r)) {
                sb.append(p).append(": ").append(r).append('\n');
            }
        }
        String s = sb.toString().trim();
        return s.isEmpty() ? "ok" : s;
    }

    @Override
    public String closeActive() {
        Editor ed = activeEditorOrNull();
        if (ed == null) {
            return "(error) no active editor";
        }
        return close(ed.file());
    }

    @Override
    public String close(Path file) {
        Path p = normalize(file);
        Editor ed = openEditors.get(p);
        if (ed == null) {
            return "(error) 文件未打开: " + file;
        }

        if (ed.isModified()) {
            // 关闭前，如果存在未保存修改，则询问用户是否保存
            console.print("文件已修改，是否保存? (y/n) ");
            String ans = console.readLine();
            if (ans != null && ans.trim().equalsIgnoreCase("y")) {
                String saved = save(p);
                if (!"ok".equals(saved)) {
                    return saved;
                }
            }
        }

        openEditors.remove(p);
        mru.remove(p);
        if (p.equals(active)) {
            // 若关闭的是活动编辑器，则将最近使用的下一个设为活动编辑器
            active = mru.peekFirst();
        }
        return "ok";
    }

    @Override
    public String edit(Path file) {
        Path p = normalize(file);
        if (!openEditors.containsKey(p)) {
            return "文件未打开: " + file;
        }
        active = p;
        touchMru(p);
        return "ok";
    }

    @Override
    public String listEditors() {
        // 按打开顺序列出编辑器，并用 * 标记当前活动编辑器
        StringBuilder sb = new StringBuilder();
        for (var entry : openEditors.entrySet()) {
            Path p = entry.getKey();
            Editor ed = entry.getValue();
            sb.append(p.equals(active) ? "* " : "  ");
            sb.append(p.getFileName());
            if (ed.isModified()) {
                sb.append(" [modified]");
            }
            sb.append('\n');
        }
        if (sb.isEmpty()) {
            return "(empty)";
        }
        // 只移除最后的换行，不能用 trim()，否则会误删第一行前导空格（非活动文件的 "  " 前缀）
        if (sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    @Override
    public String dirTree(Path pathOrNull) {
        // 默认从当前目录开始打印
        Path root = pathOrNull == null ? Path.of(".") : pathOrNull;
        root = root.toAbsolutePath().normalize();
        if (!fileSystem.exists(root) || !fileSystem.isDirectory(root)) {
            return "(error) not a directory: " + root;
        }
        try {
            return DirTreePrinter.print(fileSystem, root);
        } catch (IOException e) {
            return "(error) dir-tree failed: " + e.getMessage();
        }
    }

    @Override
    public String undo() {
        Editor ed = activeEditorOrNull();
        return ed == null ? "(error) no active editor" : ed.undo();
    }

    @Override
    public String redo() {
        Editor ed = activeEditorOrNull();
        return ed == null ? "(error) no active editor" : ed.redo();
    }

    @Override
    public String exit() {
        // 退出时对所有已修改文件逐个询问是否保存
        for (Path p : new ArrayList<>(openEditors.keySet())) {
            Editor ed = openEditors.get(p);
            if (ed != null && ed.isModified()) {
                console.print(p.getFileName() + " 已修改，是否保存? (y/n) ");
                String ans = console.readLine();
                if (ans != null && ans.trim().equalsIgnoreCase("y")) {
                    save(p);
                }
            }
        }
        // 保存工作区快照，便于下次启动恢复
        persistence.save(snapshot());
        return "bye";
    }

    @Override
    public Path activeFileOrNull() {
        return active;
    }

    @Override
    public boolean isOpen(Path file) {
        return openEditors.containsKey(normalize(file));
    }

    @Override
    public boolean isModified(Path file) {
        Editor ed = openEditors.get(normalize(file));
        return ed != null && ed.isModified();
    }

    @Override
    public boolean isLogEnabled(Path file) {
        Editor ed = openEditors.get(normalize(file));
        return ed != null && ed.isLogEnabled();
    }

    @Override
    public String logOn(Path fileOrNull) {
        Editor ed = resolveEditor(fileOrNull);
        if (ed == null) {
            return "(error) no target editor";
        }
        ed.setLogEnabled(true);
        logService.enable(ed.file());
        return "ok";
    }

    @Override
    public String logOff(Path fileOrNull) {
        Editor ed = resolveEditor(fileOrNull);
        if (ed == null) {
            return "(error) no target editor";
        }
        ed.setLogEnabled(false);
        logService.disable(ed.file());
        return "ok";
    }

    @Override
    public String logShow(Path fileOrNull) {
        Editor ed = resolveEditor(fileOrNull);
        if (ed == null) {
            return "(error) no target editor";
        }
        return logService.show(ed.file());
    }

    @Override
    public String append(String text) {
        Editor ed = activeEditorOrNull();
        if (ed == null) {
            return "(error) no active editor";
        }
        return safeEditorCall(() -> ed.append(text));
    }

    @Override
    public String insert(LineCol pos, String text) {
        Editor ed = activeEditorOrNull();
        if (ed == null) {
            return "(error) no active editor";
        }
        return safeEditorCall(() -> ed.insert(pos, text));
    }

    @Override
    public String delete(LineCol pos, int len) {
        Editor ed = activeEditorOrNull();
        if (ed == null) {
            return "(error) no active editor";
        }
        return safeEditorCall(() -> ed.delete(pos, len));
    }

    @Override
    public String replace(LineCol pos, int len, String text) {
        Editor ed = activeEditorOrNull();
        if (ed == null) {
            return "(error) no active editor";
        }
        return safeEditorCall(() -> ed.replace(pos, len, text));
    }

    @Override
    public String show(Integer startLineOrNull, Integer endLineOrNull) {
        Editor ed = activeEditorOrNull();
        if (ed == null) {
            return "(error) no active editor";
        }
        return ed.show(startLineOrNull, endLineOrNull);
    }

    @Override
    public String spellCheck() {
        Editor ed = activeEditorOrNull();
        if (ed == null) {
            return "(error) no active editor";
        }
        return ed.spellCheck();
    }

    private WorkspaceSnapshot snapshot() {
        // 组装当前工作区状态，用于持久化
        List<WorkspaceSnapshot.EditorSnapshot> list = new ArrayList<>();
        for (var entry : openEditors.entrySet()) {
            Editor ed = entry.getValue();
            list.add(new WorkspaceSnapshot.EditorSnapshot(entry.getKey(), ed.isModified(), ed.isLogEnabled()));
        }
        return new WorkspaceSnapshot(list, active, globalLogEnabled);
    }

    private void touchMru(Path p) {
        // 更新最近使用顺序：移除旧位置并放到队首
        mru.remove(p);
        mru.addFirst(p);
    }

    private Editor activeEditorOrNull() {
        return active == null ? null : openEditors.get(active);
    }

    private Editor resolveEditor(Path fileOrNull) {
        if (fileOrNull == null) {
            return activeEditorOrNull();
        }
        return openEditors.get(normalize(fileOrNull));
    }

    private List<String> readFileOrEmpty(Path p) {
        try {
            if (!fileSystem.exists(p)) {
                return List.of();
            }
            return fileSystem.readAllLines(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // 读取失败时返回空内容，避免影响编辑器打开
            return List.of();
        }
    }

    private Path normalize(Path p) {
        return fileSystem.normalize(p);
    }

    private String safeEditorCall(UnsafeStringSupplier action) {
        try {
            return action.get();
        } catch (TextEditor.EditorException e) {
            // 将可预期的编辑器异常转换为用户可见消息
            return e.getMessage();
        } catch (Exception e) {
            // 其它异常做兜底处理
            return "(error) " + e.getMessage();
        }
    }

    @FunctionalInterface
    private interface UnsafeStringSupplier {
        String get();
    }

    private static Editor decorate(Editor core) {
        // 先添加日志能力，再叠加拼写检查能力
        return new SpellCheckEditorDecorator(
                new LoggableEditorDecorator(core),
                TextEditor.SpellChecker.defaultEnglish()
        );
    }
}
