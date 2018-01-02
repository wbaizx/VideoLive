package com.library.util;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by android1 on 2017/9/25.
 */

public class OtherUtil {
    public static final int QueueNum = 300;
    public static final int waitTime = 0;
    public static final int samplerate = 44100;

    public static int getTime() {
        return (int) (System.currentTimeMillis() % (long) 1000000000);
    }

    public static long getFPS() {
        return System.nanoTime() / 1000;
    }

    public static <T> void addQueue(ArrayBlockingQueue<T> queue, T t) {
        if (queue.size() >= QueueNum) {
            queue.poll();
        }
        queue.offer(t);
    }
}
