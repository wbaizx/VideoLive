package com.library.util;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by android1 on 2017/9/25.
 */

public class OtherUtil {
    public static final int QueueNum = 300;
    public static final int waitTime = 0;

    public static int getTime() {
        return (int) (System.currentTimeMillis() % (long) 1000000000);
    }

    public static long getFPS() {
        return System.nanoTime() / 1000;
    }

    public static void sleepLongTime() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void sleepShortTime() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void addQueue(ArrayBlockingQueue queue, Object obj) {
        if (queue.size() >= QueueNum - 1) {
            queue.poll();
        }
        queue.add(obj);
    }

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
