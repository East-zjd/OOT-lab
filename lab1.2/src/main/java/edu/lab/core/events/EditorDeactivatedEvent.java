package edu.lab.core.events;

import java.nio.file.Path;

/**
 * 编辑器被取消激活事件。
 */
public record EditorDeactivatedEvent(Path file) {
}
