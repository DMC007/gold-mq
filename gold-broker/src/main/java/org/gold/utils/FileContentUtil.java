package org.gold.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * @author zhaoxun
 * @date 2025/10/21
 * @description 简化版本的文件读写工具
 */
public class FileContentUtil {

    private static final Logger log = LogManager.getLogger(FileContentUtil.class);

    public static String readFile(String filePath) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder stringBuilder = new StringBuilder();
            while (bufferedReader.ready()) {
                stringBuilder.append(bufferedReader.readLine());
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            log.error("readFile error:{}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static void writeFile(String filePath, String content) {
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write(content);
            fileWriter.flush();
        } catch (Exception e) {
            log.error("writeFile error:{}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
