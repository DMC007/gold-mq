package org.gold.model;

import org.gold.utils.ByteConvertUtil;

/**
 * @author zhaoxun
 * @date 2025/10/21
 * @description consumerQueue数据结构存储的最小单元对象
 */
public class ConsumerQueueDetailModel {
    private int commitLogFilename;
    //4byte
    private int msgIndex; //commitLog数据存储的地址，mmap映射的地址，Integer.MAX校验

    private int msgLength;

    private int retryTimes;

    public int getCommitLogFilename() {
        return commitLogFilename;
    }

    public void setCommitLogFilename(int commitLogFilename) {
        this.commitLogFilename = commitLogFilename;
    }

    public int getMsgIndex() {
        return msgIndex;
    }

    public void setMsgIndex(int msgIndex) {
        this.msgIndex = msgIndex;
    }

    public int getMsgLength() {
        return msgLength;
    }

    public void setMsgLength(int msgLength) {
        this.msgLength = msgLength;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public byte[] convertToBytes() {
        byte[] commitLogFileNameBytes = ByteConvertUtil.intToBytes(commitLogFilename);
        byte[] msgIndexBytes = ByteConvertUtil.intToBytes(msgIndex);
        byte[] msgLengthBytes = ByteConvertUtil.intToBytes(msgLength);
        byte[] retryTimeBytes = ByteConvertUtil.intToBytes(retryTimes);
        byte[] finalBytes = new byte[16];
        int p = 0;
        for (int i = 0; i < 4; i++) {
            finalBytes[p++] = commitLogFileNameBytes[i];
        }
        for (int i = 0; i < 4; i++) {
            finalBytes[p++] = msgIndexBytes[i];
        }
        for (int i = 0; i < 4; i++) {
            finalBytes[p++] = msgLengthBytes[i];
        }
        for (int i = 0; i < 4; i++) {
            finalBytes[p++] = retryTimeBytes[i];
        }
        return finalBytes;
    }

    public void buildFromBytes(byte[] body) {
        this.setCommitLogFilename(ByteConvertUtil.bytesToInt(ByteConvertUtil.readInPos(body, 0, 4)));
        this.setMsgIndex(ByteConvertUtil.bytesToInt(ByteConvertUtil.readInPos(body, 4, 4)));
        this.setMsgLength(ByteConvertUtil.bytesToInt(ByteConvertUtil.readInPos(body, 8, 4)));
        this.setRetryTimes(ByteConvertUtil.bytesToInt(ByteConvertUtil.readInPos(body, 12, 4)));
    }
}
