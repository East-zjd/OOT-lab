package edu.lab.plugins.xml;

import edu.lab.core.editor.Editor;
import edu.lab.core.editor.EditorKind;
import edu.lab.core.spell.SpellCheckIssue;
import edu.lab.core.spell.SpellCheckService;
import edu.lab.core.workspace.LineCol;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * XML 编辑器。
 */
public final class XmlEditor implements Editor {
    private final Path file;
    private final SpellCheckService spellCheckService;
    private XmlDocument document;

    private final Deque<XmlCommand> undo = new ArrayDeque<>();
    private final Deque<XmlCommand> redo = new ArrayDeque<>();

    public XmlEditor(Path file, List<String> lines, SpellCheckService spellCheckService) {
        this.file = file;
        this.spellCheckService = spellCheckService;
        this.document = XmlParser.parse(lines);
    }

    @Override
    public EditorKind kind() {
        return EditorKind.XML;
    }

    @Override
    public Path file() {
        return file;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isLogEnabled() {
        return false;
    }

    @Override
    public void setLogEnabled(boolean enabled) {
        // no-op
    }

    @Override
    public List<String> lines() {
        return XmlSerializer.toLines(document);
    }

    @Override
    public void setLines(List<String> lines, boolean markSaved) {
        this.document = XmlParser.parse(lines);
        undo.clear();
        redo.clear();
    }

    @Override
    public void markSaved() {
        // no-op
    }

    @Override
    public String append(String text) {
        return notSupported();
    }

    @Override
    public String insert(LineCol pos, String text) {
        return notSupported();
    }

    @Override
    public String delete(LineCol pos, int len) {
        return notSupported();
    }

    @Override
    public String replace(LineCol pos, int len, String text) {
        return notSupported();
    }

    @Override
    public String show(Integer startLineOrNull, Integer endLineOrNull) {
        return notSupported();
    }

    @Override
    public String spellCheck() {
        List<String> outputs = new ArrayList<>();
        collectSpellIssues(document.root(), outputs);
        if (outputs.isEmpty()) {
            return "(spell-check) OK";
        }
        StringBuilder sb = new StringBuilder("拼写检查结果:\n");
        for (String line : outputs) {
            sb.append(line).append('\n');
        }
        return sb.toString().trim();
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
        XmlCommand cmd = undo.pop();
        cmd.undo();
        redo.push(cmd);
        return "(undo) ok";
    }

    @Override
    public String redo() {
        if (redo.isEmpty()) {
            return "(redo) nothing";
        }
        XmlCommand cmd = redo.pop();
        cmd.execute();
        undo.push(cmd);
        return "(redo) ok";
    }

    @Override
    public String insertBefore(String tagName, String newId, String targetId, String textOrNull) {
        String tagErr = validateTagName(tagName);
        if (tagErr != null) {
            return tagErr;
        }
        XmlElement target = document.findById(targetId);
        if (target == null) {
            return "目标元素不存在: " + targetId;
        }
        if (document.idExists(newId)) {
            return "元素ID已存在: " + newId;
        }
        XmlElement parent = target.parent();
        if (parent == null) {
            return "不能在根元素前插入元素";
        }
        XmlElement created = createElement(tagName, newId, textOrNull);
        int index = parent.children().indexOf(target);
        return applyCommand(new InsertBeforeCommand(parent, created, index));
    }

    @Override
    public String appendChild(String tagName, String newId, String parentId, String textOrNull) {
        String tagErr = validateTagName(tagName);
        if (tagErr != null) {
            return tagErr;
        }
        XmlElement parent = document.findById(parentId);
        if (parent == null) {
            return "父元素不存在: " + parentId;
        }
        if (document.idExists(newId)) {
            return "元素ID已存在: " + newId;
        }
        XmlElement created = createElement(tagName, newId, textOrNull);
        return applyCommand(new AppendChildCommand(parent, created));
    }

    @Override
    public String editId(String oldId, String newId) {
        XmlElement target = document.findById(oldId);
        if (target == null) {
            return "元素不存在: " + oldId;
        }
        if (target == document.root()) {
            return "不建议修改根元素ID";
        }
        if (document.idExists(newId)) {
            return "目标ID已存在: " + newId;
        }
        return applyCommand(new EditIdCommand(target, oldId, newId));
    }

    @Override
    public String editText(String elementId, String textOrNull) {
        XmlElement target = document.findById(elementId);
        if (target == null) {
            return "元素不存在: " + elementId;
        }
        return applyCommand(new EditTextCommand(target, target.getText(), textOrNull));
    }

    @Override
    public String deleteElement(String elementId) {
        XmlElement target = document.findById(elementId);
        if (target == null) {
            return "元素不存在: " + elementId;
        }
        if (target == document.root()) {
            return "不能删除根元素";
        }
        XmlElement parent = target.parent();
        int index = parent.children().indexOf(target);
        return applyCommand(new DeleteElementCommand(parent, target, index));
    }

    @Override
    public String xmlTree() {
        return XmlSerializer.toTree(document);
    }

    private void collectSpellIssues(XmlElement element, List<String> outputs) {
        String text = element.getText();
        if (text != null && !text.isBlank()) {
            List<SpellCheckIssue> issues = spellCheckService.checkText(text);
            for (SpellCheckIssue issue : issues) {
                outputs.add("元素 " + element.id() + ": \"" + issue.word() + "\" -> 建议: " + issue.suggestion());
            }
        }
        for (XmlNode node : element.children()) {
            if (node instanceof XmlElement child) {
                collectSpellIssues(child, outputs);
            }
        }
    }

    private XmlElement createElement(String tagName, String id, String text) {
        XmlElement element = new XmlElement(tagName, java.util.Map.of("id", id));
        element.setText(text);
        return element;
    }

    private String validateTagName(String tagName) {
        if (tagName == null || tagName.isBlank()) {
            return "非法 tagName: 不能为空";
        }
        String s = tagName.trim();
        // 简化校验：兼容常见 XML Name（支持可选的命名空间前缀 ns:tag）。
        // 规则：整体必须匹配 [A-Za-z_][A-Za-z0-9_.-]*(:[A-Za-z_][A-Za-z0-9_.-]*)?
        if (!s.matches("[A-Za-z_][A-Za-z0-9_.-]*(?::[A-Za-z_][A-Za-z0-9_.-]*)?")) {
            return "非法 tagName: " + s + "（XML 标签名不能以数字开头，且只能包含字母/数字/._- 及可选的前缀:）";
        }
        return null;
    }

    private String applyCommand(XmlCommand cmd) {
        cmd.execute();
        undo.push(cmd);
        redo.clear();
        return "ok";
    }

    private String notSupported() {
        return "不支持";
    }

    private interface XmlCommand {
        void execute();

        void undo();
    }

    private final class InsertBeforeCommand implements XmlCommand {
        private final XmlElement parent;
        private final XmlElement created;
        private final int index;

        private InsertBeforeCommand(XmlElement parent, XmlElement created, int index) {
            this.parent = parent;
            this.created = created;
            this.index = index;
        }

        @Override
        public void execute() {
            parent.addChild(index, created);
            document.registerElement(created);
        }

        @Override
        public void undo() {
            parent.removeChild(created);
            document.unregisterElement(created);
        }
    }

    private final class AppendChildCommand implements XmlCommand {
        private final XmlElement parent;
        private final XmlElement created;

        private AppendChildCommand(XmlElement parent, XmlElement created) {
            this.parent = parent;
            this.created = created;
        }

        @Override
        public void execute() {
            parent.addChild(created);
            document.registerElement(created);
        }

        @Override
        public void undo() {
            parent.removeChild(created);
            document.unregisterElement(created);
        }
    }

    private final class EditIdCommand implements XmlCommand {
        private final XmlElement target;
        private final String oldId;
        private final String newId;

        private EditIdCommand(XmlElement target, String oldId, String newId) {
            this.target = target;
            this.oldId = oldId;
            this.newId = newId;
        }

        @Override
        public void execute() {
            document.unregisterElement(target);
            target.setId(newId);
            document.registerElement(target);
        }

        @Override
        public void undo() {
            document.unregisterElement(target);
            target.setId(oldId);
            document.registerElement(target);
        }
    }

    private final class EditTextCommand implements XmlCommand {
        private final XmlElement target;
        private final String oldText;
        private final String newText;

        private EditTextCommand(XmlElement target, String oldText, String newText) {
            this.target = target;
            this.oldText = oldText == null ? "" : oldText;
            this.newText = newText == null ? "" : newText;
        }

        @Override
        public void execute() {
            target.setText(newText);
        }

        @Override
        public void undo() {
            target.setText(oldText);
        }
    }

    private final class DeleteElementCommand implements XmlCommand {
        private final XmlElement parent;
        private final XmlElement target;
        private final int index;

        private DeleteElementCommand(XmlElement parent, XmlElement target, int index) {
            this.parent = parent;
            this.target = target;
            this.index = index;
        }

        @Override
        public void execute() {
            parent.removeChild(target);
            document.unregisterElement(target);
        }

        @Override
        public void undo() {
            parent.addChild(index, target);
            document.registerElement(target);
        }
    }
}
