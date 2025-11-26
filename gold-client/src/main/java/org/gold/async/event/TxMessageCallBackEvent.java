package org.gold.async.event;

import org.gold.dto.TxMessageCallbackReqDTO;
import org.gold.event.model.Event;

/**
 * @author zhaoxun
 * @date 2025/11/26
 */
public class TxMessageCallBackEvent extends Event {
    private TxMessageCallbackReqDTO txMessageCallbackReqDTO;

    public TxMessageCallbackReqDTO getTxMessageCallbackReqDTO() {
        return txMessageCallbackReqDTO;
    }

    public void setTxMessageCallbackReqDTO(TxMessageCallbackReqDTO txMessageCallbackReqDTO) {
        this.txMessageCallbackReqDTO = txMessageCallbackReqDTO;
    }
}
