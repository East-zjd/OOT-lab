package edu.lab.plugins.xml;

/**
 * XML 文本节点。
 */
public final class XmlText implements XmlNode {
    private String text;

    public XmlText(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toTreeString(String prefix, boolean isLast) {
        String connector = isLast ? "└── " : "├── ";
        return prefix + connector + "\"" + text + "\"";
    }

    @Override
    public String toXmlString(String indent, String childIndent) {
        return indent + escape(text);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
