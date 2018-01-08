package com.library.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by android1 on 2017/9/25.
 */

public class OtherUtil {
    private static final double a = (double) 3 / 4;
    private static final double b = (double) 2 / 3;
    private static final double c = (double) 9 / 16;
    private static final double d = (double) 1 / 2;
    private static final double e = (double) 5 / 9;
    private static final double f = (double) 3 / 5;
    private static final double g = 1;
    private static final double h = (double) 9 / 11;

    public static final int QueueNum = 300;

    public static final int waitTime = 0;
    public static final int samplerate = 44100;

    public static byte setWeitht(double weitht) {
        if (weitht == a) {
            return (byte) 'a';
        } else if (weitht == b) {
            return (byte) 'b';
        } else if (weitht == c) {
            return (byte) 'c';
        } else if (weitht == d) {
            return (byte) 'd';
        } else if (weitht == e) {
            return (byte) 'e';
        } else if (weitht == f) {
            return (byte) 'f';
        } else if (weitht == g) {
            return (byte) 'g';
        } else if (weitht == h) {
            return (byte) 'h';
        }
        return (byte) 'a';
    }

    public static double getWeight(byte weitht) {
        double v;
        switch (weitht) {
            case (byte) 'a':
                v = a;
                break;
            case (byte) 'b':
                v = b;
                break;
            case (byte) 'c':
                v = c;
                break;
            case (byte) 'd':
                v = d;
                break;
            case (byte) 'e':
                v = e;
                break;
            case (byte) 'f':
                v = f;
                break;
            case (byte) 'g':
                v = g;
                break;
            case (byte) 'h':
                v = h;
                break;
            default:
                v = a;
                break;
        }
        return v;
    }

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

    public static <T extends Closeable> void close(T t) {
        if (t != null) {
            try {
                t.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            } finally {
                t = null;
            }
        }
    }
}
