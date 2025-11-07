package org.gold.remote;

import org.gold.common.NameServerSyncFutureManager;

import java.util.concurrent.*;

/**
 * @author zhaoxun
 * @date 2025/11/7
 */
public class NameServerSyncFuture implements Future {

    /**
     * 远程rpc返回的数据内容
     */
    private Object response;
    private String msgId;

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
        countDownLatch.countDown();
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return response != null;
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        countDownLatch.await();
        NameServerSyncFutureManager.removeSyncFuture(msgId);
        return response;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            countDownLatch.await(timeout, unit);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            NameServerSyncFutureManager.removeSyncFuture(msgId);
        }
        return response;
    }
}
