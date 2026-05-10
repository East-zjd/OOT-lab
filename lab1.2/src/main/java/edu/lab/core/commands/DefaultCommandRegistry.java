package edu.lab.core.commands;

import edu.lab.core.events.CommandExecutedEvent;
import edu.lab.core.events.EventBus;
import edu.lab.core.workspace.LineCol;
import edu.lab.core.workspace.Workspace;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 默认命令注册表实现。
 * <p>
 * 负责：
 * <ul>
 *   <li>把用户输入解析为命令名/参数</li>
 *   <li>按命令名分发到对应处理器</li>
 *   <li>在命令成功执行后发布 {@link CommandExecutedEvent}（供日志等订阅）</li>
 * </ul>
 */
public final class DefaultCommandRegistry implements CommandRegistry {
    private final Workspace workspace;
    private final EventBus eventBus;

    // 命令名 -> 处理器
    private final Map<String, CommandHandler> handlers = new HashMap<>();

    public DefaultCommandRegistry(Workspace workspace, EventBus eventBus) {
        this.workspace = workspace;
        this.eventBus = eventBus;
        // 构造时注册所有内置命令
        registerAll();
    }

    @Override
    public ExecutionResult execute(String rawLine) {
        try {
            // 解析用户输入（支持引号与基础转义）
            ParsedCommand cmd = CommandLineTokenizer.parse(rawLine);
            CommandHandler handler = handlers.get(cmd.name());
            if (handler == null) {
                return ExecutionResult.ok("(error) unknown command: " + cmd.name());
            }
            // 分发到命令处理器
            return handler.handle(rawLine, cmd);
        } catch (Exception e) {
            // 任何异常都转换为可显示的错误消息，避免 REPL 崩溃
            return ExecutionResult.ok("(error) " + e.getMessage());
        }
    }

    private void registerAll() {
        // 文件/工作区相关
        register("load", this::handleLoad);
        register("save", this::handleSave);
        register("init", this::handleInit);
        register("close", this::handleClose);
        register("edit", this::handleEdit);
        register("editor-list", this::handleEditorList);
        register("dir-tree", this::handleDirTree);
        // 编辑器历史
        register("undo", this::handleUndo);
        register("redo", this::handleRedo);
        // 退出
        register("exit", this::handleExit);
        // 文本编辑
        register("append", this::handleAppend);
        register("insert", this::handleInsert);
        register("delete", this::handleDelete);
        register("replace", this::handleReplace);
        register("show", this::handleShow);
        // 拼写检查
        register("spell-check", this::handleSpellCheck);
        // 日志相关
        register("log-on", this::handleLogOn);
        register("log-off", this::handleLogOff);
        register("log-show", this::handleLogShow);
    }

    private void register(String name, CommandHandler handler) {
        CommandHandler previous = handlers.putIfAbsent(name, handler);
        if (previous != null) {
            throw new IllegalStateException("duplicate command: " + name);
        }
    }

    private ExecutionResult handleLoad(String raw, ParsedCommand c) {
        // load <file>
        requireArgs(c, 1);
        Path file = Path.of(c.args().get(0));
        String out = workspace.load(file);
        return okAndPublish(out, workspace.activeFileOrNull(), raw);
    }

    private ExecutionResult handleSave(String raw, ParsedCommand c) {
        // save [file|all]
        String out;
        Path target;
        if (c.args().isEmpty()) {
            out = workspace.saveActive();
            target = workspace.activeFileOrNull();
        } else if (c.args().size() == 1 && "all".equals(c.args().get(0))) {
            out = workspace.saveAll();
            target = workspace.activeFileOrNull();
        } else if (c.args().size() == 1) {
            target = Path.of(c.args().get(0));
            out = workspace.save(target);
        } else {
            throw new IllegalArgumentException("usage: save [file|all]");
        }
        return okAndPublish(out, resolveTargetOrActive(target), raw);
    }

    private ExecutionResult handleInit(String raw, ParsedCommand c) {
        // init <file> [with-log]
        requireArgsAtLeast(c, 1);
        Path file = Path.of(c.args().get(0));
        boolean withLog = c.args().size() >= 2 && "with-log".equals(c.args().get(1));
        String out = workspace.init(file, withLog);
        return okAndPublish(out, workspace.activeFileOrNull(), raw);
    }

    private ExecutionResult handleClose(String raw, ParsedCommand c) {
        // close [file]
        String out;
        Path target = null;
        if (c.args().isEmpty()) {
            // 关闭当前活动文件时，必须在关闭前捕获目标文件：
            // 1) 关闭后 active 可能切换到另一个文件
            // 2) 关闭后目标 editor 可能已从工作区移除
            target = workspace.activeFileOrNull();
            out = workspace.closeActive();
        } else if (c.args().size() == 1) {
            target = Path.of(c.args().get(0));
            out = workspace.close(target);
        } else {
            throw new IllegalArgumentException("usage: close [file]");
        }
        return okAndPublish(out, resolveTargetOrActive(target), raw);
    }

    private ExecutionResult handleEdit(String raw, ParsedCommand c) {
        // edit <file>
        requireArgs(c, 1);
        String out = workspace.edit(Path.of(c.args().get(0)));
        return okAndPublish(out, workspace.activeFileOrNull(), raw);
    }

    private ExecutionResult handleEditorList(String raw, ParsedCommand c) {
        // editor-list
        requireArgs(c, 0);
        String out = workspace.listEditors();
        return okAndPublish(out, workspace.activeFileOrNull(), raw);
    }

    private ExecutionResult handleDirTree(String raw, ParsedCommand c) {
        // dir-tree [path]
        String out;
        if (c.args().isEmpty()) {
            out = workspace.dirTree(null);
        } else if (c.args().size() == 1) {
            out = workspace.dirTree(Path.of(c.args().get(0)));
        } else {
            throw new IllegalArgumentException("usage: dir-tree [path]");
        }
        return okAndPublish(out, workspace.activeFileOrNull(), raw);
    }

    private ExecutionResult handleUndo(String raw, ParsedCommand c) {
        // undo
        requireArgs(c, 0);
        String out = workspace.undo();
        return okAndPublish(out, workspace.activeFileOrNull(), raw);
    }

    private ExecutionResult handleRedo(String raw, ParsedCommand c) {
        // redo
        requireArgs(c, 0);
        String out = workspace.redo();
        return okAndPublish(out, workspace.activeFileOrNull(), raw);
    }

    private ExecutionResult handleExit(String raw, ParsedCommand c) {
        // exit
        requireArgs(c, 0);
        String out = workspace.exit();
        return exitAndPublish(out, workspace.activeFileOrNull(), raw);
    }

    private ExecutionResult handleAppend(String raw, ParsedCommand c) {
        // append <text>
        requireArgs(c, 1);
        String out = workspace.append(TextEscapes.unescape(c.args().get(0)));
        return okAndPublish(out, workspace.activeFileOrNull(), raw);
    }

    private ExecutionResult handleInsert(String raw, ParsedCommand c) {
        // insert <line:col> <text>
        requireArgs(c, 2);
        LineCol pos = LineCol.parse(c.args().get(0));
        String out = workspace.insert(pos, TextEscapes.unescape(c.args().get(1)));
        return okAndPublish(out, workspace.activeFileOrNull(), raw);
    }

    private ExecutionResult handleDelete(String raw, ParsedCommand c) {
        // delete <line:col> <len>
        requireArgs(c, 2);
        LineCol pos = LineCol.parse(c.args().get(0));
        int len = Integer.parseInt(c.args().get(1));
        String out = workspace.delete(pos, len);
        return okAndPublish(out, workspace.activeFileOrNull(), raw);
    }

    private ExecutionResult handleReplace(String raw, ParsedCommand c) {
        // replace <line:col> <len> <text>
        requireArgs(c, 3);
        LineCol pos = LineCol.parse(c.args().get(0));
        int len = Integer.parseInt(c.args().get(1));
        String out = workspace.replace(pos, len, TextEscapes.unescape(c.args().get(2)));
        return okAndPublish(out, workspace.activeFileOrNull(), raw);
    }

    private ExecutionResult handleShow(String raw, ParsedCommand c) {
        // show [start:end]
        String out;
        if (c.args().isEmpty()) {
            out = workspace.show(null, null);
        } else if (c.args().size() == 1) {
            String[] parts = c.args().get(0).split(":", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("usage: show [start:end]");
            }
            Integer start = Integer.parseInt(parts[0]);
            Integer end = Integer.parseInt(parts[1]);
            out = workspace.show(start, end);
        } else {
            throw new IllegalArgumentException("usage: show [start:end]");
        }
        return okAndPublish(out, workspace.activeFileOrNull(), raw);
    }

    private ExecutionResult handleSpellCheck(String raw, ParsedCommand c) {
        // spell-check
        requireArgs(c, 0);
        String out = workspace.spellCheck();
        return okAndPublish(out, workspace.activeFileOrNull(), raw);
    }

    private ExecutionResult handleLogOn(String raw, ParsedCommand c) {
        // log-on [file]
        String out;
        Path target = null;
        if (c.args().isEmpty()) {
            out = workspace.logOn(null);
        } else if (c.args().size() == 1) {
            target = Path.of(c.args().get(0));
            out = workspace.logOn(target);
        } else {
            throw new IllegalArgumentException("usage: log-on [file]");
        }
        return okAndPublish(out, resolveTargetOrActive(target), raw);
    }

    private ExecutionResult handleLogOff(String raw, ParsedCommand c) {
        // log-off [file]
        String out;
        Path target = null;
        if (c.args().isEmpty()) {
            out = workspace.logOff(null);
        } else if (c.args().size() == 1) {
            target = Path.of(c.args().get(0));
            out = workspace.logOff(target);
        } else {
            throw new IllegalArgumentException("usage: log-off [file]");
        }
        return okAndPublish(out, resolveTargetOrActive(target), raw);
    }

    private ExecutionResult handleLogShow(String raw, ParsedCommand c) {
        // log-show [file]
        Path target = null;
        if (c.args().size() == 1) {
            target = Path.of(c.args().get(0));
        } else if (!c.args().isEmpty()) {
            throw new IllegalArgumentException("usage: log-show [file]");
        }

        // 关键点：为了让输出包含“本次 log-show”的日志记录，需要先发布事件（触发写日志），再读取日志内容。
        publish(resolveTargetOrActive(target), raw);
        String out = workspace.logShow(target);
        return ExecutionResult.ok(out);
    }

    private ExecutionResult okAndPublish(String output, Path editorFile, String raw) {
        // 执行成功后发布事件（给日志等订阅者）
        publish(editorFile, raw);
        return ExecutionResult.ok(output);
    }

    private ExecutionResult exitAndPublish(String output, Path editorFile, String raw) {
        // 退出类命令同样要发布事件（便于记录）
        publish(editorFile, raw);
        return ExecutionResult.exit(output);
    }

    private void requireArgs(ParsedCommand c, int n) {
        // 参数数量必须严格匹配
        if (c.args().size() != n) {
            throw new IllegalArgumentException("usage: " + c.name());
        }
    }

    private void requireArgsAtLeast(ParsedCommand c, int n) {
        // 参数数量至少为 n
        if (c.args().size() < n) {
            throw new IllegalArgumentException("usage: " + c.name());
        }
    }

    private Path resolveTargetOrActive(Path targetOrNull) {
        // 某些命令既可以操作指定文件，也可以默认使用当前活动文件
        if (targetOrNull != null) {
            return targetOrNull;
        }
        return workspace.activeFileOrNull();
    }

    private void publish(Path editorFile, String raw) {
        // 没有目标编辑器文件时不发布事件
        if (editorFile == null) {
            return;
        }
        eventBus.publish(new CommandExecutedEvent(editorFile, raw));
    }
}
