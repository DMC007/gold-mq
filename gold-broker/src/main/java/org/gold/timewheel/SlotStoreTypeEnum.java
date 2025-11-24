package org.gold.timewheel;

import org.gold.dto.MessageDTO;
import org.gold.dto.MessageRetryDTO;

/**
 * @author zhaoxun
 * @date 2025/11/24
 */
public enum SlotStoreTypeEnum {
    MESSAGE_RETRY_DTO(MessageRetryDTO.class),
    DELAY_MESSAGE_DTO(MessageDTO.class),
    //TODO 事务消息
    ;
    Class<?> clazz;

    SlotStoreTypeEnum(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Class<?> getClazz() {
        return clazz;
    }
}
