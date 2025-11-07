package org.gold.common;

import org.gold.remote.NameServerSyncFuture;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhaoxun
 * @date 2025/11/7
 */
public class NameServerSyncFutureManager {
    private static Map<String, NameServerSyncFuture> syncFutureMap = new ConcurrentHashMap<>();

    public static void putSyncFuture(String key, NameServerSyncFuture syncFuture) {
        syncFutureMap.put(key, syncFuture);
    }

    public static NameServerSyncFuture getSyncFuture(String key) {
        return syncFutureMap.get(key);
    }

    public static void removeSyncFuture(String key) {
        syncFutureMap.remove(key);
    }
}
