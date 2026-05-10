package edu.lab.core.events;

import java.util.function.Consumer;

/**
 * 事件总线。
 * <p>
 * 支持按事件类型订阅与发布，用于模块之间解耦通信。
 */
public interface EventBus {
    /**
     * 订阅指定类型事件。
     *
     * @param eventType 事件类型（按 class 精确匹配）
     * @param handler   处理函数
     * @return 取消订阅句柄
     */
    <T> Subscription subscribe(Class<T> eventType, Consumer<T> handler);

    /**
     * 发布事件。
     */
    void publish(Object event);

    /**
     * 订阅句柄：用于取消订阅。
     */
    interface Subscription {
        void unsubscribe();
    }
}
