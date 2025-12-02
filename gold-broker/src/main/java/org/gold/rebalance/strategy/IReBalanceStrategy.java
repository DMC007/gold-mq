package org.gold.rebalance.strategy;

/**
 * @author zhaoxun
 * @date 2025/12/2
 */
public interface IReBalanceStrategy {
    /**
     * 根据不同策略执行重分配
     *
     * @param reBalanceInfo 重分配信息
     */
    void doReBalance(ReBalanceInfo reBalanceInfo);
}
