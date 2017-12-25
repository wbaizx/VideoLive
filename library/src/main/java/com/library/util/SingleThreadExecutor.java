package com.library.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 自定义单例线程池
 */

public class SingleThreadExecutor {
    private BlockingQueue<Runnable> queue;
    private ThreadPoolExecutor threadPoolExecutor;

    public SingleThreadExecutor() {
        queue = new ArrayBlockingQueue<>(2);
        threadPoolExecutor = new ThreadPoolExecutor(
                1,
                1,
                0,
                TimeUnit.SECONDS,
                queue,
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.DiscardPolicy());
    }

    public void execute(Runnable runnable) {
        threadPoolExecutor.execute(runnable);
    }

    public void shutdownNow() {
        threadPoolExecutor.shutdownNow();
    }

    public int getQueueNum() {
        return queue.size();
    }

    public void clearQueue() {
        queue.clear();
    }
}
