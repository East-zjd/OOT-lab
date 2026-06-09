package edu.lab.plugins.xml;

import java.util.ArrayList;
import java.util.List;

/**
 * XML 序列化工具。
 */
public final class XmlSerializer {
    private XmlSerializer() {
    }

    public static List<String> toLines(XmlDocument document) {
        List<String> lines = new ArrayList<>();
        if (document.hasLogHeader()) {
            lines.add("# log");
        }
        lines.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        String content = document.root().toXmlString("", "    ");
        for (String line : content.split("\n", -1)) {
            lines.add(line);
        }
        return lines;
    }

    public static String toTree(XmlDocument document) {
        XmlElement root = document.root();
        StringBuilder sb = new StringBuilder();
        sb.append(root.tagName()).append(" [").append(renderAttributes(root)).append("]");
        List<XmlNode> children = root.children();
        boolean singleChild = children.size() == 1;
        for (int i = 0; i < children.size(); i++) {
            sb.append('\n');
            boolean isLast = i == children.size() - 1;
            if (singleChild) {
                isLast = false;
            }
            sb.append(children.get(i).toTreeString("", isLast));
        }
        return sb.toString();
    }

    private static String renderAttributes(XmlElement element) {
        return element.attributes().entrySet().stream()
                .map(entry -> entry.getKey() + "=\"" + entry.getValue() + "\"")
                .collect(java.util.stream.Collectors.joining(", "));
    }
}
