package edu.lab.core.events;

import java.nio.file.Path;

/**
 * 命令执行完成事件。
 * <p>
 * 在命令成功执行并确定目标编辑器文件后发布，用于日志记录等扩展点。
 *
 * @param editorFile      命令关联的编辑器文件
 * @param rawCommandLine  用户输入的原始命令行
 */
public record CommandExecutedEvent(Path editorFile, String rawCommandLine) {
}
