package edu.lab.core.time;

import java.time.LocalDateTime;

/**
 * 系统时钟实现。
 */
public final class SystemClock implements Clock {
    @Override
    public LocalDateTime now() {
        // 直接返回系统当前时间
        return LocalDateTime.now();
    }
}
