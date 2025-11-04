package org.gold.coder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.gold.constants.TcpConstants;

/**
 * @author zhaoxun
 * @date 2025/11/4
 */
public class TcpMsgEncoder extends MessageToByteEncoder<TcpMsg> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, TcpMsg tcpMsg, ByteBuf byteBuf) throws Exception {
        byteBuf.writeShort(tcpMsg.getMagic());
        byteBuf.writeInt(tcpMsg.getCode());
        byteBuf.writeInt(tcpMsg.getLen());
        byteBuf.writeBytes(tcpMsg.getBody());
        //因为消息存在粘包拆包问题，有不同的解决方式，这里采用自定义分隔符来解决
        byteBuf.writeBytes(TcpConstants.DEFAULT_DECODE_CHAR.getBytes());
    }
}
