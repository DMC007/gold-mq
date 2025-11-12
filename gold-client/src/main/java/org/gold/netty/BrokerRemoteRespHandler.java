package org.gold.netty;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.gold.coder.TcpMsg;
import org.gold.common.BrokerServerSyncFutureManager;
import org.gold.dto.SendMessageToBrokerResponseDTO;
import org.gold.enums.BrokerResponseCode;
import org.gold.event.EventBus;
import org.gold.remote.BrokerServerSyncFuture;

/**
 * @author zhaoxun
 * @date 2025/11/10
 */
@ChannelHandler.Sharable
public class BrokerRemoteRespHandler extends SimpleChannelInboundHandler<TcpMsg> {

    private EventBus eventBus;

    public BrokerRemoteRespHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TcpMsg msg) throws Exception {
        int code = msg.getCode();
        byte[] body = msg.getBody();
        if (BrokerResponseCode.SEND_MSG_RESP.getCode() == code) {
            SendMessageToBrokerResponseDTO sendMessageToBrokerResponseDTO = JSON.parseObject(body, SendMessageToBrokerResponseDTO.class);
            BrokerServerSyncFuture syncFuture = BrokerServerSyncFutureManager.getSyncFuture(sendMessageToBrokerResponseDTO.getMsgId());
            if (syncFuture != null) {
                syncFuture.setResponse(sendMessageToBrokerResponseDTO);
            }
        }
    }
}
