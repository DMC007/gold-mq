package org.gold.event.model;

import org.gold.dto.ConsumerMsgAckReqDTO;

/**
 * @author zhaoxun
 * @date 2025/11/19
 */
public class ConsumerMsgAckEvent extends Event {

    private ConsumerMsgAckReqDTO consumerMsgAckReqDTO;

    public ConsumerMsgAckReqDTO getConsumerMsgAckReqDTO() {
        return consumerMsgAckReqDTO;
    }

    public void setConsumerMsgAckReqDTO(ConsumerMsgAckReqDTO consumerMsgAckReqDTO) {
        this.consumerMsgAckReqDTO = consumerMsgAckReqDTO;
    }
}
