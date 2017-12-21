package com.library.util;

/**
 * Created by android1 on 2017/12/21.
 */

public class NoiseUtil {
    public static byte[] noise(byte[] src, int srclength) {
        byte[] bytes = new byte[srclength];
        srclength = srclength >> 1;
        short[] dest = new short[srclength];
        for (int i = 0; i < srclength; i++) {
            dest[i] = (short) (((short) ((src[i * 2] & 0xff) | ((src[2 * i + 1] & 0xff) << 8))) >> 2);
            bytes[i * 2] = (byte) (dest[i]);
            bytes[i * 2 + 1] = (byte) (dest[i] >> 8);
        }
        return bytes;
    }
}
