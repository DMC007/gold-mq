package org.gold.event;

import org.gold.event.model.Event;

/**
 * @author zhaoxun
 * @date 2025/11/4
 */
public interface Listener<E extends Event> {

    /**
     * 用作回调通知处理
     *
     * @param event 事件
     */
    void onReceive(E event);
}
