package org.gold;

import org.gold.cache.CommonCache;
import org.gold.config.ConsumerQueueOffsetLoader;
import org.gold.config.GlobalPropertiesLoader;
import org.gold.config.GoldMqTopicLoader;
import org.gold.core.CommitLogAppendHandler;
import org.gold.core.ConsumerQueueAppendHandler;
import org.gold.core.ConsumerQueueConsumeHandler;
import org.gold.enums.BrokerClusterModeEnum;
import org.gold.model.GoldMqTopicModel;
import org.gold.slave.SlaveSyncService;

import java.io.IOException;

/**
 * @author zhaoxun
 * @date 2025/10/21
 */
public class BrokerStartUp {

    private static GlobalPropertiesLoader globalPropertiesLoader;
    private static GoldMqTopicLoader goldMqTopicLoader;
    private static ConsumerQueueOffsetLoader consumerQueueOffsetLoader;

    private static CommitLogAppendHandler commitLogAppendHandler;
    private static ConsumerQueueAppendHandler consumerQueueAppendHandler;
    private static ConsumerQueueConsumeHandler consumerQueueConsumeHandler;
    private static SlaveSyncService slaveSyncService;

    /**
     * 启动
     *
     * @param args 参数
     */
    public static void main(String[] args) throws IOException {
        initProperties();
        initNameServerClient();
    }

    /**
     * 初始化配置文件
     * 加载配置, 缓存对象的生成
     */
    private static void initProperties() throws IOException {
        globalPropertiesLoader = new GlobalPropertiesLoader();
        goldMqTopicLoader = new GoldMqTopicLoader();
        consumerQueueOffsetLoader = new ConsumerQueueOffsetLoader();

        commitLogAppendHandler = new CommitLogAppendHandler();
        consumerQueueAppendHandler = new ConsumerQueueAppendHandler();
        consumerQueueConsumeHandler = new ConsumerQueueConsumeHandler();

        globalPropertiesLoader.loadProperties();
        goldMqTopicLoader.loadProperties();
        goldMqTopicLoader.startRefreshGoldMqTopicInfoTask();

        consumerQueueOffsetLoader.loadProperties();
        consumerQueueOffsetLoader.startRefreshConsumerQueueOffsetTask();

        for (GoldMqTopicModel goldMqTopicModel : CommonCache.getGoldMqTopicModelMap().values()) {
            String topicName = goldMqTopicModel.getTopic();
            commitLogAppendHandler.prepareMMapLoading(topicName);
            consumerQueueAppendHandler.prepareConsumerQueue(topicName);
        }
        //TODO 时间轮相关的后期开发
    }

    /**
     * 初始化名称服务客户端
     */
    private static void initNameServerClient() {
        CommonCache.getNameServerClient().initConnection();
        CommonCache.getNameServerClient().sendRegistryMsg();
        //目前集群模式通过master-slave实现，如果当前节点是slave，那么需要与master节点建立连接
        if (!BrokerClusterModeEnum.MASTER_SLAVE.getCode().equals(CommonCache.getGlobalProperties().getBrokerClusterMode())
                || "master".equals(CommonCache.getGlobalProperties().getBrokerClusterRole())) {
            return;
        }
        String masterAddress = CommonCache.getNameServerClient().queryBrokerMasterAddress();
        if (masterAddress != null) {
            slaveSyncService = new SlaveSyncService();
            CommonCache.setSlaveSyncService(slaveSyncService);
            // 尝试与master建立连接
            boolean connectResult = slaveSyncService.connectMasterBroker(masterAddress);
            if (connectResult) {
                // 连接建立成功, 发送同步开始消息
                slaveSyncService.sendStartSyncMsg();
            }
        }
    }

}
