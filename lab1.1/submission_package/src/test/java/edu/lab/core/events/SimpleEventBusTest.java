package edu.lab.core.events;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SimpleEventBus} 的单元测试。
 * <p>
 * 覆盖：发布事件会投递给订阅者；取消订阅后不再投递。
 */
class SimpleEventBusTest {
    @Test
    void publish_delivers_event_to_subscribers() {
        EventBus bus = new SimpleEventBus();
        AtomicInteger hits = new AtomicInteger();

        // 订阅 CommandExecutedEvent，并验证事件内容
        bus.subscribe(CommandExecutedEvent.class, ev -> {
            assertEquals(Path.of("a.txt"), ev.editorFile());
            assertEquals("append \"x\"", ev.rawCommandLine());
            hits.incrementAndGet();
        });

        // 发布事件后应命中一次
        bus.publish(new CommandExecutedEvent(Path.of("a.txt"), "append \"x\""));
        assertEquals(1, hits.get());
    }

    @Test
    void unsubscribe_stops_delivery() {
        EventBus bus = new SimpleEventBus();
        AtomicInteger hits = new AtomicInteger();

        // 订阅后立刻取消订阅
        EventBus.Subscription sub = bus.subscribe(CommandExecutedEvent.class, ignored -> hits.incrementAndGet());
        sub.unsubscribe();

        // 发布事件不应再命中
        bus.publish(new CommandExecutedEvent(Path.of("a.txt"), "exit"));
        assertEquals(0, hits.get());
    }
}
