package org.gold.remote;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.coder.TcpMsg;
import org.gold.coder.TcpMsgDecoder;
import org.gold.coder.TcpMsgEncoder;
import org.gold.constants.TcpConstants;

/**
 * @author zhaoxun
 * @date 2025/11/6
 * @description 对nameserver进行远程访问的一个客户端工具
 */
public class NameServerNettyRemoteClient {

    private static final Logger log = LogManager.getLogger(NameServerNettyRemoteClient.class);

    private String ip;
    private Integer port;

    public NameServerNettyRemoteClient(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
    }

    //TODO 后面会根据系统选择线程模型
    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
    private Bootstrap bootstrap = new Bootstrap();
    private Channel channel;

    /**
     * 初始化连接
     */
    public void buildConnection() {
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ByteBuf delimiterBuf = Unpooled.copiedBuffer(TcpConstants.DEFAULT_DECODE_CHAR.getBytes());
                        ch.pipeline().addLast(new DelimiterBasedFrameDecoder(8 * 1024, delimiterBuf));
                        ch.pipeline().addLast(new TcpMsgEncoder());
                        ch.pipeline().addLast(new TcpMsgDecoder());
                    }
                });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            eventLoopGroup.shutdownGracefully();
        }));
        ChannelFuture channelFuture = null;
        try {
            channelFuture = bootstrap.connect(ip, port).sync().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (!future.isSuccess()) {
                        log.error("Connection to nameserver error", future.cause());
                        eventLoopGroup.shutdownGracefully();
                    } else if (future.isSuccess()) {
                        log.info("Connection to nameserver success");
                    }
                }
            });
            channel = channelFuture.channel();
            channel.closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    log.info("Connection to nameserver closed");
                    eventLoopGroup.shutdownGracefully();
                }
            });
        } catch (Exception e) {
            log.error("Connection to nameserver error", e);
            eventLoopGroup.shutdownGracefully();
        }
    }

    public TcpMsg sendSynMsg(TcpMsg tcpMsg, String msgId) {
        channel.writeAndFlush(tcpMsg);
        return null;
    }
}
