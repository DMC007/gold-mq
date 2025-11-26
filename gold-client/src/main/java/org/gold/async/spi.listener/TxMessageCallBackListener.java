package org.gold.async.spi.listener;

import com.alibaba.fastjson2.JSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.async.event.TxMessageCallBackEvent;
import org.gold.coder.TcpMsg;
import org.gold.common.CommonCache;
import org.gold.dto.MessageDTO;
import org.gold.dto.TxMessageCallbackReqDTO;
import org.gold.enums.BrokerEventCode;
import org.gold.enums.LocalTransactionState;
import org.gold.enums.TxMessageFlagEnum;
import org.gold.event.Listener;
import org.gold.transaction.TransactionListener;
import org.gold.utils.AssertUtils;

/**
 * @author zhaoxun
 * @date 2025/11/26
 */
public class TxMessageCallBackListener implements Listener<TxMessageCallBackEvent> {

    private static final Logger log = LogManager.getLogger(TxMessageCallBackListener.class);

    @Override
    public void onReceive(TxMessageCallBackEvent event) throws Exception {
        TxMessageCallbackReqDTO txMessageCallbackReqDTO = event.getTxMessageCallbackReqDTO();
        AssertUtils.isNotNull(txMessageCallbackReqDTO.getMessageDTO().getProducerId(), "producerId is null");
        AssertUtils.isNotNull(txMessageCallbackReqDTO.getMessageDTO().getMsgId(), "msgId is null");
        TransactionListener transactionListener = CommonCache.getTransactionListenerMap().get(txMessageCallbackReqDTO.getMessageDTO().getProducerId());
        LocalTransactionState localTransactionState = transactionListener.callBackHandler(txMessageCallbackReqDTO.getMessageDTO());

        MessageDTO messageDTO = txMessageCallbackReqDTO.getMessageDTO();
        if (LocalTransactionState.COMMIT == localTransactionState) {
            messageDTO.setTxFlag(TxMessageFlagEnum.REMAIN_HALF_ACK.getCode());
            messageDTO.setLocalTxState(LocalTransactionState.COMMIT.getCode());
            TcpMsg remainHalfMsg = new TcpMsg(BrokerEventCode.PUSH_MSG.getCode(), JSON.toJSONBytes(messageDTO));
            event.getChannelHandlerContext().writeAndFlush(remainHalfMsg);
            log.info("commit half msg, msgId: {}", messageDTO.getMsgId());
        } else if (LocalTransactionState.ROLLBACK == localTransactionState) {
            messageDTO.setTxFlag(TxMessageFlagEnum.REMAIN_HALF_ACK.getCode());
            messageDTO.setLocalTxState(LocalTransactionState.ROLLBACK.getCode());
            TcpMsg remainHalfMsg = new TcpMsg(BrokerEventCode.PUSH_MSG.getCode(), JSON.toJSONBytes(messageDTO));
            event.getChannelHandlerContext().writeAndFlush(remainHalfMsg);
            log.info("rollback half msg, msgId: {}", messageDTO.getMsgId());
        }
        //unknow暂不处理
    }
}
