package edu.lab.plugins.xml;

import java.util.HashMap;
import java.util.Map;

/**
 * XML 文档对象。
 */
public final class XmlDocument {
    private final XmlElement root;
    private final Map<String, XmlElement> idIndex = new HashMap<>();
    private final boolean logHeader;

    public XmlDocument(XmlElement root, boolean logHeader) {
        this.root = root;
        this.logHeader = logHeader;
        indexElement(root);
    }

    public XmlElement root() {
        return root;
    }

    public boolean hasLogHeader() {
        return logHeader;
    }

    public XmlElement findById(String id) {
        return idIndex.get(id);
    }

    public boolean idExists(String id) {
        return idIndex.containsKey(id);
    }

    public void registerElement(XmlElement element) {
        indexElement(element);
    }

    public void unregisterElement(XmlElement element) {
        removeFromIndex(element);
    }

    private void indexElement(XmlElement element) {
        idIndex.put(element.id(), element);
        for (XmlNode node : element.children()) {
            if (node instanceof XmlElement child) {
                indexElement(child);
            }
        }
    }

    private void removeFromIndex(XmlElement element) {
        idIndex.remove(element.id());
        for (XmlNode node : element.children()) {
            if (node instanceof XmlElement child) {
                removeFromIndex(child);
            }
        }
    }
}
