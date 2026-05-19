package edu.lab.plugins.xml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * XML 元素节点。
 */
public final class XmlElement implements XmlNode {
    private final String tagName;
    private final Map<String, String> attributes;
    private final List<XmlNode> children = new ArrayList<>();
    private XmlElement parent;

    public XmlElement(String tagName, Map<String, String> attributes) {
        this.tagName = tagName;
        this.attributes = new LinkedHashMap<>(attributes);
    }

    public String tagName() {
        return tagName;
    }

    public XmlElement parent() {
        return parent;
    }

    public void setParent(XmlElement parent) {
        this.parent = parent;
    }

    public List<XmlNode> children() {
        return children;
    }

    public Map<String, String> attributes() {
        return attributes;
    }

    public String id() {
        return attributes.get("id");
    }

    public void setId(String newId) {
        attributes.put("id", newId);
    }

    public void addChild(XmlNode node) {
        if (node instanceof XmlElement element) {
            element.setParent(this);
        }
        children.add(node);
    }

    public void addChild(int index, XmlNode node) {
        if (node instanceof XmlElement element) {
            element.setParent(this);
        }
        children.add(index, node);
    }

    public void removeChild(XmlNode node) {
        children.remove(node);
    }

    public void setText(String text) {
        XmlText existing = findTextNode();
        if (text == null || text.isEmpty()) {
            if (existing != null) {
                children.remove(existing);
            }
            return;
        }
        if (existing != null) {
            existing.setText(text);
            return;
        }
        addChild(new XmlText(text));
    }

    public String getText() {
        XmlText existing = findTextNode();
        return existing == null ? "" : existing.text();
    }

    private XmlText findTextNode() {
        for (XmlNode node : children) {
            if (node instanceof XmlText text) {
                return text;
            }
        }
        return null;
    }

    @Override
    public String toTreeString(String prefix, boolean isLast) {
        String connector = isLast ? "└── " : "├── ";
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append(connector).append(tagName).append(" ");
        sb.append("[").append(renderAttributes()).append("]");
        String childPrefix = prefix + (isLast ? "    " : "│   ");
        for (int i = 0; i < children.size(); i++) {
            sb.append('\n');
            sb.append(children.get(i).toTreeString(childPrefix, i == children.size() - 1));
        }
        return sb.toString();
    }

    @Override
    public String toXmlString(String indent, String childIndent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append('<').append(tagName);
        for (var entry : attributes.entrySet()) {
            sb.append(' ').append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
        }
        sb.append('>');

        if (children.isEmpty()) {
            sb.append("</").append(tagName).append('>');
            return sb.toString();
        }

        sb.append('\n');
        for (int i = 0; i < children.size(); i++) {
            sb.append(children.get(i).toXmlString(childIndent, childIndent + "    "));
            sb.append('\n');
        }
        sb.append(indent).append("</").append(tagName).append('>');
        return sb.toString();
    }

    private String renderAttributes() {
        if (attributes.isEmpty()) {
            return "";
        }
        return attributes.entrySet().stream()
                .map(entry -> entry.getKey() + "=\"" + entry.getValue() + "\"")
                .collect(Collectors.joining(", "));
    }
}
