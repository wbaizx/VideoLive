package com.library.util;

/**
 * Created by android1 on 2017/9/25.
 */

public class ByteUtil {
    //short转bytes
    public static byte[] short_to_byte(short num) {
        byte[] result = new byte[2];
        result[0] = (byte) ((num >> 8) & 0xff);
        result[1] = (byte) ((num >> 0) & 0xff);
        return result;
    }

    //bytes转short
    public static short byte_to_short(byte b1, byte b2) {
        return (short) ((b1 & 0xff) << 8 | (b2 & 0xff));
    }

    //int转bytes(高位在前)
    public static byte[] int_to_byte(int value) {
        byte[] src = new byte[4];
        src[0] = (byte) ((value >> 24) & 0xFF);
        src[1] = (byte) ((value >> 16) & 0xFF);
        src[2] = (byte) ((value >> 8) & 0xFF);
        src[3] = (byte) (value & 0xFF);
        return src;
    }


    //bytes转int(高位在前)
    public static int byte_to_int(byte b1, byte b2, byte b3, byte b4) {
        return (((b1 & 0xFF) << 24) | ((b2 & 0xFF) << 16) | ((b3 & 0xFF) << 8) | (b4 & 0xFF));
    }

    // byte拼接
    public static byte[] byte_add(byte[] a, byte[] b) {
        byte[] bytes = new byte[a.length + b.length];
        System.arraycopy(a, 0, bytes, 0, a.length);
        System.arraycopy(b, 0, bytes, a.length, b.length);
        return bytes;
    }

    //将数组以16进制打印
    public static String byte_to_16(byte[] buffer) {
        StringBuffer h = new StringBuffer("");
        String temp;
        for (byte b : buffer) {
            temp = Integer.toHexString(b & 0xFF);
            if (temp.length() == 1) {
                temp = "0" + temp;
            }
            h.append(" " + temp);
        }
        return h.toString();
    }
}
