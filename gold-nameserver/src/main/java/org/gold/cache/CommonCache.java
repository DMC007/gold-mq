package org.gold.cache;

import io.netty.channel.Channel;
import org.gold.config.NameserverProperties;
import org.gold.config.PropertiesLoader;
import org.gold.dto.NodeAckDTO;
import org.gold.dto.SlaveAckDTO;
import org.gold.replication.ReplicationTask;
import org.gold.store.ReplicationChannelManager;
import org.gold.store.ReplicationMsgQueueManager;
import org.gold.store.ServiceInstanceManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhaoxun
 * @date 2025/11/4
 * @description 管理中心【可看作spring容器管理一样】
 */
public class CommonCache {

    private static PropertiesLoader propertiesLoader = new PropertiesLoader();
    private static NameserverProperties nameserverProperties = new NameserverProperties();
    private static ServiceInstanceManager serviceInstanceManager = new ServiceInstanceManager();
    private static Channel connectNodeChannel = null;
    private static ReplicationTask replicationTask;
    private static ReplicationMsgQueueManager replicationMsgQueueManager = new ReplicationMsgQueueManager();
    private static ReplicationChannelManager replicationChannelManager = new ReplicationChannelManager();

    private static Map<String, NodeAckDTO> nodeAckMap = new ConcurrentHashMap<>();
    private static Map<String, SlaveAckDTO> ackMap = new ConcurrentHashMap<>();

    public static NameserverProperties getNameserverProperties() {
        return nameserverProperties;
    }

    public static void setNameserverProperties(NameserverProperties nameserverProperties) {
        CommonCache.nameserverProperties = nameserverProperties;
    }

    public static PropertiesLoader getPropertiesLoader() {
        return propertiesLoader;
    }

    public static void setPropertiesLoader(PropertiesLoader propertiesLoader) {
        CommonCache.propertiesLoader = propertiesLoader;
    }

    public static ServiceInstanceManager getServiceInstanceManager() {
        return serviceInstanceManager;
    }

    public static void setServiceInstanceManager(ServiceInstanceManager serviceInstanceManager) {
        CommonCache.serviceInstanceManager = serviceInstanceManager;
    }

    public static Channel getConnectNodeChannel() {
        return connectNodeChannel;
    }

    public static void setConnectNodeChannel(Channel connectNodeChannel) {
        CommonCache.connectNodeChannel = connectNodeChannel;
    }

    public static ReplicationTask getReplicationTask() {
        return replicationTask;
    }

    public static void setReplicationTask(ReplicationTask replicationTask) {
        CommonCache.replicationTask = replicationTask;
    }

    public static ReplicationMsgQueueManager getReplicationMsgQueueManager() {
        return replicationMsgQueueManager;
    }

    public static void setReplicationMsgQueueManager(ReplicationMsgQueueManager replicationMsgQueueManager) {
        CommonCache.replicationMsgQueueManager = replicationMsgQueueManager;
    }

    public static ReplicationChannelManager getReplicationChannelManager() {
        return replicationChannelManager;
    }

    public static void setReplicationChannelManager(ReplicationChannelManager replicationChannelManager) {
        CommonCache.replicationChannelManager = replicationChannelManager;
    }

    public static Map<String, NodeAckDTO> getNodeAckMap() {
        return nodeAckMap;
    }

    public static void setNodeAckMap(Map<String, NodeAckDTO> nodeAckMap) {
        CommonCache.nodeAckMap = nodeAckMap;
    }

    public static Map<String, SlaveAckDTO> getAckMap() {
        return ackMap;
    }

    public static void setAckMap(Map<String, SlaveAckDTO> ackMap) {
        CommonCache.ackMap = ackMap;
    }
}
