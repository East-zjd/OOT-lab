package edu.lab.core.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SystemClock} 的单元测试。
 */
class SystemClockTest {
    @Test
    void now_returns_a_value() {
        Clock clock = new SystemClock();
        // now() 应返回非空时间对象
        assertDoesNotThrow(() -> {
            assertNotNull(clock.now());
        });
    }
}
