package org.gold.dto;

import java.util.List;

/**
 * @author zhaoxun
 * @date 2025/11/19
 */
public class ConsumerMsgRetryReqDTO extends BaseBrokerRemoteDTO{
    private List<ConsumerMsgRetryReqDetailDTO> consumerMsgRetryReqDetailDTOList;

    public List<ConsumerMsgRetryReqDetailDTO> getConsumerMsgRetryReqDetailDTOList() {
        return consumerMsgRetryReqDetailDTOList;
    }

    public void setConsumerMsgRetryReqDetailDTOList(List<ConsumerMsgRetryReqDetailDTO> consumerMsgRetryReqDetailDTOList) {
        this.consumerMsgRetryReqDetailDTOList = consumerMsgRetryReqDetailDTOList;
    }
}
