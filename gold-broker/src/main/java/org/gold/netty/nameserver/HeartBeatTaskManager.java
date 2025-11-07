package org.gold.netty.nameserver;

import com.alibaba.fastjson2.JSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.dto.HeartBeatDTO;
import org.gold.enums.NameServerEventCode;
import org.gold.enums.NameServerResponseCode;
import org.gold.remote.NameServerNettyRemoteClient;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhaoxun
 * @date 2025/11/6
 * @description 心跳任务管理器, 上报nameserver
 */
public class HeartBeatTaskManager {

    private static final Logger log = LogManager.getLogger(HeartBeatTaskManager.class);

    private AtomicInteger startMark = new AtomicInteger(0);

    public void startHeartBeatTask() {
        //心跳任务我们只需开启一次即可，这是为了放在网络波动的时候可能导致注册消息被传递了2次
        if (startMark.getAndIncrement() > 1) {
            return;
        }
        log.info("start heartbeat task");
        Thread thread = new Thread(new HeartBeatRequestTask());
        thread.setName("heartbeat-request-task");
        thread.start();
    }

    private class HeartBeatRequestTask implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(3);
                    NameServerNettyRemoteClient nameServerNettyRemoteClient = CommonCache.getNameServerClient().getNameServerNettyRemoteClient();
                    HeartBeatDTO heartBeatDTO = new HeartBeatDTO();
                    heartBeatDTO.setMsgId(UUID.randomUUID().toString());
                    TcpMsg tcpMsg = new TcpMsg(NameServerEventCode.HEART_BEAT.getCode(), JSON.toJSONBytes(heartBeatDTO));
                    TcpMsg tcpMsgRes = nameServerNettyRemoteClient.sendSynMsg(tcpMsg, heartBeatDTO.getMsgId());
                    if (NameServerResponseCode.HEART_BEAT_SUCCESS.getCode() != tcpMsgRes.getCode()) {
                        log.error("heartbeat failed, nameserver response code:{}, ", tcpMsgRes.getCode());
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
