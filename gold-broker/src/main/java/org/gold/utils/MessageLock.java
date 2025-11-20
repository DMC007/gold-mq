package org.gold.utils;

/**
 * @author zhaoxun
 * @date 2025/10/21
 */
public interface MessageLock {
    /**
     * 加锁
     */
    void lock();

    /**
     * 解锁
     */
    void unlock();
}
