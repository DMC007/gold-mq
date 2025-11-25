package org.gold.transaction;

import org.gold.dto.MessageDTO;
import org.gold.enums.LocalTransactionState;

/**
 * @author zhaoxun
 * @date 2025/11/25
 */
public interface TransactionListener {
    /**
     * 执行本地事务逻辑处理的回调函数
     *
     * @param messageDTO 消息
     * @return 本地事务状态
     */
    LocalTransactionState executeLocalTransaction(final MessageDTO messageDTO);

    /**
     * 事务消息从broker回调到本地的回调接口
     *
     * @param messageDTO 消息
     * @return 本地事务状态
     */
    LocalTransactionState callBackHandler(final MessageDTO messageDTO);
}
