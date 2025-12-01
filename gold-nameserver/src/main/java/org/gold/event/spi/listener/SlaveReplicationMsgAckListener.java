package org.gold.event.spi.listener;

import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.dto.SlaveAckDTO;
import org.gold.enums.NameServerResponseCode;
import org.gold.event.Listener;
import org.gold.event.model.SlaveReplicationMsgAckEvent;

/**
 * @author zhaoxun
 * @date 2025/12/1
 */
public class SlaveReplicationMsgAckListener implements Listener<SlaveReplicationMsgAckEvent> {
    @Override
    public void onReceive(SlaveReplicationMsgAckEvent event) throws Exception {
        String slaveAckMsgId = event.getMsgId();
        SlaveAckDTO slaveAckDTO = CommonCache.getAckMap().get(slaveAckMsgId);
        if (slaveAckDTO == null) {
            return;
        }
        int currentAckTime = slaveAckDTO.getNeedAckTime().decrementAndGet();
        //如果是复制模式，代码所有从节点已经回复主节点ack完毕
        //如果是半同步复制模式，代码一半的从节点已经回复了主节点ack，不必再等待剩下的从节点回复，直接响应注册客户端注册成功的消息
        //[剩余的从节点后续回复过来到这里也不会处理，因为SlaveAckDTO对象已经被删除，就会按照上面的代码直接return]
        if (currentAckTime == 0) {
            CommonCache.getAckMap().remove(slaveAckMsgId);
            TcpMsg tcpMsg = new TcpMsg(NameServerResponseCode.REGISTRY_SUCCESS.getCode(), NameServerResponseCode.REGISTRY_SUCCESS.getDesc().getBytes());
            slaveAckDTO.getBrokerChannel().writeAndFlush(tcpMsg);
        }
    }
}
