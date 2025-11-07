package org.gold.producer;

import org.gold.dto.MessageDTO;

/**
 * @author zhaoxun
 * @date 2025/11/7
 */
public interface Producer {

    /**
     * 同步发送消息
     *
     * @param message 待发送的消息
     * @return 发送结果
     */
    SendResult send(MessageDTO message);

    /**
     * 异步发送消息
     *
     * @param message 待发送的消息
     */
    void sendAsync(MessageDTO message);

    /**
     * 发送事务消息
     *
     * @param message 待发送的消息
     * @return 发送结果
     */
    SendResult sendTxMessage(MessageDTO message);
}
