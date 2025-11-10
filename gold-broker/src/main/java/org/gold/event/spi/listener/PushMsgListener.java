package org.gold.event.spi.listener;

import org.gold.cache.CommonCache;
import org.gold.dto.MessageDTO;
import org.gold.event.Listener;
import org.gold.event.model.PushMsgEvent;

import java.io.IOException;

/**
 * @author zhaoxun
 * @date 2025/11/10
 */
public class PushMsgListener implements Listener<PushMsgEvent> {
    @Override
    public void onReceive(PushMsgEvent event) throws Exception {
        //将消息写入commitLog
        MessageDTO messageDTO = event.getMessageDTO();
        //TODO 是否是延迟消息
        boolean isDelay = messageDTO.getDelay() > 0;
        //TODO 是否是事务消息
        if (isDelay) {
            //TODO 延迟消息处理
        } else {
            //普通消息处理[event里面的ctx需要用来做响应]
            this.appendDefaultMsgHandler(messageDTO, event);
        }
    }

    private void appendDefaultMsgHandler(MessageDTO messageDTO, PushMsgEvent event) throws IOException {
        CommonCache.getCommitLogAppendHandler().appendMessage(messageDTO, event);
    }
}
