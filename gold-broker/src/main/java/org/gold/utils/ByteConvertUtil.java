package org.gold.utils;

/**
 * @author zhaoxun
 * @date 2025/10/21
 */
public class ByteConvertUtil {
    public static byte[] readInPos(byte[] source, int pos, int len) {
        byte[] result = new byte[len];
        for (int i = pos, j = 0; i < pos + len; i++) {
            result[j++] = source[i];
        }
        return result;
    }

    public static byte[] intToBytes(int value) {
        byte[] src = new byte[4];
        //32位-24位=8位
        //00000000001 0xFF 16
        src[3] = (byte) ((value >> 24) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    public static int bytesToInt(byte[] ary) {
        int value;
        value = (int) ((ary[0] & 0xFF)
                | ((ary[1] << 8) & 0xFF00)
                | ((ary[2] << 16) & 0xFF0000)
                | ((ary[3] << 24) & 0xFF000000));
        return value;
    }
}
