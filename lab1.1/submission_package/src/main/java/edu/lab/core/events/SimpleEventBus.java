package edu.lab.core.events;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 线程安全的简单事件总线实现。
 * <p>
 * 使用 {@link ConcurrentHashMap} + {@link CopyOnWriteArrayList}，在读多写少的订阅场景下实现简单并发安全。
 */
public final class SimpleEventBus implements EventBus {
    private final Map<Class<?>, CopyOnWriteArrayList<Consumer<?>>> handlers = new ConcurrentHashMap<>();

    @Override
    public <T> Subscription subscribe(Class<T> eventType, Consumer<T> handler) {
        // 为事件类型创建 handler 列表，并追加订阅者
        handlers.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>()).add(handler);
        return () -> {
            var list = handlers.get(eventType);
            if (list != null) {
                // 取消订阅：从列表移除 handler
                list.remove(handler);
            }
        };
    }

    @Override
    public void publish(Object event) {
        if (event == null) {
            // 忽略空事件
            return;
        }
        var list = handlers.get(event.getClass());
        if (list == null) {
            // 无订阅者则直接返回
            return;
        }
        for (var rawHandler : list) {
            @SuppressWarnings("unchecked")
            Consumer<Object> h = (Consumer<Object>) rawHandler;
            h.accept(event);
        }
    }
}
