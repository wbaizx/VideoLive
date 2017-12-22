package com.library.util;

/**
 * Created by android1 on 2017/12/21.
 */

public class VoiceUtil {
    /**
     * 算法降噪，但效果并不是很理想
     *
     * @param src       原数据
     * @param srclength 原数据长度
     * @return
     */
    public static byte[] noise(byte[] src, int srclength) {
        byte[] bytes = new byte[srclength];
        srclength = srclength >> 1;
        short dest;
        for (int i = 0; i < srclength; i++) {
            dest = (short) (((short) ((src[i * 2] & 0xff) | ((src[2 * i + 1] & 0xff) << 8))) >> 2);
            bytes[i * 2] = (byte) dest;
            bytes[i * 2 + 1] = (byte) (dest >> 8);
        }
        return bytes;
    }


    /**
     * PCM放大
     *
     * @param src       原数据
     * @param srclength 原数据长度
     * @param multiple  放大倍数
     * @return
     */
    public static byte[] increasePCM(byte[] src, int srclength, int multiple) {
        byte[] bytes = new byte[srclength];
        srclength = srclength >> 1;
        int dest;
        for (int i = 0; i < srclength; i++) {
            dest = (short) ((src[i * 2] & 0xff) | (src[2 * i + 1] & 0xff) << 8);
            dest = dest * multiple;
            //爆音处理
            if (dest > 32766) {
                dest = 32766;
            } else if (dest < -32767) {
                dest = -32767;
            }
            bytes[i * 2] = (byte) dest;
            bytes[i * 2 + 1] = (byte) (dest >> 8);
        }
        return bytes;
    }
}
