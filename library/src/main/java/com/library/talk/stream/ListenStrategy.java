package com.library.talk.stream;

import android.os.Handler;
import android.os.HandlerThread;

import com.library.util.OtherUtil;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by android1 on 2017/12/25.
 */

public class ListenStrategy {
    private ListenCachingStrategyCallback listenCachingStrategyCallback;
    private ArrayBlockingQueue<FramesObject> voiceframes = new ArrayBlockingQueue<>(OtherUtil.QueueNum);
    private HandlerThread handlerVoiceThread;
    private Handler VoiceHandler;
    private boolean isStart = false;//开始播放标志(调用star的开始标志)
    private boolean isVoiceStart = false;//开始播放标志(真正播放的开始标志)
    private int voicemin = 5;//音频帧缓存达到播放条件
    private boolean voiceObservation = false;

    public void addVoice(int timedifference, int time, byte[] voice) {
        OtherUtil.addQueue(voiceframes, new FramesObject(
                Math.max(1, Math.min(80, timedifference)),
                time,
                voice));//音频帧时间差控制在1-80之间
        if (voiceObservation && voiceframes.size() >= voicemin) {//允许观察并且达到播放条件唤醒
            voiceObservation = false;
            VoiceHandler.post(voiceRunnable);
        }
    }


    public void setListenCachingStrategyCallback(ListenCachingStrategyCallback listenCachingStrategyCallback) {
        this.listenCachingStrategyCallback = listenCachingStrategyCallback;
    }

    public void start() {
        voiceframes.clear();
        isStart = true;

        handlerVoiceThread = new HandlerThread("ListenCaching");
        handlerVoiceThread.start();
        VoiceHandler = new Handler(handlerVoiceThread.getLooper());

        VoiceHandler.post(voiceRunnable);
    }

    private Runnable voiceRunnable = new Runnable() {
        @Override
        public void run() {
            if (isStart) {
                if (isVoiceStart && voiceframes.size() > 0) {
                    FramesObject framesObject = voiceframes.poll();
                    if (voiceframes.size() > (voicemin + 10)) {//帧缓存峰值为起始条件 +5，超过这个值则加快播放
                        while (voiceframes.size() > (voicemin + 40)) {//堆积数量超过峰值过多，丢弃部分
                            voiceframes.poll();
                        }
                        VoiceHandler.post(this);
                    } else {
                        VoiceHandler.postDelayed(this, framesObject.getTimedifference());
                    }
                    if (listenCachingStrategyCallback != null) {
                        listenCachingStrategyCallback.voiceStrategy(framesObject.getData());
                    }
                } else {
                    if (voiceframes.size() >= voicemin) {
                        isVoiceStart = true;
                        VoiceHandler.post(this);
                    } else {
                        isVoiceStart = false;
                        voiceObservation = true;
                    }
                }
            }
        }
    };

    public void stop() {
        isStart = false;
        if (handlerVoiceThread != null) {
            VoiceHandler.removeCallbacksAndMessages(null);
            handlerVoiceThread.quitSafely();
        }
    }

    public void destroy() {
        listenCachingStrategyCallback = null;
    }

    public void setVoiceFrameCacheMin(int voiceFrameCacheMin) {
        this.voicemin = voiceFrameCacheMin;
    }

    private class FramesObject {
        private int timedifference;
        private int time;
        private byte[] data;

        public FramesObject(int timedifference, int time, byte[] data) {
            this.time = time;
            this.data = data;
            this.timedifference = timedifference;
        }

        public int getTimedifference() {
            return timedifference;
        }

        public int getTime() {
            return time;
        }

        public byte[] getData() {
            return data;
        }
    }
}
