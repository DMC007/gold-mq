package org.gold.dto;

/**
 * @author zhaoxun
 * @date 2025/10/24
 * @description 读取commitLog的消息载体
 */
public class ConsumerMsgCommitLogDTO {
    private String fileName;

    private long commitLogOffset;

    private int commitLogSize;

    private byte[] body;

    private int retryTimes;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getCommitLogOffset() {
        return commitLogOffset;
    }

    public void setCommitLogOffset(long commitLogOffset) {
        this.commitLogOffset = commitLogOffset;
    }

    public int getCommitLogSize() {
        return commitLogSize;
    }

    public void setCommitLogSize(int commitLogSize) {
        this.commitLogSize = commitLogSize;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }
}
