package org.gold.replication;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.util.internal.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.coder.TcpMsgDecoder;
import org.gold.coder.TcpMsgEncoder;
import org.gold.config.MasterSlaveReplicationProperties;
import org.gold.config.NameserverProperties;
import org.gold.config.TraceReplicationProperties;
import org.gold.constants.TcpConstants;
import org.gold.enums.ReplicationModeEnum;
import org.gold.enums.ReplicationRoleEnum;
import org.gold.event.EventBus;
import org.gold.handler.MasterReplicationServerHandler;
import org.gold.handler.NodeWriteMsgReplicationServerHandler;
import org.gold.handler.SlaveReplicationServerHandler;
import org.gold.utils.AssertUtils;

/**
 * @author zhaoxun
 * @date 2025/11/28
 */
public class ReplicationService {
    private static final Logger log = LogManager.getLogger(ReplicationService.class);

    public ReplicationModeEnum checkProperties() {
        NameserverProperties nameserverProperties = CommonCache.getNameserverProperties();
        String replicationMode = nameserverProperties.getReplicationMode();
        if (StringUtil.isNullOrEmpty(replicationMode)) {
            log.info("execute single mode...");
            return null;
        }
        ReplicationModeEnum replicationModeEnum = ReplicationModeEnum.getByCode(replicationMode);
        AssertUtils.isNotNull(replicationModeEnum, "replicationMode is error");
        if (replicationModeEnum == ReplicationModeEnum.MASTER_SLAVE) {
            //主从复制
            MasterSlaveReplicationProperties masterSlaveReplicationProperties = nameserverProperties.getMasterSlaveReplicationProperties();
            AssertUtils.isNotNull(masterSlaveReplicationProperties.getMaster(), "master can not be null");
            AssertUtils.isNotNull(masterSlaveReplicationProperties.getRole(), "role can not be null");
            AssertUtils.isNotNull(masterSlaveReplicationProperties.getType(), "type can not be null");
            AssertUtils.isNotNull(masterSlaveReplicationProperties.getPort(), "port can not be null");
        } else {
            //链路复制
            TraceReplicationProperties traceReplicationProperties = nameserverProperties.getTraceReplicationProperties();
            AssertUtils.isNotNull(traceReplicationProperties.getPort(), "port can not be null");
        }
        return replicationModeEnum;
    }

    public void startReplicationTask(ReplicationModeEnum replicationModeEnum) {
        //单机模式, 不需要开启复制进程
        if (replicationModeEnum == null) {
            return;
        }
        int port = 0;
        NameserverProperties nameserverProperties = CommonCache.getNameserverProperties();
        if (replicationModeEnum == ReplicationModeEnum.MASTER_SLAVE) {
            port = nameserverProperties.getMasterSlaveReplicationProperties().getPort();
        }

        ReplicationRoleEnum replicationRoleEnum;
        if (replicationModeEnum == ReplicationModeEnum.MASTER_SLAVE) {
            replicationRoleEnum = ReplicationRoleEnum.getByCode(nameserverProperties.getMasterSlaveReplicationProperties().getRole());
        } else {
            //链路复制模式
            String nextNode = nameserverProperties.getTraceReplicationProperties().getNextNode();
            if (StringUtil.isNullOrEmpty(nextNode)) {
                //尾节点
                replicationRoleEnum = ReplicationRoleEnum.TAIL_NODE;
            } else {
                //非尾节点
                replicationRoleEnum = ReplicationRoleEnum.NODE;
            }
            port = nameserverProperties.getTraceReplicationProperties().getPort();
        }
        int replicationPort = port;
        //是master角色，就开启netty进程同步数据给slave
        if (replicationRoleEnum == ReplicationRoleEnum.MASTER) {
            startNettyServerAsync(new MasterReplicationServerHandler(new EventBus("master-replication-task-")), replicationPort);
        } else if (replicationRoleEnum == ReplicationRoleEnum.SLAVE) {
            //slave主动链接master
            String masterAddress = nameserverProperties.getMasterSlaveReplicationProperties().getMaster();
            startNettyConnAsync(new SlaveReplicationServerHandler(new EventBus("slave-replication-task-")), masterAddress);
        } else if (replicationRoleEnum == ReplicationRoleEnum.NODE) {
            String nextNodeAddress = nameserverProperties.getTraceReplicationProperties().getNextNode();
            startNettyServerAsync(new NodeWriteMsgReplicationServerHandler(new EventBus("node-write-msg-replication-task-")), replicationPort);
            startNettyConnAsync(new SlaveReplicationServerHandler(new EventBus("node-send-replication-msg-task-")), nextNodeAddress);
        } else if (replicationRoleEnum == ReplicationRoleEnum.TAIL_NODE) {
            startNettyServerAsync(new NodeWriteMsgReplicationServerHandler(new EventBus("node-write-msg-replication-task-")), replicationPort);
        }
    }

    public void startNettyConnAsync(SimpleChannelInboundHandler<TcpMsg> simpleChannelInboundHandler, String address) {
        new Thread(() -> {
            NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ByteBuf delimiter = Unpooled.copiedBuffer(TcpConstants.DEFAULT_DECODE_CHAR.getBytes());
                            ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024 * 8, delimiter));
                            ch.pipeline().addLast(new TcpMsgDecoder());
                            ch.pipeline().addLast(new TcpMsgEncoder());
                            ch.pipeline().addLast(simpleChannelInboundHandler);
                        }
                    });
            Runtime.getRuntime().addShutdownHook(new Thread(eventLoopGroup::shutdownGracefully));
            try {
                String[] addr = address.split(":");
                ChannelFuture channelFuture = null;

                channelFuture = bootstrap.connect(addr[0], Integer.parseInt(addr[1])).sync().addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            log.error("connect master node error...");
                            eventLoopGroup.shutdownGracefully();
                        } else if (future.isSuccess()) {
                            log.info("connect master node success...");
                        }
                    }
                });
                //连接了master节点的channel对象，建议保存
                Channel channel = channelFuture.channel();
                log.info("connect master node success...");
                //保存连接的channel对象
                CommonCache.setConnectNodeChannel(channel);
                channelFuture.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                log.error("connect master node error...", e);
                eventLoopGroup.shutdownGracefully();
            }
        }).start();
    }

    public void startNettyServerAsync(SimpleChannelInboundHandler<TcpMsg> simpleChannelInboundHandler, int port) {
        new Thread(() -> {
            NioEventLoopGroup bossGroup = new NioEventLoopGroup();
            NioEventLoopGroup workerGroup = new NioEventLoopGroup();
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ByteBuf delimiter = Unpooled.copiedBuffer(TcpConstants.DEFAULT_DECODE_CHAR.getBytes());
                            ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024 * 8, delimiter));
                            ch.pipeline().addLast(new TcpMsgDecoder());
                            ch.pipeline().addLast(new TcpMsgEncoder());
                            ch.pipeline().addLast(simpleChannelInboundHandler);
                        }
                    });
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }));
            //master-slave架构
            //写入数据的节点，这里就会开启一个服务
            //非写入数据的节点，这里就需要链接一个服务
            //trace架构
            //又要接收外界数据，又要复制数据给外界
            try {
                ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
                channelFuture.addListener(future -> {
                    if (future.isSuccess()) {
                        log.info("BrokerServer startUp success, port：{}", port);
                    } else {
                        log.error("BrokerServer startUp failed, port：{}", port);
                    }
                });
                log.info("start nameserver's replication application on port:{}", port);
                //阻塞代码
                channelFuture.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                log.error("start nameserver's replication application error", e);
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }).start();
    }
}
