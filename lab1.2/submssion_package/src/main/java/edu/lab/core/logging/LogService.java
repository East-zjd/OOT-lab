package edu.lab.core.logging;

import java.nio.file.Path;

/**
 * 命令日志服务。
 * <p>
 * 用于记录某个文件编辑会话中的命令执行历史，并支持显示日志内容。
 */
public interface LogService {
    /**
     * 为指定文件启用日志。
     */
    void enable(Path file);

    /**
     * 为指定文件禁用日志。
     */
    void disable(Path file);

    /**
     * @return 指定文件是否启用日志
     */
    boolean isEnabled(Path file);

    /**
     * 显示日志内容。
     *
     * @return 日志文件内容；若不存在则返回提示信息
     */
    String show(Path file);

    /**
     * 写入一条命令日志。
     * <p>
     * 最佳努力（best-effort）：必须保证不抛出异常，以免影响主流程。
     */
    void logCommand(Path file, String rawCommandLine);
}
