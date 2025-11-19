package org.gold.event.spi.listener;

import com.alibaba.fastjson2.JSON;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.dto.ConsumerMsgBaseRespDTO;
import org.gold.dto.ConsumerMsgCommitLogDTO;
import org.gold.dto.ConsumerMsgReqDTO;
import org.gold.dto.ConsumerMsgRespDTO;
import org.gold.enums.BrokerResponseCode;
import org.gold.event.Listener;
import org.gold.event.model.ConsumerMsgEvent;
import org.gold.model.ConsumerQueueConsumeReqModel;
import org.gold.rebalance.ConsumerInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author zhaoxun
 * @date 2025/11/19
 * @description 消费者拉取消息，broker业务处理类
 */
public class ConsumerMsgListener implements Listener<ConsumerMsgEvent> {
    @Override
    public void onReceive(ConsumerMsgEvent event) throws Exception {
        ConsumerMsgReqDTO consumerMsgReqDTO = event.getConsumerMsgReqDTO();
        String currentReqId = consumerMsgReqDTO.getIp() + ":" + consumerMsgReqDTO.getPort();
        String topic = consumerMsgReqDTO.getTopic();
        //构建实例对象并赋值
        ConsumerInstance consumerInstance = new ConsumerInstance();
        consumerInstance.setIp(consumerMsgReqDTO.getIp());
        consumerInstance.setPort(consumerMsgReqDTO.getPort());
        consumerInstance.setConsumeGroup(consumerMsgReqDTO.getConsumeGroup());
        consumerInstance.setTopic(topic);
        consumerInstance.setBatchSize(consumerMsgReqDTO.getBatchSize());
        consumerInstance.setConsumerReqId(currentReqId);
        //放入消费者实力池
        CommonCache.getConsumerInstancePool().addConsumerInstance(consumerInstance);
        //定义消费数据拉取响应结果
        ConsumerMsgBaseRespDTO consumerMsgBaseRespDTO = new ConsumerMsgBaseRespDTO();
        List<ConsumerMsgRespDTO> consumerMsgRespDTOList = new ArrayList<>();
        consumerMsgBaseRespDTO.setMsgId(event.getMsgId());
        consumerMsgBaseRespDTO.setConsumerMsgRespDTOList(consumerMsgRespDTOList);
        //拿到key:消费者, value为消费组对应的实例集合
        Map<String, List<ConsumerInstance>> consumerGroupMap = CommonCache.getConsumerHoldMap().get(topic);
        //有可能当前消费组还没经过第一轮重平衡，因此不会那么快消费到数据，所以需要通知客户端，目前服务端还未将队列分配好
        if (consumerGroupMap == null) {
            //直接返回空数据
            TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.CONSUME_MSG_RESP.getCode(), JSON.toJSONBytes(consumerMsgBaseRespDTO));
            event.getChannelHandlerContext().writeAndFlush(tcpMsg);
            return;
        }
        List<ConsumerInstance> consumerInstances = consumerGroupMap.get(consumerMsgReqDTO.getConsumeGroup());
        if (consumerInstances == null) {
            //直接返回空数据
            TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.CONSUME_MSG_RESP.getCode(), JSON.toJSONBytes(consumerMsgBaseRespDTO));
            event.getChannelHandlerContext().writeAndFlush(tcpMsg);
            return;
        }
        //匹配到当前请求的实例
        for (ConsumerInstance instance : consumerInstances) {
           if(instance.getConsumerReqId().equals(currentReqId)) {
               for (Integer queueId : instance.getQueueIdSet()) {
                   ConsumerQueueConsumeReqModel consumerQueueConsumeReqModel = new ConsumerQueueConsumeReqModel();
                   consumerQueueConsumeReqModel.setTopic(topic);
                   consumerQueueConsumeReqModel.setConsumerGroup(instance.getConsumeGroup());
                   consumerQueueConsumeReqModel.setQueueId(queueId);
                   consumerQueueConsumeReqModel.setBatchSize(instance.getBatchSize());
                   //获取消费数据, 核心操作
                   List<ConsumerMsgCommitLogDTO> commitLogDTOList = CommonCache.getConsumerQueueConsumeHandler().consume(consumerQueueConsumeReqModel);
                   ConsumerMsgRespDTO consumerMsgRespDTO = new ConsumerMsgRespDTO();
                   consumerMsgRespDTO.setQueueId(queueId);
                   consumerMsgRespDTO.setCommitLogContentList(commitLogDTOList);
                   //放入集合
                   consumerMsgRespDTOList.add(consumerMsgRespDTO);
               }
           }
        }
        //返回数据
        TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.CONSUME_MSG_RESP.getCode(), JSON.toJSONBytes(consumerMsgBaseRespDTO));
        event.getChannelHandlerContext().writeAndFlush(tcpMsg);
    }
}
