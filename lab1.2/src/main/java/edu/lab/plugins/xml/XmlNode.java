package edu.lab.plugins.xml;

/**
 * XML 组合节点。
 */
public interface XmlNode {
    String toTreeString(String prefix, boolean isLast);

    String toXmlString(String indent, String childIndent);
}
