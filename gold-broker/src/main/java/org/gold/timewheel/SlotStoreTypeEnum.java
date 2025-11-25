package org.gold.timewheel;

import org.gold.dto.MessageDTO;
import org.gold.dto.MessageRetryDTO;
import org.gold.dto.TxMessageDTO;

/**
 * @author zhaoxun
 * @date 2025/11/24
 */
public enum SlotStoreTypeEnum {
    MESSAGE_RETRY_DTO(MessageRetryDTO.class),
    DELAY_MESSAGE_DTO(MessageDTO.class),
    TX_MESSAGE_DTO(TxMessageDTO.class),
    ;
    Class<?> clazz;

    SlotStoreTypeEnum(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Class<?> getClazz() {
        return clazz;
    }
}
