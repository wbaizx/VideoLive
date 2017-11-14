package com.library.util.data;

/**
 * Created by android1 on 2017/9/25.
 */

public class Value {
    public static final int QueueNum = 200;
    public static final int sleepTime = 5;
    public static final int waitTime = 0;

    public static int getTime() {
        return (int) (System.currentTimeMillis() % (long) 1000000000);
    }

    public static long getFPS() {
        return System.nanoTime() / 1000;
    }
}
