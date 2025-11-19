package org.gold.rebalance;

import com.alibaba.fastjson2.JSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.model.GoldMqTopicModel;
import org.gold.utils.AssertUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
            //TODO 分配策略
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
}
