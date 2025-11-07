package org.gold.cache;

import io.netty.channel.ChannelHandlerContext;
import org.gold.config.GlobalProperties;
import org.gold.core.*;
import org.gold.model.ConsumerQueueOffsetModel;
import org.gold.model.GoldMqTopicModel;
import org.gold.netty.nameserver.HeartBeatTaskManager;
import org.gold.netty.nameserver.NameServerClient;
import org.gold.slave.SlaveSyncService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zhaoxun
 * @date 2025/10/20
 * @description 统一缓存对象
 */
public class CommonCache {
    private static GlobalProperties globalProperties = new GlobalProperties();
    private static List<GoldMqTopicModel> goldMqTopicModelList = new ArrayList<>();
    private static ConsumerQueueOffsetModel consumerQueueOffsetModel = new ConsumerQueueOffsetModel();
    private static ConsumerQueueMMapFileModelManager consumerQueueMMapFileModelManager = new ConsumerQueueMMapFileModelManager();
    private static CommitLogMMapFileModelManager commitLogMMapFileModelManager = new CommitLogMMapFileModelManager();

    private static CommitLogAppendHandler commitLogAppendHandler;
    private static ConsumerQueueAppendHandler consumerQueueAppendHandler;
    private static ConsumerQueueConsumeHandler consumerQueueConsumeHandler;

    private static NameServerClient nameServerClient = new NameServerClient();
    private static HeartBeatTaskManager heartBeatTaskManager = new HeartBeatTaskManager();
    private static SlaveSyncService slaveSyncService;

    private static Map<String, ChannelHandlerContext> slaveChannelMap = new HashMap<>();

    public static GlobalProperties getGlobalProperties() {
        return globalProperties;
    }

    public static void setGlobalProperties(GlobalProperties globalProperties) {
        CommonCache.globalProperties = globalProperties;
    }

    public static List<GoldMqTopicModel> getGoldMqTopicModelList() {
        return goldMqTopicModelList;
    }

    public static Map<String, GoldMqTopicModel> getGoldMqTopicModelMap() {
        return goldMqTopicModelList.stream().collect(Collectors.toMap(GoldMqTopicModel::getTopic, Function.identity()));
    }

    public static void setGoldMqTopicModelList(List<GoldMqTopicModel> goldMqTopicModelList) {
        CommonCache.goldMqTopicModelList = goldMqTopicModelList;
    }

    public static ConsumerQueueOffsetModel getConsumerQueueOffsetModel() {
        return consumerQueueOffsetModel;
    }

    public static void setConsumerQueueOffsetModel(ConsumerQueueOffsetModel consumerQueueOffsetModel) {
        CommonCache.consumerQueueOffsetModel = consumerQueueOffsetModel;
    }

    public static ConsumerQueueMMapFileModelManager getConsumerQueueMMapFileModelManager() {
        return consumerQueueMMapFileModelManager;
    }

    public static void setConsumerQueueMMapFileModelManager(ConsumerQueueMMapFileModelManager consumerQueueMMapFileModelManager) {
        CommonCache.consumerQueueMMapFileModelManager = consumerQueueMMapFileModelManager;
    }

    public static CommitLogMMapFileModelManager getCommitLogMMapFileModelManager() {
        return commitLogMMapFileModelManager;
    }

    public static void setCommitLogMMapFileModelManager(CommitLogMMapFileModelManager commitLogMMapFileModelManager) {
        CommonCache.commitLogMMapFileModelManager = commitLogMMapFileModelManager;
    }

    public static CommitLogAppendHandler getCommitLogAppendHandler() {
        return commitLogAppendHandler;
    }

    public static void setCommitLogAppendHandler(CommitLogAppendHandler commitLogAppendHandler) {
        CommonCache.commitLogAppendHandler = commitLogAppendHandler;
    }

    public static ConsumerQueueAppendHandler getConsumerQueueAppendHandler() {
        return consumerQueueAppendHandler;
    }

    public static void setConsumerQueueAppendHandler(ConsumerQueueAppendHandler consumerQueueAppendHandler) {
        CommonCache.consumerQueueAppendHandler = consumerQueueAppendHandler;
    }

    public static ConsumerQueueConsumeHandler getConsumerQueueConsumeHandler() {
        return consumerQueueConsumeHandler;
    }

    public static void setConsumerQueueConsumeHandler(ConsumerQueueConsumeHandler consumerQueueConsumeHandler) {
        CommonCache.consumerQueueConsumeHandler = consumerQueueConsumeHandler;
    }

    public static NameServerClient getNameServerClient() {
        return nameServerClient;
    }

    public static void setNameServerClient(NameServerClient nameServerClient) {
        CommonCache.nameServerClient = nameServerClient;
    }

    public static HeartBeatTaskManager getHeartBeatTaskManager() {
        return heartBeatTaskManager;
    }

    public static void setHeartBeatTaskManager(HeartBeatTaskManager heartBeatTaskManager) {
        CommonCache.heartBeatTaskManager = heartBeatTaskManager;
    }

    public static SlaveSyncService getSlaveSyncService() {
        return slaveSyncService;
    }

    public static void setSlaveSyncService(SlaveSyncService slaveSyncService) {
        CommonCache.slaveSyncService = slaveSyncService;
    }

    public static Map<String, ChannelHandlerContext> getSlaveChannelMap() {
        return slaveChannelMap;
    }

    public static void setSlaveChannelMap(Map<String, ChannelHandlerContext> slaveChannelMap) {
        CommonCache.slaveChannelMap = slaveChannelMap;
    }
}
