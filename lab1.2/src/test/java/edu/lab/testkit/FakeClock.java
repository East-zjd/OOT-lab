package edu.lab.testkit;

import edu.lab.core.time.Clock;

import java.time.LocalDateTime;

/**
 * 测试用假时钟。
 * <p>
 * 允许在测试中固定/推进时间，以便断言日志时间戳等行为。
 */
public final class FakeClock implements Clock {
    private LocalDateTime now;

    public FakeClock(LocalDateTime now) {
        this.now = now;
    }

    /**
     * 设置当前时间。
     */
    public void setNow(LocalDateTime now) {
        this.now = now;
    }

    @Override
    public LocalDateTime now() {
        // 返回可控的当前时间
        return now;
    }
}
