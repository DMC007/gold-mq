package org.gold.coder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.gold.constants.BrokerConstants;

import java.util.List;

/**
 * @author zhaoxun
 * @date 2025/11/4
 */
public class TcpMsgDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() > 2 + 4 + 4) {
            //先比较魔数
            if (in.readShort() != BrokerConstants.DEFAULT_MAGIC_NUM) {
                ctx.close();
                return;
            }
            int code = in.readInt();
            int len = in.readInt();
            if (in.readableBytes() > len) {
                ctx.close();
                return;
            }
            byte[] body = new byte[len];
            in.readBytes(body);
            out.add(new TcpMsg(code, body));
        }
    }
}
