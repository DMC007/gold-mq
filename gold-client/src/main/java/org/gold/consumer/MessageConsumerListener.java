package org.gold.consumer;

import java.util.List;

/**
 * @author zhaoxun
 * @date 2025/11/12
 * @description 消费监听器
 */
public interface MessageConsumerListener {


    /**
     *
     * @param consumerMessageList 消息消费处理方法
     * @return 消费结果
     * @throws Exception 消费异常
     * @see ConsumerMessage
     */
    ConsumeResult consume(List<ConsumerMessage> consumerMessageList) throws Exception;
}
