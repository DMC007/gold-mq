package org.gold.event;

import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.event.model.Event;
import org.gold.utils.ReflectUtils;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zhaoxun
 * @date 2025/11/4
 * @description 事件总线
 */
public class EventBus {

    private static final Logger log = LogManager.getLogger(EventBus.class);

    private Map<Class<? extends Event>, List<Listener<Event>>> eventListenerMap = new ConcurrentHashMap<>();

    private String taskName = "event-bus-task-";

    public EventBus(String taskName) {
        this.taskName = taskName;
    }

    public EventBus(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            10,
            100,
            3,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1000),
            r -> {
                Thread thread = new Thread(r);
                thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        log.error("uncaughtException:{}", e.getMessage(), e);
                    }
                });
                thread.setName(taskName + UUID.randomUUID());
                return thread;
            });

    public void init() {
        //利用spi机制获取所有监听器[对其启动就像加载进来的类，可以利用spi机制实现]
        ServiceLoader<Listener> serviceLoader = ServiceLoader.load(Listener.class);
        for (Listener listener : serviceLoader) {
            Class clazz = ReflectUtils.getInterfaceT(listener, 0);
            this.register(clazz, listener);
        }
    }

    public <E extends Event> void register(Class<? extends Event> clazz, Listener<Event> listener) {
        List<Listener<Event>> listenerList = eventListenerMap.get(clazz);
        if (CollectionUtils.isEmpty(listenerList)) {
            eventListenerMap.put(clazz, Lists.newArrayList(listener));
        } else {
            listenerList.add(listener);
            eventListenerMap.put(clazz, listenerList);
        }
    }

    public void publish(Event event) {
        threadPoolExecutor.execute(() -> {
            List<Listener<Event>> listenerList = eventListenerMap.get(event.getClass());
            if (CollectionUtils.isEmpty(listenerList)) {
                log.warn("no listener for event:{}", event.getClass().getName());
                return;
            }
            try {
                for (Listener<Event> listener : listenerList) {
                    listener.onReceive(event);
                }
            } catch (Exception e) {
                log.error("publish event error:{}", e.getMessage(), e);
            }
        });
    }
}
