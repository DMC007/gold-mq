package org.gold.utils;

import java.util.List;

/**
 * @author zhaoxun
 * @date 2025/11/10
 */
public class AssertUtils {
    public static void isNotBlank(String val, String msg) {
        if (val == null || val.trim().isEmpty()) {
            throw new RuntimeException(msg);
        }
    }

    public static void isNotEmpty(List<?> list, String msg) {
        if (list == null || list.isEmpty()) {
            throw new RuntimeException(msg);
        }
    }


    public static void isNotNull(Object val, String msg) {
        if (val == null) {
            throw new RuntimeException(msg);
        }
    }

    public static void isTrue(Boolean condition, String msg) {
        if (!condition) {
            throw new RuntimeException(msg);
        }
    }
}
