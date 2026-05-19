package edu.lab.core.events;

import java.nio.file.Path;

/**
 * 编辑器被激活事件。
 */
public record EditorActivatedEvent(Path file) {
}
