package org.gold.rebalance;

import com.alibaba.fastjson2.JSON;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.model.GoldMqTopicModel;
import org.gold.rebalance.strategy.IReBalanceStrategy;
import org.gold.rebalance.strategy.ReBalanceInfo;
import org.gold.rebalance.strategy.impl.RandomReBalanceStrategyImpl;
import org.gold.utils.AssertUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author zhaoxun
 * @date 2025/11/19
 * @description 消费者实例池
 */
public class ConsumerInstancePool {
    private static final Logger log = LogManager.getLogger(ConsumerInstancePool.class);

    /**
     * key：topic; value：消费者实例列表
     */
    private Map<String, List<ConsumerInstance>> consumerInstanceMap = new ConcurrentHashMap<>();

    private static Map<String, IReBalanceStrategy> reBalanceStrategyMap = new HashMap<>();
    private ReBalanceInfo reBalanceInfo = new ReBalanceInfo();

    static {
        reBalanceStrategyMap.put("random", new RandomReBalanceStrategyImpl());
        reBalanceStrategyMap.put("range", new RandomReBalanceStrategyImpl());
    }


    public void addConsumerInstance(ConsumerInstance consumerInstance) {
        synchronized (this) {
            String topic = consumerInstance.getTopic();
            GoldMqTopicModel goldMqTopicModel = CommonCache.getGoldMqTopicModelMap().get(topic);
            AssertUtils.isNotNull(goldMqTopicModel, "topic not exist");
            List<ConsumerInstance> consumerInstanceList = consumerInstanceMap.getOrDefault(topic, new ArrayList<>());
            for (ConsumerInstance instance : consumerInstanceList) {
                if (instance.getConsumerReqId().equals(consumerInstance.getConsumerReqId())) {
                    return;
                }
            }
            consumerInstanceList.add(consumerInstance);
            consumerInstanceMap.put(topic, consumerInstanceList);
            //分配策略
            Set<String> consumerGroupSet = reBalanceInfo.getChangeConsumerGroupMap().get(topic);
            if (CollectionUtils.isEmpty(consumerGroupSet)) {
                consumerGroupSet = new HashSet<>();
            }
            consumerGroupSet.add(consumerInstance.getConsumeGroup());
            reBalanceInfo.getChangeConsumerGroupMap().put(topic, consumerGroupSet);
            log.info("add consumer instance in pool：{}", JSON.toJSONString(consumerInstance));
        }
    }

    public void removeFromInstancePool(String reqId) {
        synchronized (this) {
            for (String topic : consumerInstanceMap.keySet()) {
                List<ConsumerInstance> consumerInstances = consumerInstanceMap.get(topic);
                //过滤出ReqId不相等的保留
                List<ConsumerInstance> filterInstances = consumerInstances.stream().filter(instance -> !instance.getConsumerReqId().equals(reqId)).toList();
                consumerInstanceMap.put(topic, filterInstances);
            }
            Map<String, Map<String, List<ConsumerInstance>>> consumerHoldMap = CommonCache.getConsumerHoldMap();
            for (String topic : consumerHoldMap.keySet()) {
                Map<String, List<ConsumerInstance>> consumetGroupInstanceMap = consumerHoldMap.get(topic);
                for (String consumerGroup : consumetGroupInstanceMap.keySet()) {
                    List<ConsumerInstance> consumerInstances = consumetGroupInstanceMap.get(consumerGroup);
                    List<ConsumerInstance> filterInstances = consumerInstances.stream().filter(instance -> !instance.getConsumerReqId().equals(reqId)).toList();
                    consumetGroupInstanceMap.put(consumerGroup, filterInstances);
                }
            }
        }
    }

    public void startReBalanceJob() {
        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(10);
                doReBalance();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "reBalance-task").start();
    }

    /**
     * 执行重平衡逻辑
     * 定时任务触发，把已有的队列分配给消费者
     */
    private void doReBalance() {
        synchronized (this) {
            String reBalanceStrategy = CommonCache.getGlobalProperties().getReBalanceStrategy();
            //触发重平衡行为，根据参数决定重平衡策略的不同
            reBalanceInfo.setConsumeInstanceMap(this.consumerInstanceMap);
            reBalanceStrategyMap.get(reBalanceStrategy).doReBalance(reBalanceInfo);
            reBalanceInfo.getChangeConsumerGroupMap().clear();
        }
    }
}
