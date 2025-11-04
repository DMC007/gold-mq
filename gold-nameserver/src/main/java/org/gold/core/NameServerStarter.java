package org.gold.core;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.coder.TcpMsgDecoder;
import org.gold.coder.TcpMsgEncoder;
import org.gold.constants.TcpConstants;
import org.gold.handler.TcpNettyServerHandler;

/**
 * @author zhaoxun
 * @date 2025/11/4
 * @description 基于netty启动nameserver服务
 */
public class NameServerStarter {
    private static final Logger log = LogManager.getLogger(NameServerStarter.class);

    private int port;

    public NameServerStarter(int port) {
        this.port = port;
    }

    public void startServer() throws InterruptedException {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) throws Exception {
                        ByteBuf byteBuf = Unpooled.copiedBuffer(TcpConstants.DEFAULT_DECODE_CHAR.getBytes());
                        //maxFrameLength – 解码帧的最大长度。如果帧的长度超过此值，则会抛出 TooLongFrameException 异常。
                        channel.pipeline().addLast(new DelimiterBasedFrameDecoder(1024 * 8, byteBuf));
                        channel.pipeline().addLast(new TcpMsgDecoder());
                        channel.pipeline().addLast(new TcpMsgEncoder());
                        channel.pipeline().addLast(new TcpNettyServerHandler());
                    }
                });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            log.info("NameServer shutdown closed");
        }));
        ChannelFuture channelFuture = bootstrap.bind(port).sync();
        log.info("NameServer startUp success, port：{}", port);
        //阻塞代码
        channelFuture.channel().closeFuture().sync();
    }
}
