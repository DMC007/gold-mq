package org.gold.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zhaoxun
 * @date 2025/10/21
 * @description 通用的线程池配置
 */
public class CommonThreadPoolConfig {
    /**
     * 刷新mq主题配置的线程池,专门用于将topic配置异步刷盘使用
     */
    public static ThreadPoolExecutor refreshGoldMqTopicExecutor = new ThreadPoolExecutor(1,
            1, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), r -> {
        Thread thread = new Thread(r);
        thread.setName("refresh-gold-mq-topic-config");
        return thread;
    });

    /**
     * 专门用于将各个消费者消费进度刷新到磁盘中
     */
    public static ThreadPoolExecutor refreshConsumeQueueOffsetExecutor = new ThreadPoolExecutor(1,
            1, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), r -> {
        Thread thread = new Thread(r);
        thread.setName("refresh-consumer-queue-offset");
        return thread;
    });
}
