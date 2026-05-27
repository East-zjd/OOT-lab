package edu.lab.core.workspace;

import edu.lab.core.console.Console;
import edu.lab.core.editor.Editor;
import edu.lab.core.editor.EditorKind;
import edu.lab.core.editor.LoggableEditorDecorator;
import edu.lab.core.editor.ModifiedEditorDecorator;
import edu.lab.core.editor.SpellCheckEditorDecorator;
import edu.lab.core.editor.TextEditor;
import edu.lab.core.events.CommandExecutedEvent;
import edu.lab.core.events.EditorActivatedEvent;
import edu.lab.core.events.EditorClosedEvent;
import edu.lab.core.events.EditorDeactivatedEvent;
import edu.lab.core.events.EventBus;
import edu.lab.core.fs.FileSystem;
import edu.lab.core.logging.LogService;
import edu.lab.core.persistence.WorkspacePersistence;
import edu.lab.core.spell.SpellCheckService;
import edu.lab.core.stats.StatisticsService;
import edu.lab.plugins.xml.XmlEditor;

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
    private final EventBus eventBus;
    private final SpellCheckService spellCheckService;
    private final StatisticsService statistics;

    private final Map<Path, Editor> openEditors = new LinkedHashMap<>();
    private final Deque<Path> mru = new ArrayDeque<>();
    private Path active;

    // 预留的全局日志开关：会持久化到快照中（当前逻辑主要以单文件开关为主）
    private boolean globalLogEnabled = true;

    public WorkspaceService(FileSystem fileSystem,
                           WorkspacePersistence persistence,
                           EventBus eventBus,
                           LogService logService,
                           Console console,
                           SpellCheckService spellCheckService,
                           StatisticsService statistics) {
        this.fileSystem = fileSystem;
        this.persistence = persistence;
        this.logService = logService;
        this.console = console;
        this.eventBus = eventBus;
        this.spellCheckService = spellCheckService;
        this.statistics = statistics;

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
            Editor ed = createEditor(p, lines, !es.modified());
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
        if (active != null) {
            eventBus.publish(new EditorActivatedEvent(active));
        }
    }

    @Override
    public String load(Path file) {
        Path p = normalize(file);
        if (openEditors.containsKey(p)) {
            // 已打开：只切换为活动编辑器即可
            switchActive(p);
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
        Editor ed = createEditor(p, lines, exists);
        boolean autoLog = TextEditor.shouldAutoEnableLog(lines);
        if (autoLog) {
            // 文件首行是 # log 时自动开启日志
            ed.setLogEnabled(true);
            logService.enable(p);
        }
        openEditors.put(p, ed);
        switchActive(p);
        return "ok";
    }

    @Override
    public String init(Path file, boolean withLog) {
        // 初始化一个新文件编辑器（默认不落盘；保存时才写入）
        Path p = normalize(file);
        List<String> lines = initialLinesFor(p, withLog);
        Editor ed = createEditor(p, lines, false);
        if (withLog) {
            ed.setLogEnabled(true);
            logService.enable(p);
        }
        openEditors.put(p, ed);
        switchActive(p);
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

        boolean wasActive = p.equals(active);
        if (wasActive) {
            eventBus.publish(new EditorDeactivatedEvent(p));
        }
        openEditors.remove(p);
        mru.remove(p);
        eventBus.publish(new EditorClosedEvent(p));
        if (wasActive) {
            // 若关闭的是活动编辑器，则将最近使用的下一个设为活动编辑器
            Path next = mru.peekFirst();
            if (next != null) {
                switchActive(next);
            } else {
                active = null;
            }
        }
        return "ok";
    }

    @Override
    public String edit(Path file) {
        Path p = normalize(file);
        if (!openEditors.containsKey(p)) {
            return "文件未打开: " + file;
        }
        switchActive(p);
        return "ok";
    }

    @Override
    public String listEditors(boolean tree) {
        if (openEditors.isEmpty()) {
            return "(empty)";
        }
        return tree ? listEditorsTree() : listEditorsFlat();
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
        if (active != null) {
            eventBus.publish(new EditorDeactivatedEvent(active));
        }
        for (Path p : new ArrayList<>(openEditors.keySet())) {
            eventBus.publish(new EditorClosedEvent(p));
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
        if (ed.kind() != EditorKind.TEXT) {
            return "不支持";
        }
        return safeEditorCall(() -> ed.append(text));
    }

    @Override
    public String insert(LineCol pos, String text) {
        Editor ed = activeEditorOrNull();
        if (ed == null) {
            return "(error) no active editor";
        }
        if (ed.kind() != EditorKind.TEXT) {
            return "不支持";
        }
        return safeEditorCall(() -> ed.insert(pos, text));
    }

    @Override
    public String delete(LineCol pos, int len) {
        Editor ed = activeEditorOrNull();
        if (ed == null) {
            return "(error) no active editor";
        }
        if (ed.kind() != EditorKind.TEXT) {
            return "不支持";
        }
        return safeEditorCall(() -> ed.delete(pos, len));
    }

    @Override
    public String replace(LineCol pos, int len, String text) {
        Editor ed = activeEditorOrNull();
        if (ed == null) {
            return "(error) no active editor";
        }
        if (ed.kind() != EditorKind.TEXT) {
            return "不支持";
        }
        return safeEditorCall(() -> ed.replace(pos, len, text));
    }

    @Override
    public String show(Integer startLineOrNull, Integer endLineOrNull) {
        Editor ed = activeEditorOrNull();
        if (ed == null) {
            return "(error) no active editor";
        }
        if (ed.kind() != EditorKind.TEXT) {
            return "不支持";
        }
        return ed.show(startLineOrNull, endLineOrNull);
    }

    @Override
    public String spellCheck(Path fileOrNull) {
        Editor ed = resolveEditor(fileOrNull);
        if (ed == null) {
            return "(error) no target editor";
        }
        return ed.spellCheck();
    }

    @Override
    public String insertBefore(String tagName, String newId, String targetId, String textOrNull) {
        Editor ed = activeEditorOrNull();
        if (ed == null) {
            return "(error) no active editor";
        }
        if (ed.kind() != EditorKind.XML) {
            return "不支持";
        }
        return safeEditorCall(() -> ed.insertBefore(tagName, newId, targetId, textOrNull));
    }

    @Override
    public String appendChild(String tagName, String newId, String parentId, String textOrNull) {
        Editor ed = activeEditorOrNull();
        if (ed == null) {
            return "(error) no active editor";
        }
        if (ed.kind() != EditorKind.XML) {
            return "不支持";
        }
        return safeEditorCall(() -> ed.appendChild(tagName, newId, parentId, textOrNull));
    }

    @Override
    public String editId(String oldId, String newId) {
        Editor ed = activeEditorOrNull();
        if (ed == null) {
            return "(error) no active editor";
        }
        if (ed.kind() != EditorKind.XML) {
            return "不支持";
        }
        return safeEditorCall(() -> ed.editId(oldId, newId));
    }

    @Override
    public String editText(String elementId, String textOrNull) {
        Editor ed = activeEditorOrNull();
        if (ed == null) {
            return "(error) no active editor";
        }
        if (ed.kind() != EditorKind.XML) {
            return "不支持";
        }
        return safeEditorCall(() -> ed.editText(elementId, textOrNull));
    }

    @Override
    public String deleteElement(String elementId) {
        Editor ed = activeEditorOrNull();
        if (ed == null) {
            return "(error) no active editor";
        }
        if (ed.kind() != EditorKind.XML) {
            return "不支持";
        }
        return safeEditorCall(() -> ed.deleteElement(elementId));
    }

    @Override
    public String xmlTree(Path fileOrNull) {
        Editor ed = resolveEditor(fileOrNull);
        if (ed != null) {
            if (ed.kind() != EditorKind.XML) {
                return "不支持";
            }
            return ed.xmlTree();
        }
        if (fileOrNull == null) {
            return "(error) no target editor";
        }
        if (!isXml(fileOrNull)) {
            return "不支持";
        }
        List<String> lines = readFileOrEmpty(normalize(fileOrNull));
        Editor temp = new XmlEditor(fileOrNull, lines, spellCheckService);
        return temp.xmlTree();
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

    private void switchActive(Path p) {
        if (p == null) {
            return;
        }
        if (p.equals(active)) {
            touchMru(p);
            return;
        }
        if (active != null) {
            eventBus.publish(new EditorDeactivatedEvent(active));
        }
        active = p;
        touchMru(p);
        eventBus.publish(new EditorActivatedEvent(p));
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

    private Editor createEditor(Path path, List<String> lines, boolean markSaved) {
        if (isXml(path)) {
            try {
                Editor core = new XmlEditor(path, lines, spellCheckService);
                return decorateXml(core, markSaved);
            } catch (IllegalArgumentException e) {
                // XML 不合法时不要让应用直接崩溃：降级为文本编辑器，让用户能够修复文件。
                console.print("(warn) XML 解析失败，已按文本打开: " + path.getFileName() + " - " + e.getMessage() + "\n");
                return decorateText(new TextEditor(path, lines, markSaved));
            }
        }
        return decorateText(new TextEditor(path, lines, markSaved));
    }

    private List<String> initialLinesFor(Path file, boolean withLog) {
        if (isXml(file)) {
            List<String> base = new ArrayList<>();
            if (withLog) {
                base.add("# log");
            }
            base.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            base.add("<root id=\"root\">");
            base.add("</root>");
            return base;
        }
        return withLog ? List.of("# log") : List.of();
    }

    private boolean isXml(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".xml");
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

    private Editor decorateText(Editor core) {
        return new SpellCheckEditorDecorator(
                new LoggableEditorDecorator(core),
                spellCheckService
        );
    }

    private Editor decorateXml(Editor core, boolean markSaved) {
        return new ModifiedEditorDecorator(new LoggableEditorDecorator(core), !markSaved);
    }

    private String listEditorsFlat() {
        StringBuilder sb = new StringBuilder();
        for (var entry : openEditors.entrySet()) {
            Path p = entry.getKey();
            Editor ed = entry.getValue();
            sb.append(p.equals(active) ? "* " : "  ");
            sb.append(p.getFileName());
            if (ed.isModified()) {
                sb.append(" [modified]");
            }
            sb.append(" (").append(statistics.formatDuration(p)).append(")");
            sb.append('\n');
        }
        if (sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private String listEditorsTree() {
        Path root = commonRoot();
        TreeNode tree = TreeNode.build(root, openEditors);
        StringBuilder sb = new StringBuilder();
        sb.append(root.toString()).append('\n');
        tree.render(sb, "", p -> formatTreeFile(p, openEditors.get(p)));
        if (sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private String formatTreeFile(Path p, Editor ed) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.equals(active) ? "* " : "  ");
        sb.append(p.getFileName());
        if (ed.isModified()) {
            sb.append(" [modified]");
        }
        sb.append(" (").append(statistics.formatDuration(p)).append(")");
        return sb.toString();
    }

    private Path commonRoot() {
        Path root = null;
        for (Path p : openEditors.keySet()) {
            Path abs = p.toAbsolutePath().normalize();
            if (root == null) {
                root = abs.getParent();
            } else {
                root = commonPrefix(root, abs.getParent());
            }
        }
        return root == null ? Path.of(".").toAbsolutePath().normalize() : root;
    }

    private Path commonPrefix(Path a, Path b) {
        int count = Math.min(a.getNameCount(), b.getNameCount());
        Path result = a.getRoot();
        for (int i = 0; i < count; i++) {
            if (!a.getName(i).equals(b.getName(i))) {
                break;
            }
            result = result == null ? a.getName(i) : result.resolve(a.getName(i));
        }
        return result == null ? Path.of(".") : result;
    }

    private static final class TreeNode {
        private final String name;
        private final Map<String, TreeNode> children = new LinkedHashMap<>();
        private Path filePath;

        private TreeNode(String name) {
            this.name = name;
        }

        private static TreeNode build(Path root, Map<Path, Editor> openEditors) {
            TreeNode node = new TreeNode(root.toString());
            for (Path path : openEditors.keySet()) {
                Path relative = root.relativize(path.toAbsolutePath().normalize());
                TreeNode cur = node;
                for (int i = 0; i < relative.getNameCount(); i++) {
                    String part = relative.getName(i).toString();
                    cur = cur.children.computeIfAbsent(part, TreeNode::new);
                }
                cur.filePath = path;
            }
            return node;
        }

        private void render(StringBuilder sb, String prefix, java.util.function.Function<Path, String> formatter) {
            int index = 0;
            int size = children.size();
            for (TreeNode child : children.values()) {
                boolean isLast = index == size - 1;
                sb.append(prefix).append(isLast ? "└── " : "├── ");
                if (child.filePath != null) {
                    sb.append(formatter.apply(child.filePath));
                } else {
                    sb.append(child.name);
                }
                sb.append('\n');
                if (!child.children.isEmpty()) {
                    child.render(sb, prefix + (isLast ? "    " : "│   "), formatter);
                }
                index++;
            }
        }
    }
}
