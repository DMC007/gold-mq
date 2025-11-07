package org.gold.netty.nameserver;

import com.alibaba.fastjson2.JSON;
import io.netty.util.internal.StringUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.config.GlobalProperties;
import org.gold.dto.PullBrokerIpDTO;
import org.gold.dto.PullBrokerIpRespDTO;
import org.gold.dto.ServiceRegistryReqDTO;
import org.gold.enums.*;
import org.gold.remote.NameServerNettyRemoteClient;

import java.net.Inet4Address;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author zhaoxun
 * @date 2025/11/6
 */
public class NameServerClient {

    private static final Logger log = LogManager.getLogger(NameServerClient.class);

    private NameServerNettyRemoteClient nameServerNettyRemoteClient;

    public NameServerNettyRemoteClient getNameServerNettyRemoteClient() {
        return nameServerNettyRemoteClient;
    }

    public void initConnection() {
        String nameserverIp = CommonCache.getGlobalProperties().getNameserverIp();
        Integer nameserverPort = CommonCache.getGlobalProperties().getNameserverPort();
        if (StringUtil.isNullOrEmpty(nameserverIp) || nameserverPort == null || nameserverPort <= 0) {
            log.error("nameserver ip or port is null or error");
            throw new RuntimeException("nameserver ip or port is null or error");
        }
        log.info("connect to nameserver: {}:{}", nameserverIp, nameserverPort);
        nameServerNettyRemoteClient = new NameServerNettyRemoteClient(nameserverIp, nameserverPort);
        nameServerNettyRemoteClient.buildConnection();
    }

    public void sendRegistryMsg() {
        ServiceRegistryReqDTO reqDTO = new ServiceRegistryReqDTO();
        try {
            Map<String, Object> attrs = new HashMap<>();
            GlobalProperties globalProperties = CommonCache.getGlobalProperties();
            //赋值属性
            reqDTO.setIp(Inet4Address.getLocalHost().getHostAddress());
            reqDTO.setPort(globalProperties.getBrokerPort());
            reqDTO.setUser(globalProperties.getNameserverUser());
            reqDTO.setPassword(globalProperties.getNameserverPassword());
            reqDTO.setRegistryType(RegistryTypeEnum.BROKER.getCode());
            reqDTO.setMsgId(UUID.randomUUID().toString());
            //TODO 后期增加集群相关功能
            attrs.put("role", "single");
            reqDTO.setAttrs(attrs);
            byte[] body = JSON.toJSONBytes(reqDTO);
            //发送注册事件消息给nameserver
            TcpMsg tcpMsg = new TcpMsg(NameServerEventCode.REGISTRY.getCode(), body);
            TcpMsg responseTcpMsg = nameServerNettyRemoteClient.sendSynMsg(tcpMsg, reqDTO.getMsgId());
            int code = responseTcpMsg.getCode();
            if (NameServerResponseCode.REGISTRY_SUCCESS.getCode() == code) {
                log.info("register success, start the heartbeat mission..");
                //启动心跳任务
                CommonCache.getHeartBeatTaskManager().startHeartBeatTask();
            } else if (NameServerResponseCode.ERROR_USER_OR_PASSWORD.getCode() == code) {
                log.error("register error, user or password error");
                throw new RuntimeException("register error, user or password error");
            }
            log.info("register response: {}", JSON.toJSONString(responseTcpMsg));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询broker的master主节点地址
     *
     * @return broker的master主节点地址
     */
    public String queryBrokerMasterAddress() {
        String clusterMode = CommonCache.getGlobalProperties().getBrokerClusterMode();
        if (!BrokerClusterModeEnum.MASTER_SLAVE.getCode().equals(clusterMode)) {
            log.warn("broker cluster mode is not master-slave, return null");
            return null;
        }
        PullBrokerIpDTO reqDTO = new PullBrokerIpDTO();
        reqDTO.setRole(BrokerRegistryRoleEnum.MASTER.getCode());
        reqDTO.setBrokerClusterGroup(CommonCache.getGlobalProperties().getBrokerClusterGroup());
        reqDTO.setMsgId(UUID.randomUUID().toString());
        TcpMsg tcpMsg = new TcpMsg(NameServerEventCode.PULL_BROKER_IP_LIST.getCode(), JSON.toJSONBytes(reqDTO));
        TcpMsg responseTcpMsg = nameServerNettyRemoteClient.sendSynMsg(tcpMsg, reqDTO.getMsgId());
        PullBrokerIpRespDTO pullBrokerIpRespDTO = JSON.parseObject(responseTcpMsg.getBody(), PullBrokerIpRespDTO.class);
        //这里采用单master模式，因此只返回第一个
        List<String> masterAddressList = pullBrokerIpRespDTO.getMasterAddressList();
        if (CollectionUtils.isEmpty(masterAddressList)) {
            log.warn("no master broker ip address found");
            return null;
        }
        return masterAddressList.getFirst();
    }

}
