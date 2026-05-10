package edu.lab.core.time;

import java.time.LocalDateTime;

/**
 * 时钟抽象。
 * <p>
 * 便于在测试中注入固定时间或可控时间。
 */
public interface Clock {
    /**
     * @return 当前时间
     */
    LocalDateTime now();
}
