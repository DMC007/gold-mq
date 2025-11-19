package org.gold.event.spi.listener;

import org.gold.dto.ConsumerMsgAckReqDTO;
import org.gold.dto.ConsumerMsgAckRespDTO;
import org.gold.event.Listener;
import org.gold.event.model.ConsumerMsgAckEvent;

/**
 * @author zhaoxun
 * @date 2025/11/19
 */
public class ConsumerMsgAckListener implements Listener<ConsumerMsgAckEvent> {
    @Override
    public void onReceive(ConsumerMsgAckEvent event) throws Exception {
        ConsumerMsgAckReqDTO consumerMsgAckReqDTO = event.getConsumerMsgAckReqDTO();
        String topic = consumerMsgAckReqDTO.getTopic();
        String consumeGroup = consumerMsgAckReqDTO.getConsumeGroup();
        Integer queueId = consumerMsgAckReqDTO.getQueueId();
        Integer ackCount = consumerMsgAckReqDTO.getAckCount();
        //构建响应
        ConsumerMsgAckRespDTO consumerMsgAckRespDTO = new ConsumerMsgAckRespDTO();
        consumerMsgAckRespDTO.setMsgId(event.getMsgId());
    }
}
