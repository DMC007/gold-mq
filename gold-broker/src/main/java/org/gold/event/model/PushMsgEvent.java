package org.gold.event.model;

import org.gold.dto.MessageDTO;

/**
 * @author zhaoxun
 * @date 2025/11/10
 */
public class PushMsgEvent extends Event {
    private MessageDTO messageDTO;

    public MessageDTO getMessageDTO() {
        return messageDTO;
    }

    public void setMessageDTO(MessageDTO messageDTO) {
        this.messageDTO = messageDTO;
    }
}
