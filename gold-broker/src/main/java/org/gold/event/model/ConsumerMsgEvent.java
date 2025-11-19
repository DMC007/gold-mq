package org.gold.event.model;

import org.gold.dto.ConsumerMsgReqDTO;

/**
 * @author zhaoxun
 * @date 2025/11/19
 */
public class ConsumerMsgEvent extends Event {
    private ConsumerMsgReqDTO consumerMsgReqDTO;

    public ConsumerMsgReqDTO getConsumerMsgReqDTO() {
        return consumerMsgReqDTO;
    }

    public void setConsumerMsgReqDTO(ConsumerMsgReqDTO consumerMsgReqDTO) {
        this.consumerMsgReqDTO = consumerMsgReqDTO;
    }
}
