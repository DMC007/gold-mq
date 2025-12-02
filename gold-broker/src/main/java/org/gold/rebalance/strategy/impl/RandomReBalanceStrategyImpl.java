package org.gold.rebalance.strategy.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.model.GoldMqTopicModel;
import org.gold.rebalance.ConsumerInstance;
import org.gold.rebalance.strategy.IReBalanceStrategy;
import org.gold.rebalance.strategy.ReBalanceInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author zhaoxun
 * @date 2025/12/2
 * @description 随机重平衡
 */
public class RandomReBalanceStrategyImpl implements IReBalanceStrategy {

    private static final Logger log = LogManager.getLogger(RandomReBalanceStrategyImpl.class);

    @Override
    public void doReBalance(ReBalanceInfo reBalanceInfo) {
        //key：topic; value：消费者实例列表
        Map<String, List<ConsumerInstance>> consumeInstanceMap = reBalanceInfo.getConsumeInstanceMap();
        //key：topic; value：topic模型
        Map<String, GoldMqTopicModel> goldMqTopicModelMap = CommonCache.getGoldMqTopicModelMap();
        for (String topic : consumeInstanceMap.keySet()) {
            //拿到当前topic有哪些消费者实例
            List<ConsumerInstance> consumerInstances = consumeInstanceMap.get(topic);
            GoldMqTopicModel goldMqTopicModel = goldMqTopicModelMap.get(topic);
            if (goldMqTopicModel == null) {
                log.error("topic:{} topicModel is null", topic);
                continue;
            }
            if (goldMqTopicModel.getQueueList() == null) {
                log.error("topic:{} topicModel.queueList is null", topic);
                continue;
            }
            //队列数量
            int queueSize = goldMqTopicModel.getQueueList().size();
            //key：消费组名称; value：消费者实例列表
            Map<String, List<ConsumerInstance>> consumerGroupMap = consumerInstances.stream().collect(Collectors.groupingBy(ConsumerInstance::getConsumeGroup));
            Set<String> changeConsumerGroup = reBalanceInfo.getChangeConsumerGroupMap().get(topic);
            if (changeConsumerGroup == null || changeConsumerGroup.isEmpty()) {
                //目前没有新消费者加入，不需要触发重平衡
                return;
            }
            //key：消费组名称; value：消费者实例列表
            Map<String, List<ConsumerInstance>> consumerGroupHoldMap = new ConcurrentHashMap<>();
            for (String consumeGroup : consumerGroupMap.keySet()) {
                //变更的消费者名单中没有包含当前消费者，不触发重平衡
                if (!changeConsumerGroup.contains(consumeGroup)) {
                    //依旧保存之前的消费者信息
                    consumerGroupHoldMap.put(consumeGroup, consumerGroupMap.get(consumeGroup));
                    continue;
                }
                //当前消费者有变更
                List<ConsumerInstance> consumerGroupInstanceList = consumerGroupMap.get(consumeGroup);
                //先释放消费者之前所持有的队列，重新分配
                for (ConsumerInstance consumerInstance : consumerGroupInstanceList) {
                    consumerInstance.getQueueIdSet().clear();
                }
                List<ConsumerInstance> newConsumerQueueInstanceList = new ArrayList<>();
                int consumerNum = consumerGroupInstanceList.size();
                //队列数>消费者数，那么每个消费者都会持有队列
                Collections.shuffle(consumerGroupInstanceList);
                //参与重平衡的消费者，需要重新分配队列
                if (queueSize >= consumerNum) {
                    int j = 0;
                    for (int i = 0; i < consumerNum; i++, j++) {
                        ConsumerInstance consumerInstance = consumerGroupInstanceList.get(i);
                        consumerInstance.getQueueIdSet().add(j);
                        newConsumerQueueInstanceList.add(consumerInstance);
                    }
                    for (; j < queueSize; j++) {
                        Random random = new Random();
                        int randomConsumerId = random.nextInt(consumerNum);
                        ConsumerInstance consumerInstance = consumerGroupInstanceList.get(randomConsumerId);
                        consumerInstance.getQueueIdSet().add(j);
                    }
                } else {
                    for (int i = 0; i < queueSize; i++) {
                        ConsumerInstance consumerInstance = consumerGroupInstanceList.get(i);
                        consumerInstance.getQueueIdSet().add(i);
                    }
                }
                consumerGroupHoldMap.put(consumeGroup, newConsumerQueueInstanceList);
            }
            CommonCache.getConsumerHoldMap().put(topic, consumerGroupHoldMap);
        }
    }
}
