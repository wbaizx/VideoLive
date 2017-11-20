package com.library.nativec;

/**
 * Created by android1 on 2017/11/20.
 */

public class NativeC {
    static {
        System.loadLibrary("liveudpnative");
    }

    public static native int getCrcInt(byte[] buf, int offset, int length);
}
