package org.gold.dto;

import java.util.List;

/**
 * @author zhaoxun
 * @date 2025/11/18
 */
public class ConsumerMsgBaseRespDTO extends BaseBrokerRemoteDTO {
    private List<ConsumerMsgRespDTO> consumerMsgRespDTOList;

    public List<ConsumerMsgRespDTO> getConsumerMsgRespDTOList() {
        return consumerMsgRespDTOList;
    }

    public void setConsumerMsgRespDTOList(List<ConsumerMsgRespDTO> consumerMsgRespDTOList) {
        this.consumerMsgRespDTOList = consumerMsgRespDTOList;
    }
}
