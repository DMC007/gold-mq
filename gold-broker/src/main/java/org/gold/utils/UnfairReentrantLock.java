package org.gold.utils;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @author zhaoxun
 * @date 2025/10/21
 */
public class UnfairReentrantLock implements PutMessageLock {

    private ReentrantLock lock = new ReentrantLock();

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        lock.unlock();
    }
}
