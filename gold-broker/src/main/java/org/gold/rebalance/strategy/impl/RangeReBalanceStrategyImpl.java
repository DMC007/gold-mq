package org.gold.rebalance.strategy.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.model.GoldMqTopicModel;
import org.gold.rebalance.ConsumerInstance;
import org.gold.rebalance.strategy.IReBalanceStrategy;
import org.gold.rebalance.strategy.ReBalanceInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zhaoxun
 * @date 2025/12/2
 * @description 平均分配
 */
public class RangeReBalanceStrategyImpl implements IReBalanceStrategy {

    private static final Logger log = LogManager.getLogger(RangeReBalanceStrategyImpl.class);

    @Override
    public void doReBalance(ReBalanceInfo reBalanceInfo) {
        //key：topic; value：消费者实例列表
        Map<String, List<ConsumerInstance>> consumeInstanceMap = reBalanceInfo.getConsumeInstanceMap();
        //key：topic; value：topic模型
        Map<String, GoldMqTopicModel> goldMqTopicModelMap = CommonCache.getGoldMqTopicModelMap();
        for (String topic : consumeInstanceMap.keySet()) {
            Set<String> changeConsumerGroup = reBalanceInfo.getChangeConsumerGroupMap().get(topic);
            if (changeConsumerGroup == null || changeConsumerGroup.isEmpty()) {
                //目前没有新消费者加入，不需要触发重平衡
                return;
            }
            List<ConsumerInstance> consumerInstances = consumeInstanceMap.get(topic);
            if (CollectionUtils.isEmpty(consumerInstances)) {
                return;
            }
            //拿到topic模型
            GoldMqTopicModel goldMqTopicModel = goldMqTopicModelMap.get(topic);
            if (goldMqTopicModel == null) {
                log.error("topic:{} topicModel is null", topic);
                continue;
            }
            if (goldMqTopicModel.getQueueList() == null) {
                log.error("topic:{} topicModel.queueList is null", topic);
                continue;
            }
            int queueSize = goldMqTopicModel.getQueueList().size();
            //key：消费组名称; value：消费者实例列表
            Map<String, List<ConsumerInstance>> consumerGroupMap = consumerInstances.stream().collect(Collectors.groupingBy(ConsumerInstance::getConsumeGroup));
            for (String consumeGroup : consumerGroupMap.keySet()) {
                //每个消费组对应的当前实例集合
                List<ConsumerInstance> consumerInstanceList = consumerGroupMap.get(consumeGroup);
                //算出每个消费者平均拥有多少条队列
                int eachConsumerQueueNum = queueSize / consumerInstanceList.size();
                int queueId = 0;
                for (ConsumerInstance consumerInstance : consumerInstanceList) {
                    for (int queueNums = 0; queueNums < eachConsumerQueueNum; queueNums++) {
                        consumerInstance.getQueueIdSet().add(queueId++);
                    }
                }
                //剩余的队列没有被用到的数量
                int remainQueueNum = queueSize - queueId;
                if (remainQueueNum > 0) {
                    for (int i = 0; i < remainQueueNum; i++) {
                        consumerInstanceList.get(i).getQueueIdSet().add(queueId++);
                    }
                }
            }
        }
    }
}
