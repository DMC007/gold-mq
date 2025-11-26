package org.gold.common;

import org.gold.transaction.TransactionListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhaoxun
 * @date 2025/11/26
 */
public class CommonCache {
    /**
     * 事务监听器
     * key: producerId, value: transactionListener
     */
    private static Map<String, TransactionListener> transactionListenerMap = new ConcurrentHashMap<>();

    public static Map<String, TransactionListener> getTransactionListenerMap() {
        return transactionListenerMap;
    }

    public static void setTransactionListenerMap(Map<String, TransactionListener> transactionListenerMap) {
        CommonCache.transactionListenerMap = transactionListenerMap;
    }
}
