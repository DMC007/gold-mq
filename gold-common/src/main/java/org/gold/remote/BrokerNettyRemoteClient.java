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
import org.gold.common.BrokerServerSyncFutureManager;
import org.gold.constants.TcpConstants;

import java.util.concurrent.TimeUnit;

/**
 * @author zhaoxun
 * @date 2025/11/7
 * @description 对broker进行远程连接的客户端
 */
public class BrokerNettyRemoteClient {

    private static final Logger log = LogManager.getLogger(BrokerNettyRemoteClient.class);

    private String ip;
    private Integer port;

    public BrokerNettyRemoteClient(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
    }

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
    private Bootstrap bootstrap = new Bootstrap();
    private Channel channel;

    public void buildConnection(SimpleChannelInboundHandler<TcpMsg> simpleChannelInboundHandler) {
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ByteBuf delimiter = Unpooled.copiedBuffer(TcpConstants.DEFAULT_DECODE_CHAR.getBytes());
                        ch.pipeline().addLast(new DelimiterBasedFrameDecoder(8 * 1024, delimiter));
                        ch.pipeline().addLast(new TcpMsgEncoder());
                        ch.pipeline().addLast(new TcpMsgDecoder());
                        //业务处理handler
                        ch.pipeline().addLast(simpleChannelInboundHandler);
                    }
                });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook");
            eventLoopGroup.shutdownGracefully();
        }));
        ChannelFuture channelFuture = null;
        try {
            channelFuture = bootstrap.connect(ip, port).sync().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (!future.isSuccess()) {
                        log.error("Connection to broker error", future.cause());
                        eventLoopGroup.shutdownGracefully();
                    } else if (future.isSuccess()) {
                        log.info("Connection to broker success");
                    }
                }
            });
            channel = channelFuture.channel();
        } catch (Exception e) {
            log.error("Connection to broker error", e);
            eventLoopGroup.shutdownGracefully();
        }
    }

    public String getBrokerReqId() {
        return ip + ":" + port;
    }

    public TcpMsg sendSyncMsg(TcpMsg tcpMsg, String msgId) {
        //不要有传统springweb思维，这里我们的请求发出去之后，同步结果会在本身的netty服务的handler进行处理，
        // 这个时候通过Manager去设置好结果值，这里等待就能拿到结果，也就模拟出了同步的效果
        BrokerServerSyncFuture brokerServerSyncFuture = new BrokerServerSyncFuture();
        brokerServerSyncFuture.setMsgId(msgId);
        BrokerServerSyncFutureManager.putSyncFuture(msgId, brokerServerSyncFuture);
        //这里需要先往BrokerServerSyncFutureManager存入消息信息，如果writeAndFlush在前面(异步)，
        // 很可能在还没往Manager中存入消息, 主节点的响应就过来了，而SlaveSyncServerHandler处理响应的时候从manager获取信息是获取不到的
        //该方法是异步方法，返回的channelFuture
        channel.writeAndFlush(tcpMsg);
        try {
            //可能在等待结果的时候，节点挂掉了[get方法的finally会执行移除，无论是否异常，外面不用单独再去处理]
            return (TcpMsg) brokerServerSyncFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("send sync msg error", e);
            throw new RuntimeException(e);
        }
    }

    public void sendAsyncMsg(TcpMsg tcpMsg) {
        channel.writeAndFlush(tcpMsg);
    }

    public boolean isChannelActive() {
        return channel.isActive();
    }

    public void close() {
        this.channel.close().addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("Broker:{} connection closed", this.getBrokerReqId());
                eventLoopGroup.shutdownGracefully();
            } else {
                log.info("Broker:{} connection close error", this.getBrokerReqId());
            }
        });
    }
}
