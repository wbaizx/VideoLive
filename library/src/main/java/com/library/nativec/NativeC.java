package com.library.nativec;

/**
 * Created by android1 on 2017/11/20.
 */

public class NativeC {
    static {
        System.loadLibrary("liveudpnative");
    }

    public static native byte[] rotateYUVDegree270AndMirror(byte[] data, int imageWidth, int imageHeight);

    /*
    计算CRC
     */
    public static int getCrcInt(byte[] buf, int offset, int length) {
        int remain = 0;
        byte val;
        for (int i = offset; i < offset + length; i++) {
            val = buf[i];
            for (int j = 0; j < 8; j++) {
                if (((val ^ remain) & 0x0001) != 0) {
                    remain ^= 0x0810;
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
