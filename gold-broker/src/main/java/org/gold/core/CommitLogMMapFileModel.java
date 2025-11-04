package org.gold.core;

import com.alibaba.fastjson2.JSON;
import io.netty.util.internal.PlatformDependent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.constants.BrokerConstants;
import org.gold.dto.ConsumerMsgCommitLogDTO;
import org.gold.dto.MessageDTO;
import org.gold.model.*;
import org.gold.utils.LogFileNameUtil;
import org.gold.utils.PutMessageLock;
import org.gold.utils.UnfairReentrantLock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhaoxun
 * @date 2025/10/21
 * @description 最基础的mmap对象模型
 */
public class CommitLogMMapFileModel {
    private static final Logger log = LogManager.getLogger(CommitLogMMapFileModel.class);

    private File file;
    private MappedByteBuffer mappedByteBuffer;
    private ByteBuffer readByteBuffer;
    private FileChannel fileChannel;
    private String topic;
    private PutMessageLock putMessageLock;

    /**
     * 指定offset做文件的映射
     *
     * @param topicName   消息主题
     * @param startOffset 开始映射的offset
     * @param mappedSize  映射的体积 (byte)
     */
    public void loadFileToMMap(String topicName, int startOffset, int mappedSize) throws IOException {
        this.topic = topicName;
        String filePath = this.getLatestCommitLogFile(topicName);
        this.doMMap(filePath, startOffset, mappedSize);
        //默认非公平
        this.putMessageLock = new UnfairReentrantLock();
    }

    /**
     * mmap映射文件
     *
     * @param filePath    文件路径
     * @param startOffset 映射的开始offset
     * @param mappedSize  映射的体积 (byte)
     */
    @SuppressWarnings({"resource"})
    private void doMMap(String filePath, int startOffset, int mappedSize) throws IOException {
        file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("filePath is " + filePath + " inValid");
        }
        //后面只需要关闭fileChannel即可，它的底层会自动关闭RandomAccessFile
        fileChannel = new RandomAccessFile(file, "rw").getChannel();
        mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, startOffset, mappedSize);
        //从指定位置开始追加写入
        this.readByteBuffer = mappedByteBuffer.slice();
        GoldMqTopicModel goldMqTopicModel = CommonCache.getGoldMqTopicModelMap().get(topic);
        //定位到指定位置
        this.mappedByteBuffer.position(goldMqTopicModel.getCommitLogModel().getOffset().get());
    }

    /**
     * 获取最新的commitLog文件路径
     *
     * @param topicName 消息主题
     * @return commitLog文件路径
     */
    public String getLatestCommitLogFile(String topicName) {
        GoldMqTopicModel goldMqTopicModel = CommonCache.getGoldMqTopicModelMap().get(topicName);
        if (goldMqTopicModel == null) {
            throw new IllegalArgumentException("topic is inValid! topicName is " + topicName);
        }
        CommitLogModel commitLogModel = goldMqTopicModel.getCommitLogModel();
        long diff = commitLogModel.countDiff();
        String filePath = null;
        if (diff == 0) {
            //写满了
            CommitLogFilePath newCommitLogFile = this.createNewCommitLogFile(topicName, commitLogModel);
            filePath = newCommitLogFile.getFilePath();
        } else if (diff > 0) {
            //还有机会写入
            filePath = LogFileNameUtil.buildCommitLogFilePath(topicName, commitLogModel.getFileName());
        }
        return filePath;
    }

    /**
     * 写入内容到磁盘中
     *
     * @param messageDTO 消息对象
     * @param force      是否强制刷盘
     */
    public void writeContent(MessageDTO messageDTO, boolean force) throws IOException {
        //定位到最新的commitLog文件中，记录下当前文件是否已经写满，如果写满，则创建新的文件，并且做新的mmap映射 done
        //如果当前文件没有写满，对content内容做一层封装 done
        //再判断写入是否会导致commitLog写满，如果不会，则选择当前commitLog，如果会则创建新文件，并且做mmap映射 done
        //定位到最新的commitLog文件之后，写入 done

        //定义一个对象专门管理各个topic的最新写入offset值，并且定时刷新到磁盘中（缺少了同步到磁盘的机制）
        //写入数据，offset变更，如果是高并发场景，offset是不是会被多个线程访问？

        //offset会用一个原子类AtomicLong去管理
        //线程安全问题：线程1：111，线程2：122
        //加锁机制 （锁的选择非常重要）
        GoldMqTopicModel goldMqTopicModel = CommonCache.getGoldMqTopicModelMap().get(topic);
        if (goldMqTopicModel == null) {
            throw new IllegalArgumentException("goldMqTopicModel is null, topic is " + topic);
        }
        CommitLogModel commitLogModel = goldMqTopicModel.getCommitLogModel();
        if (commitLogModel == null) {
            throw new IllegalArgumentException("commitLogModel is null, topic is " + topic);
        }
        //默认刷到page cache中，
        //如果需要强制刷盘，这里要兼容
        putMessageLock.lock();
        CommitLogMessageModel commitLogMessageModel = new CommitLogMessageModel();
        commitLogMessageModel.setContent(messageDTO.getBody());
        //检测commitLog文件是否还有可用空间
        this.checkCommitLogHasEnableSpace(commitLogMessageModel);
        //继续执行
        byte[] writeContent = commitLogMessageModel.getContent();
        mappedByteBuffer.put(writeContent);
        AtomicInteger currentLatestMsgOffset = commitLogModel.getOffset();
        //对consumerqueue文件进行写入
        this.dispatcher(messageDTO, currentLatestMsgOffset.get());
        currentLatestMsgOffset.addAndGet(writeContent.length);
        if (force) {
            mappedByteBuffer.force();
        }
        putMessageLock.unlock();
    }

    /**
     * 支持从文件的指定offset开始读取内容
     *
     * @param pos    读取开始位置
     * @param length 读取长度
     * @return 读取内容
     */
    public ConsumerMsgCommitLogDTO readContent(int pos, int length) {
        ByteBuffer readBuf = readByteBuffer.slice();
        readBuf.position(pos);
        byte[] readBytes = new byte[length];
        //读取数据[底层源码get后会移动position位置，所以这里readByteBuffer.slice()再变一个readBuf引用副本出来，就是为了不去破坏readByteBuffer本身结构]
        readBuf.get(readBytes);
        ConsumerMsgCommitLogDTO consumerMsgCommitLogDTO = new ConsumerMsgCommitLogDTO();
        consumerMsgCommitLogDTO.setFileName(file.getName());
        consumerMsgCommitLogDTO.setCommitLogOffset(pos);
        consumerMsgCommitLogDTO.setCommitLogSize(length);
        consumerMsgCommitLogDTO.setBody(readBytes);
        return consumerMsgCommitLogDTO;
    }

    /**
     * 写入consumerqueue文件
     *
     * @param messageDTO 消息对象
     * @param msgIndex   消息索引[最新commitLog文件写入数据的地址]
     */
    private void dispatcher(MessageDTO messageDTO, int msgIndex) throws IOException {
        GoldMqTopicModel goldMqTopicModel = CommonCache.getGoldMqTopicModelMap().get(topic);
        if (goldMqTopicModel == null) {
            throw new IllegalArgumentException("goldMqTopicModel is null, topic is " + topic);
        }
        int queueId;
        if (messageDTO.getQueueId() >= 0) {
            queueId = messageDTO.getQueueId();
        } else {
            //todo 后续大家可以在这里自由扩展不同的消息分派策略
            int queueSize = goldMqTopicModel.getQueueList().size();
            queueId = new Random().nextInt(queueSize);
        }
        //consumerQueue数据结构存储的最小单元对象[这里面不存储实际内容, 把它看作存储索引即可]
        ConsumerQueueDetailModel consumerQueueDetailModel = new ConsumerQueueDetailModel();
        consumerQueueDetailModel.setCommitLogFilename(Integer.parseInt(goldMqTopicModel.getCommitLogModel().getFileName()));
        consumerQueueDetailModel.setMsgIndex(msgIndex);
        consumerQueueDetailModel.setRetryTimes(messageDTO.getCurrentRetryTimes());
        consumerQueueDetailModel.setMsgLength(messageDTO.getBody().length);
        log.info("write consumerQueue data : {}", JSON.toJSONString(consumerQueueDetailModel));
        //转换后拿到真实物理需要写入文件的内容
        byte[] content = consumerQueueDetailModel.convertToBytes();
        List<ConsumerQueueMMapFileModel> consumeQueueMMapFileModels = CommonCache.getConsumerQueueMMapFileModelManager().get(topic);
        ConsumerQueueMMapFileModel consumeQueueMMapFileModel = consumeQueueMMapFileModels.stream()
                .filter(consumerQueueModel -> consumerQueueModel.getQueueId().equals(queueId))
                .findFirst()
                .orElse(null);
        if (consumeQueueMMapFileModel == null) {
            throw new IllegalArgumentException("consumeQueueMMapFileModel is null, queueId is " + queueId + ", topic is " + topic);
        }
        //看下当前的映射文件例如:是否需要从00000000变成00000001
        consumeQueueMMapFileModel.checkConsumerQueueHasEnableSpace(queueId, content);
        //刷新offset
        consumeQueueMMapFileModel.writeContent(content);
        QueueModel queueModel = goldMqTopicModel.getQueueList().get(queueId);
        queueModel.getLatestOffset().addAndGet(content.length);
    }

    private void checkCommitLogHasEnableSpace(CommitLogMessageModel commitLogMessageModel) throws IOException {
        GoldMqTopicModel goldMqTopicModel = CommonCache.getGoldMqTopicModelMap().get(topic);
        CommitLogModel commitLogModel = goldMqTopicModel.getCommitLogModel();
        Long writeAbleOffsetNum = commitLogModel.countDiff();
        //空间不足，需要创建新的commitLog文件并且做映射
        if (!(writeAbleOffsetNum >= commitLogMessageModel.getContent().length)) {
            //00000000文件 -》00000001文件
            //commitLog剩余150byte大小的空间，最新的消息体积是151byte
            CommitLogFilePath newCommitLogFile = this.createNewCommitLogFile(topic, commitLogModel);
            commitLogModel.setOffsetLimit(Long.valueOf(BrokerConstants.COMMIT_LOG_DEFAULT_MMAP_SIZE));
            commitLogModel.setOffset(new AtomicInteger(0));
            commitLogModel.setFileName(newCommitLogFile.getFileName());
            //创建新文件之前, 先清理旧资源, 释放掉旧资源
            this.clean(true);
            //新文件路径映射进来
            this.doMMap(newCommitLogFile.getFilePath(), 0, BrokerConstants.COMMIT_LOG_DEFAULT_MMAP_SIZE);
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

    static class CommitLogFilePath {
        private String fileName;
        private String filePath;

        public CommitLogFilePath(String fileName, String filePath) {
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

    /**
     * 创建新的commitLog路径文件对象
     *
     * @param topicName      消息主题
     * @param commitLogModel commitLog文件对象
     * @return 新的commitLog路径文件对象
     */
    private CommitLogFilePath createNewCommitLogFile(String topicName, CommitLogModel commitLogModel) {
        String newFileName = LogFileNameUtil.incrCommitLogFileName(commitLogModel.getFileName());
        String newFilePath = LogFileNameUtil.buildCommitLogFilePath(topicName, newFileName);
        File newCommitLogFile = new File(newFilePath);
        try {
            boolean newFile = newCommitLogFile.createNewFile();
            if (newFile) {
                log.info("create new commitLogFile success! fileName is {}", newFileName);
            } else {
                throw new RuntimeException("create new commitLogFile fail! fileName is " + newFileName);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new CommitLogFilePath(newFileName, newFilePath);
    }
}
