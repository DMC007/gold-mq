package org.gold.event.spi.listener;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import io.netty.util.internal.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.enums.NameServerResponseCode;
import org.gold.event.Listener;
import org.gold.event.model.UnRegistryEvent;
import org.gold.store.ServiceInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author zhaoxun
 * @date 2025/11/5
 */
public class UnRegistryListener implements Listener<UnRegistryEvent> {

    private static final Logger log = LogManager.getLogger(UnRegistryListener.class);

    @Override
    public void onReceive(UnRegistryEvent event) throws IllegalAccessException {
        ChannelHandlerContext ctx = event.getChannelHandlerContext();
        Object reqId = ctx.channel().attr(AttributeKey.valueOf("reqId")).get();
        if (reqId == null) {
            TcpMsg tcpMsg = new TcpMsg(NameServerResponseCode.ERROR_USER_OR_PASSWORD.getCode(),
                    NameServerResponseCode.ERROR_USER_OR_PASSWORD.getDesc().getBytes());
            ctx.writeAndFlush(tcpMsg);
            ctx.close();
            throw new IllegalAccessException("Authentication failed");
        }
        log.info("UnRegistryEvent:{}", JSON.toJSONString(event));
        String reqIdStr = reqId.toString();
        //需要确认下是不是broker的master下线，如果是就需要切换保证业务可用
        ServiceInstance needRemoveServiceInstance = CommonCache.getServiceInstanceManager().get(reqIdStr);
        Map<String, Object> attrs = needRemoveServiceInstance.getAttrs();
        String brokerClusterGroup = (String) attrs.getOrDefault("group", "");
        String brokerClusterRole = (String) attrs.getOrDefault("role", "");
        //判断下是集群模式还是单机模式
        boolean isClusterMode = !StringUtil.isNullOrEmpty(brokerClusterGroup) && !StringUtil.isNullOrEmpty(brokerClusterRole);
        if (isClusterMode) {
            if ("master".equals(brokerClusterRole)) {
                log.error("master node fail!");
                //从节点备选集合
                List<ServiceInstance> reloadNodeList = new ArrayList<>();
                Map<String, ServiceInstance> serviceInstanceMap = CommonCache.getServiceInstanceManager().getServiceInstanceMap();
                for (ServiceInstance serviceInstance : serviceInstanceMap.values()) {
                    //赛选出同组的从节点，放入备选集合
                    String matchGroup = (String) serviceInstance.getAttrs().getOrDefault("group", "");
                    String matchRole = (String) serviceInstance.getAttrs().getOrDefault("role", "");
                    if (matchGroup.equals(brokerClusterGroup) && "slave".equals(matchRole)) {
                        reloadNodeList.add(serviceInstance);
                    }
                }
                log.info("find same cluster group slave nodes:{}", JSON.toJSONString(reloadNodeList));
                //移除之前的master节点，选择从节点中版本号最新的作为新主节点
                long maxVersion = 0;
                ServiceInstance newMasterServiceInstance = null;
                for (ServiceInstance salveServiceInstance : reloadNodeList) {
                    long lastVersion = (long) salveServiceInstance.getAttrs().getOrDefault("lastVersion", 0);
                    if (maxVersion <= lastVersion) {
                        newMasterServiceInstance = salveServiceInstance;
                        maxVersion = lastVersion;
                    }
                }
                CommonCache.getServiceInstanceManager().remove(reqIdStr);
                if (newMasterServiceInstance != null) {
                    newMasterServiceInstance.getAttrs().put("role", "master");
                }
                //重新设置主从关系
                CommonCache.getServiceInstanceManager().reload(reloadNodeList);
                log.info("new cluster node is:{}", JSON.toJSONString(newMasterServiceInstance));
            }
        }
        //移除需要下线的节点信息
        CommonCache.getServiceInstanceManager().remove(reqIdStr);
        //关闭连接
        if (ctx.channel().isActive()) {
            ctx.close();
        }
    }
}
