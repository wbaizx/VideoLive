package com.library.stream.upd;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.library.util.OtherUtil;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by android1 on 2017/11/16.
 */

public class Strategy {
    private CachingStrategyCallback cachingStrategyCallback;

    private ArrayBlockingQueue<FramesObject> videoframes = new ArrayBlockingQueue<>(OtherUtil.QueueNum);
    private ArrayBlockingQueue<FramesObject> voiceframes = new ArrayBlockingQueue<>(OtherUtil.QueueNum);

    private int VDtime;
    private int VCtime;

    private HandlerThread handlerVideoThread;
    private HandlerThread handlerVoiceThread;
    private Handler VideoHandler;
    private Handler VoiceHandler;

    private boolean iscode = false;
    private boolean isVideocode = false;

    public Strategy() {
        handlerVideoThread = new HandlerThread("VideoPlayer");
        handlerVoiceThread = new HandlerThread("VoicePlayer");
        handlerVideoThread.start();
        handlerVoiceThread.start();
        VideoHandler = new Handler(handlerVideoThread.getLooper());
        VoiceHandler = new Handler(handlerVoiceThread.getLooper());
    }

    public void setCachingStrategyCallback(CachingStrategyCallback cachingStrategyCallback) {
        this.cachingStrategyCallback = cachingStrategyCallback;
    }

    public void addVideo(int timedifference, int time, byte[] video) {
        videoframes.add(new FramesObject(Math.min(150, timedifference), time, video));//视频帧时间差最大为150（有多）
    }

    public void addVoice(int timedifference, int time, byte[] voice) {
//        voiceframes.add(new FramesObject(Math.min(40, timedifference), time, voice));//音频帧时间差最大为40（有多）
    }

    private Runnable videoRunnable = new Runnable() {
        @Override
        public void run() {
            if (iscode) {
                if (isVideocode && videoframes.size() > 0) {
                    FramesObject framesObject = videoframes.poll();
                    if (videoframes.size() > 30) {
                        //如果缓存过多则手动控制一下
                        VideoHandler.postDelayed(this, framesObject.getTimedifference() - 5);
                    } else {
                        VideoHandler.postDelayed(this, framesObject.getTimedifference());
                    }
                    Log.d("wegerfr", "---" + videoframes.size());

                    VDtime = framesObject.getTime();
                    cachingStrategyCallback.videoStrategy(framesObject.getData());
                } else {
                    isVideocode = false;
                    if (videoframes.size() > 20) {
                        isVideocode = true;
                        VideoHandler.post(this);
                    } else {
                        VideoHandler.postDelayed(this, 200);
                    }
                }
            }
        }
    };

    private Runnable voiceRunnable = new Runnable() {
        @Override
        public void run() {
//            if (iscode) {
//                FramesObject framesObject = voiceframes.poll();
//                VCtime = framesObject.getTime();
//                if (voiceframes.size() > 200) {
//
//                }
//            }
        }
    };

    public void star() {
        videoframes.clear();
        voiceframes.clear();
        iscode = true;
        VideoHandler.post(videoRunnable);
        VoiceHandler.post(voiceRunnable);
    }

    public void stop() {
        iscode = false;
    }

    public void destroy() {
        stop();
        handlerVideoThread.quitSafely();
        handlerVoiceThread.quitSafely();
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
