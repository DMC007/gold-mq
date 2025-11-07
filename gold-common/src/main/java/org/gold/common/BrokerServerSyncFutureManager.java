package org.gold.common;

import org.gold.remote.BrokerServerSyncFuture;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhaoxun
 * @date 2025/11/7
 */
public class BrokerServerSyncFutureManager {
    private static Map<String, BrokerServerSyncFuture> syncFutureMap = new ConcurrentHashMap<>();

    public static void putSyncFuture(String key, BrokerServerSyncFuture syncFuture) {
        syncFutureMap.put(key, syncFuture);
    }

    public static BrokerServerSyncFuture getSyncFuture(String key) {
        return syncFutureMap.get(key);
    }

    public static void removeSyncFuture(String key) {
        syncFutureMap.remove(key);
    }
}
