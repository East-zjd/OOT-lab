package edu.lab.core.events;

import java.nio.file.Path;

/**
 * 编辑器关闭事件。
 */
public record EditorClosedEvent(Path file) {
}
