package com.library.util.data;

/**
 * Created by android1 on 2017/11/3.
 */

public class Crc {
    /*
    检验数据CRC是否正确
     */
    public static boolean isCrcRight(byte[] bytes, int crc) {
        return getCrcInt(bytes) == crc;
    }

    /*
    计算CRC
     */
    public static int getCrcInt(byte[] buf) {
        int MASK = 0x0001, CRCSEED = 0x0810;
        int remain = 0;
        byte val;
        for (int i = 0; i < buf.length; i++) {
            val = buf[i];
            for (int j = 0; j < 8; j++) {
                if (((val ^ remain) & MASK) != 0) {
                    remain ^= CRCSEED;
                    remain >>= 1;
                    remain |= 0x8000;
                } else {
                    remain >>= 1;
                }
                val >>= 1;
            }
        }
        return remain;
    }
}
