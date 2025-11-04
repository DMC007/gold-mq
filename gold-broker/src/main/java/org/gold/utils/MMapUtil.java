package org.gold.utils;

import io.netty.util.internal.PlatformDependent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author zhaoxun
 * @date 2025/10/20
 * @description 支持基于Java的MMap api访问文件能力（文件的读写的能力）
 * 支持指定的offset的文件映射（结束映射的offset-开始映射的offset=映射的内存体积）done!
 * 文件从指定的offset开始读取 done!
 * 文件从指定的offset开始写入 done!
 * 文件映射后的内存释放 todo
 */
public class MMapUtil {

    private static final Logger log = LogManager.getLogger(MMapUtil.class);

    private File file;
    private Long atomicOffset;
    private FileChannel fileChannel;
    private MappedByteBuffer mappedByteBuffer;

    /**
     * 指定offset做文件的映射
     *
     * @param filePath    文件路径
     * @param startOffset 开始映射的offset
     * @param mappedSize  映射的体积 (byte)
     */
    @SuppressWarnings({"resource"})
    public void loadFileToMMap(String filePath, int startOffset, int mappedSize) throws IOException {
        file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("filePath is " + filePath + " inValid");
        }
        fileChannel = new RandomAccessFile(file, "rw").getChannel();
        mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, startOffset, mappedSize);
//        fileChannel.close(); 这里关闭的channel后续再test的mappedByteBuffer使用无影响
    }

    /**
     * 支持从文件的指定offset开始读取内容
     *
     * @param readOffset 读取的offset
     * @param size       读取的体积
     * @return 读取的内容
     */
    public byte[] readContent(int readOffset, int size) {
        mappedByteBuffer.position(readOffset);
        byte[] content = new byte[size];
        int j = 0;
        for (int i = 0; i < size; i++) {
            //这里是从内存空间读取数据
            byte b = mappedByteBuffer.get(readOffset + i);
            content[j++] = b;
        }
        return content;
    }

    /**
     * 更高性能的一种写入api
     *
     * @param content 写入的内容
     */
    public void writeContent(byte[] content) {
        this.writeContent(content, false);
    }

    /**
     * 写入数据到磁盘当中
     *
     * @param content 写入的内容
     * @param force   是否强制刷盘
     */
    public void writeContent(byte[] content, boolean force) {
        //默认刷到page cache中，
        //如果需要强制刷盘，这里要兼容
        mappedByteBuffer.put(content);
        if (force) {
            //强制刷盘
            mappedByteBuffer.force();
        }
    }

    /**
     * 释放文件映射的内存
     */
    public void clean() {
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
            //直接调用netty封装好的工具类释放直接内存
            PlatformDependent.freeDirectBuffer(mappedByteBuffer);
        } catch (Exception e) {
            log.error("freeDirectBuffer error :{}", e.getMessage(), e);
        }
    }

//    public static void clean(MappedByteBuffer buffer) {
//        if (buffer == null) {
//            return;
//        }
//        try {
//            // 在 JDK 9 之后，sun.misc.Cleaner 被移到 jdk.internal.ref.Cleaner
//            // 而 MappedByteBuffer 的实现类仍然是 DirectByteBuffer
//            Method cleanerMethod = buffer.getClass().getMethod("cleaner");
//            cleanerMethod.setAccessible(true);
//            Object cleaner = cleanerMethod.invoke(buffer);
//
//            if (cleaner != null) {
//                Method cleanMethod = cleaner.getClass().getMethod("clean");
//                cleanMethod.invoke(cleaner);
//            }
//
//        } catch (Exception e) {
//            // 反射失败时，尝试通过强制 GC 回收作为兜底方案
//            System.gc();
//        }
//    }
    //MappedByteBuffer是没有close方法的，即使它的FileChannel被close了，MappedByteBuffer仍然处于打开状态，只有JVM进行垃圾回收的时候才会被关闭。而这个时间是不确定的。
}
