package org.gold.event.model;

import org.gold.dto.ConsumerMsgRetryReqDTO;

/**
 * @author zhaoxun
 * @date 2025/11/20
 */
public class ConsumerMsgRetryEvent extends Event {
    private ConsumerMsgRetryReqDTO consumerMsgRetryReqDTO;

    public ConsumerMsgRetryReqDTO getConsumerMsgRetryReqDTO() {
        return consumerMsgRetryReqDTO;
    }

    public void setConsumerMsgRetryReqDTO(ConsumerMsgRetryReqDTO consumerMsgRetryReqDTO) {
        this.consumerMsgRetryReqDTO = consumerMsgRetryReqDTO;
    }
}
