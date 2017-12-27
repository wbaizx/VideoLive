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
        for (int i = 0, dest; i < srclength; i++) {
            dest = (short) (((short) ((src[i * 2] & 0xff) | ((src[2 * i + 1] & 0xff) << 8))) >> 2);
            bytes[i * 2] = (byte) dest;
            bytes[i * 2 + 1] = (byte) (dest >> 8);
        }
        return bytes;
    }


    /**
     * PCM放大
     *
     * @param src      原数据
     * @param multiple 放大倍数
     * @return
     */
    public static byte[] increasePCM(byte[] src, int multiple) {
        if (multiple == 1) {
            return src;
        }
        byte[] bytes = new byte[src.length];
        int datalength = src.length >> 1;
        for (int i = 0, dest; i < datalength; i++) {
            dest = (short) ((src[i * 2] & 0xff) | (src[2 * i + 1] & 0xff) << 8) * multiple;
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

    /**
     * aac音频添加adts头
     */
    public static void addADTStoPacket(byte[] packet, int packetLen) {
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((2/*profile AAC LC*/ - 1) << 6) + (4/*freqIdx 44.1KHz*/ << 2) + (2/*chanCfgCPE*/ >> 2));
        packet[3] = (byte) (((2/*chanCfgCPE*/ & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }
}