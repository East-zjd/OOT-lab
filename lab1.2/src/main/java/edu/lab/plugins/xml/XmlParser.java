package edu.lab.plugins.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 简化 XML 解析器。
 */
public final class XmlParser {
    private XmlParser() {
    }

    public static XmlDocument parse(List<String> lines) {
        boolean hasLogHeader = !lines.isEmpty() && "# log".equals(lines.get(0).trim());
        List<String> contentLines = hasLogHeader ? lines.subList(1, lines.size()) : lines;
        String content = String.join("\n", contentLines).trim();
        if (content.isEmpty()) {
            XmlElement root = new XmlElement("root", Map.of("id", "root"));
            return new XmlDocument(root, hasLogHeader);
        }
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(content)));
            Element rootEl = doc.getDocumentElement();
                XmlElement root = toElement(rootEl, null, new HashSet<>());
            return new XmlDocument(root, hasLogHeader);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid xml: " + e.getMessage());
        }
    }

    private static XmlElement toElement(Element element, XmlElement parent, Set<String> ids) {
        Map<String, String> attrs = new LinkedHashMap<>();
        var named = element.getAttributes();
        for (int i = 0; i < named.getLength(); i++) {
            Node node = named.item(i);
            attrs.put(node.getNodeName(), node.getNodeValue());
        }
        String id = attrs.get("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("element missing id: " + element.getTagName());
        }
        if (!ids.add(id)) {
            throw new IllegalArgumentException("duplicate id: " + id);
        }
        XmlElement current = new XmlElement(element.getTagName(), attrs);
        current.setParent(parent);

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                XmlElement childElement = toElement((Element) child, current, ids);
                current.addChild(childElement);
            } else if (child.getNodeType() == Node.TEXT_NODE) {
                String text = child.getTextContent();
                if (text != null && !text.trim().isEmpty()) {
                    current.addChild(new XmlText(text.trim()));
                }
            }
        }
        return current;
    }
}
