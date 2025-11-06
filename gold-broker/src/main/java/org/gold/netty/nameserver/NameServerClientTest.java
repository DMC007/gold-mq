//package org.gold.netty.nameserver;
//
//import com.alibaba.fastjson2.JSON;
//import io.netty.bootstrap.Bootstrap;
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.Unpooled;
//import io.netty.channel.*;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.nio.NioSocketChannel;
//import io.netty.handler.codec.DelimiterBasedFrameDecoder;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.gold.coder.TcpMsg;
//import org.gold.coder.TcpMsgDecoder;
//import org.gold.coder.TcpMsgEncoder;
//import org.gold.constants.TcpConstants;
//import org.gold.dto.ServiceRegistryReqDTO;
//import org.gold.enums.NameServerEventCode;
//import org.gold.enums.RegistryTypeEnum;
//
//import java.util.UUID;
//
///**
// * @author zhaoxun
// * @date 2025/11/5
// */
//public class NameServerClientTest {
//
//    private static final Logger log = LogManager.getLogger(NameServerClientTest.class);
//
//    public static void main(String[] args) throws InterruptedException {
//        EventLoopGroup bossGroup = new NioEventLoopGroup();
//        Bootstrap bootstrap = new Bootstrap();
//        bootstrap.group(bossGroup)
//                .channel(NioSocketChannel.class)
//                .handler(new ChannelInitializer<NioSocketChannel>() {
//                    @Override
//                    protected void initChannel(NioSocketChannel ch) throws Exception {
//                        ByteBuf byteBuf = Unpooled.copiedBuffer(TcpConstants.DEFAULT_DECODE_CHAR.getBytes());
//                        //maxFrameLength – 解码帧的最大长度。如果帧的长度超过此值，则会抛出 TooLongFrameException 异常。
//                        ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024 * 8, byteBuf));
//                        ch.pipeline().addLast(new TcpMsgDecoder());
//                        ch.pipeline().addLast(new TcpMsgEncoder());
//                    }
//                });
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            log.info("NameServerClient shutdown closed");
//            bossGroup.shutdownGracefully();
//        }));
//        try {
//            ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 9093).addListener(new ChannelFutureListener() {
//                @Override
//                public void operationComplete(ChannelFuture future) throws Exception {
//                    log.info("NameServerClient startUp :{}", future.isSuccess());
//                }
//            }).sync();
//            Channel channel = channelFuture.channel();
//            channelFuture.addListener(new ChannelFutureListener() {
//                @Override
//                public void operationComplete(ChannelFuture future) throws Exception {
//                    ServiceRegistryReqDTO serviceRegistryReqDTO = new ServiceRegistryReqDTO();
//                    serviceRegistryReqDTO.setMsgId(UUID.randomUUID().toString());
//                    serviceRegistryReqDTO.setRegistryType(RegistryTypeEnum.BROKER.getCode());
//                    serviceRegistryReqDTO.setUser("gold_mq");
//                    serviceRegistryReqDTO.setPassword("gold_mq");
//                    TcpMsg tcpMsg = new TcpMsg(NameServerEventCode.REGISTRY.getCode(), JSON.toJSONBytes(serviceRegistryReqDTO));
//                    channel.writeAndFlush(tcpMsg);
//                }
//            });
//            channel.closeFuture().addListener(new ChannelFutureListener() {
//                @Override
//                public void operationComplete(ChannelFuture future) throws Exception {
//                    log.info("NameServerClient closed");
//                    bossGroup.shutdownGracefully();
//                }
//            });
//        } catch (Exception e) {
//            log.error("NameServerClient error:{}", e.getMessage(), e);
//            bossGroup.shutdownGracefully();
//        }
//    }
//}
