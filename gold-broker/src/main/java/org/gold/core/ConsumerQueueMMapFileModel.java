package org.gold.core;

import io.netty.util.internal.PlatformDependent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.constants.BrokerConstants;
import org.gold.model.GoldMqTopicModel;
import org.gold.model.QueueModel;
import org.gold.utils.LogFileNameUtil;
import org.gold.utils.PutMessageLock;
import org.gold.utils.UnfairReentrantLock;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhaoxun
 * @date 2025/10/22
 * @description 对consumeQueue文件做mmap映射的核心对象
 */
public class ConsumerQueueMMapFileModel {

    private static final Logger log = LogManager.getLogger(ConsumerQueueMMapFileModel.class);

    private File file;
    private MappedByteBuffer mappedByteBuffer;
    private ByteBuffer readByteBuffer;
    private FileChannel fileChannel;
    private String topic;
    private Integer queueId;
    private PutMessageLock putMessageLock;

    /**
     * 指定offset做文件的映射
     *
     * @param topicName         消息主题
     * @param queueId           队列id
     * @param startOffset       开始映射的offset
     * @param latestWriteOffset 最新写入的offset
     * @param mappedSize        映射的体积 (byte)
     */
    public void loadFileToMMap(String topicName, Integer queueId, int startOffset, int latestWriteOffset, int mappedSize) throws IOException {
        this.topic = topicName;
        this.queueId = queueId;
        String filePath = this.getLatestConsumeQueueFile();
        this.doMMap(filePath, startOffset, latestWriteOffset, mappedSize);
        //默认非公平
        putMessageLock = new UnfairReentrantLock();
    }

    /**
     * 写入内容[性能更高]
     *
     * @param content 内容
     */
    public void writeContent(byte[] content) {
        writeContent(content, false);
    }

    /**
     * 写入内容
     *
     * @param content 要写入的内容
     * @param force   强制刷盘
     */
    public void writeContent(byte[] content, boolean force) {
        try {
            putMessageLock.lock();
            mappedByteBuffer.put(content);
            if (force) {
                mappedByteBuffer.force();
            }
        } finally {
            putMessageLock.unlock();
        }
    }

    /**
     * 读取consumerqueue数据内容
     *
     * @param pos 消息读取开始位置
     * @return 消息内容
     */
    public byte[] readContent(int pos) {
        //ConsumeQueue每个单元文件存储的固定大小是16字节
        //readByteBuffer pos:0~limit(开了一个窗口) -》readBuf
        //readBuf 任意修改
        ByteBuffer readBuf = readByteBuffer.slice();
        readBuf.position(pos);
        byte[] content = new byte[BrokerConstants.CONSUME_QUEUE_EACH_MSG_SIZE];
        readBuf.get(content);
        return content;
    }


    /**
     * 读取consumerqueue数据内容
     *
     * @param pos      消息读取开始位置
     * @param msgCount 消息条数
     * @return 消息内容
     */
    public List<byte[]> readContent(int pos, int msgCount) {
        ByteBuffer readBuf = readByteBuffer.slice();
        readBuf.position(pos);
        List<byte[]> loadContentList = new ArrayList<>();
        for (int i = 0; i < msgCount; i++) {
            byte[] content = new byte[BrokerConstants.CONSUME_QUEUE_EACH_MSG_SIZE];
            //根据源码，内部position会在读取后自动累加
            readBuf.get(content);
            loadContentList.add(content);
        }
        return loadContentList;
    }

    /**
     * mmap映射文件
     *
     * @param filePath          文件路径
     * @param startOffset       映射的开始offset
     * @param latestWriteOffset 最新写入的offset
     * @param mappedSize        映射的体积 (byte)
     */
    @SuppressWarnings({"resource"})
    private void doMMap(String filePath, int startOffset, int latestWriteOffset, int mappedSize) throws IOException {
        file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("file is not exist! filePath is " + filePath);
        }
        this.fileChannel = new RandomAccessFile(file, "rw").getChannel();
        this.mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, startOffset, mappedSize);
        //读专用[底层共用一个内存映射]
        this.readByteBuffer = mappedByteBuffer.slice();
        this.mappedByteBuffer.position(latestWriteOffset);
    }

    /**
     * 获取最新的commitLog文件路径
     *
     * @return 文件路径
     */
    private String getLatestConsumeQueueFile() {
        GoldMqTopicModel goldMqTopicModel = CommonCache.getGoldMqTopicModelMap().get(topic);
        if (goldMqTopicModel == null) {
            throw new IllegalArgumentException("topic is inValid! topicName is " + topic);
        }
        List<QueueModel> queueModelList = goldMqTopicModel.getQueueList();
        QueueModel queueModel = queueModelList.get(queueId);
        if (queueModel == null) {
            throw new IllegalArgumentException("queueId is inValid! queueId is " + queueId);
        }
        int diff = queueModel.getOffsetLimit();
        String filePath = null;
        if (diff == 0) {
            //已经写满了, 就创建一个新的文件
            ConsumerQueueFilePath consumerQueueFilePath = this.createNewConsumeQueueFile(queueModel.getFileName());
            filePath = consumerQueueFilePath.getFilePath();
        } else if (diff > 0) {
            //还有机会写入
            filePath = LogFileNameUtil.buildConsumeQueueFilePath(topic, queueId, queueModel.getFileName());
        }
        return filePath;
    }

    static class ConsumerQueueFilePath {
        private String fileName;
        private String filePath;

        public ConsumerQueueFilePath(String fileName, String filePath) {
            this.fileName = fileName;
            this.filePath = filePath;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
    }

    private ConsumerQueueFilePath createNewConsumeQueueFile(String fileName) {
        String newFileName = LogFileNameUtil.incrConsumeQueueFileName(fileName);
        String newFilePath = LogFileNameUtil.buildConsumeQueueFilePath(topic, queueId, newFileName);
        File newConsumeQueueFile = new File(newFilePath);
        try {
            //新的commitLog文件创建
            boolean newFile = newConsumeQueueFile.createNewFile();
            if (newFile) {
                log.info("create new consumerqueueFile success! fileName is {}", newFileName);
            } else {
                throw new RuntimeException("create new consumerqueueFile fail! fileName is " + newFileName);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ConsumerQueueFilePath(newFileName, newFilePath);
    }

    public void checkConsumerQueueHasEnableSpace(int queueId, byte[] content) throws IOException {
        GoldMqTopicModel goldMqTopicModel = CommonCache.getGoldMqTopicModelMap().get(topic);
        List<QueueModel> queueList = goldMqTopicModel.getQueueList();
        QueueModel queueModel = queueList.stream().filter(queue -> queue.getId().equals(queueId)).findFirst().orElse(null);
        if (queueModel == null) {
            throw new IllegalArgumentException("queueId is inValid! queueId is " + queueId + ", topic is " + topic);
        }
        int writeAbleOffsetNum = queueModel.countDiff();
        //空间不足，需要创建新的consumerqueue文件并且做映射
        if (!(writeAbleOffsetNum >= content.length)) {
            //00000000文件 -》00000001文件
            //consumerqueue剩余150byte大小的空间，最新的消息体积是151byte
            ConsumerQueueFilePath consumerQueueFilePath = this.createNewConsumeQueueFile(queueModel.getFileName());
            queueModel.setFileName(consumerQueueFilePath.getFileName());
            queueModel.setOffsetLimit(BrokerConstants.COMSUMERQUEUE_DEFAULT_MMAP_SIZE);
            queueModel.setLastOffset(0);
            queueModel.setLatestOffset(new AtomicInteger(0));
            //TODO 后面性能优化可以做延迟删除
            //创建新文件之前, 先清理旧资源, 释放掉旧资源
            this.clean(true);
            //新文件路径映射进来
            this.doMMap(consumerQueueFilePath.getFilePath(), 0, 0, BrokerConstants.COMSUMERQUEUE_DEFAULT_MMAP_SIZE);
        }
    }

    /**
     * 释放文件映射的内存
     */
    public void clean(boolean force) {
        if (fileChannel != null) {
            try {
                fileChannel.close();
            } catch (IOException e) {
                log.error("close fileChannel error");
            }
        }
        try {
            if (mappedByteBuffer == null || !mappedByteBuffer.isDirect() || mappedByteBuffer.capacity() == 0) {
                return;
            }
            if (force) {
                mappedByteBuffer.force();
            }
            //直接调用netty封装好的工具类释放直接内存
            PlatformDependent.freeDirectBuffer(mappedByteBuffer);
        } catch (Exception e) {
            log.error("freeDirectBuffer error :{}", e.getMessage(), e);
        }
    }

    public Integer getQueueId() {
        return queueId;
    }
}
