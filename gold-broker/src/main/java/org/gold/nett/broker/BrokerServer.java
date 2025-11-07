package org.gold.nett.broker;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.coder.TcpMsgDecoder;
import org.gold.coder.TcpMsgEncoder;
import org.gold.constants.TcpConstants;
import org.gold.event.EventBus;

/**
 * @author zhaoxun
 * @date 2025/11/7
 * @description broker服务端
 */
public class BrokerServer {

    private static final Logger log = LogManager.getLogger(BrokerServer.class);

    private int port;

    public BrokerServer(int port) {
        this.port = port;
    }

    public void startBrokerServer() throws InterruptedException {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ByteBuf delimiter = Unpooled.copiedBuffer(TcpConstants.DEFAULT_DECODE_CHAR.getBytes());
                        ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024 * 8, delimiter));
                        ch.pipeline().addLast(new TcpMsgDecoder());
                        ch.pipeline().addLast(new TcpMsgEncoder());
                        // 增加业务处理类
                        ch.pipeline().addLast(new BrokerServerHandler(new EventBus("broker-server-handle")));
                    }
                });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            log.info("BrokerServer shutdown closed");
        }));
        ChannelFuture channelFuture = bootstrap.bind(port).sync();
        channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                log.info("BrokerServer startUp success, port：{}", port);
            } else {
                log.error("BrokerServer startUp failed, port：{}", port);
            }
        });
        //阻塞代码
        channelFuture.channel().closeFuture().sync();
    }
}
