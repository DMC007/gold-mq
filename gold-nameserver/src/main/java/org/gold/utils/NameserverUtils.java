package org.gold.utils;


import org.gold.cache.CommonCache;

/**
 * @author zhaoxun
 * @date 2025/11/4
 * @description 名称服务工具类
 */
public class NameserverUtils {

    public static boolean isVerify(String user, String password) {
        String rightUser = CommonCache.getNameserverProperties().getNameserverUser();
        String rightPassword = CommonCache.getNameserverProperties().getNameserverPwd();
        return rightUser.equals(user) && rightPassword.equals(password);
    }
}
