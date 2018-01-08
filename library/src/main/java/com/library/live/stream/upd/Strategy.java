package com.library.live.stream.upd;

import android.os.Handler;
import android.os.HandlerThread;

import com.library.live.stream.IsInBuffer;
import com.library.util.OtherUtil;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * 由于视频帧数量（一般20左右）小于音频帧数量（40左右），所以该缓存策略以视频流畅为基准
 */

public class Strategy {
    private ArrayBlockingQueue<FramesObject> videoframes = new ArrayBlockingQueue<>(OtherUtil.QueueNum);
    private ArrayBlockingQueue<FramesObject> voiceframes = new ArrayBlockingQueue<>(OtherUtil.QueueNum);
    private CachingStrategyCallback cachingStrategyCallback;

    private HandlerThread handlerVideoThread;
    private HandlerThread handlerVoiceThread;
    private Handler VideoHandler;
    private Handler VoiceHandler;

    private boolean isStart = false;//开始播放标志(调用star的开始标志)

    private boolean isVideoStart = false;//开始播放标志(真正播放的开始标志)

    private int VDtime;//视频帧绝对时间

    private int videomin = 6;//视频帧缓存达到播放条件

    private final int frameControltime = 10;//帧时间控制
    private final int voiceFrameControltime = 100;//音视频帧同步时间差错范围
    private boolean videoObservation = false;
    private boolean voiceObservation = false;
    private int VideoTimedifference = 0;
    private int VoiceTimedifference = 0;

    private IsInBuffer isInBuffer;

    public void setCachingStrategyCallback(CachingStrategyCallback cachingStrategyCallback) {
        this.cachingStrategyCallback = cachingStrategyCallback;
    }

    public void addVideo(int time, byte[] video) {
        OtherUtil.addQueue(videoframes, new FramesObject(time, video));
        if (videoObservation && videoframes.size() >= videomin) {//允许观察并且达到播放条件唤醒
            videoObservation = false;
            VideoHandler.post(videoRunnable);
        }
    }

    public void addVoice(int time, byte[] voice) {
        OtherUtil.addQueue(voiceframes, new FramesObject(time, voice));
        if (voiceObservation && isVideoStart) {//允许观察并且视频达到播放条件唤醒
            voiceObservation = false;
            VoiceHandler.post(voiceRunnable);
        }
    }

    private Runnable videoRunnable = new Runnable() {
        @Override
        public void run() {
            if (isStart) {
                if (isVideoStart && videoframes.size() > 0) {
                    FramesObject framesObject = videoframes.poll();

                    //计算下一帧播放延时时间
                    if (videoframes.size() > 0) {
                        //视频帧时间差控制在20-180之间
                        VideoTimedifference = Math.max(20, Math.min(200, videoframes.peek().getTime() - framesObject.getTime()));
                    } else {
                        VideoTimedifference = 0;
                    }
                    if (videoframes.size() > (videomin + 5)) {//帧缓存峰值为起始条件 +5，超过这个值则加快 frameControltime ms播放
                        while (videoframes.size() > (videomin + 35)) {//堆积数量超过峰值过多，丢弃部分
                            videoframes.poll();
                        }
                        VideoHandler.postDelayed(this, VideoTimedifference - frameControltime);
                    } else {
                        VideoHandler.postDelayed(this, VideoTimedifference);
                    }
                    VDtime = framesObject.getTime();
                    if (cachingStrategyCallback != null) {
                        cachingStrategyCallback.videoStrategy(framesObject.getData());
                    }
                } else {
                    if (videoframes.size() >= videomin) {
                        if (isInBuffer != null) {
                            //结束缓冲，回调给PlayerView
                            isInBuffer.isBuffer(false);
                        }
                        isVideoStart = true;
                        VideoHandler.post(this);
                    } else {
                        isVideoStart = false;
                        if (isInBuffer != null) {
                            //开始缓冲，回调给PlayerView
                            isInBuffer.isBuffer(true);
                        }
                        videoObservation = true;//允许视频观察
                    }
                }
            }
        }
    };

    private Runnable voiceRunnable = new Runnable() {
        @Override
        public void run() {
            if (isStart) {
                if (isVideoStart && voiceframes.size() > 0) {
                    FramesObject framesObject = voiceframes.poll();

                    //计算下一帧播放延时时间
                    if (voiceframes.size() > 0) {
                        //音频帧时间差控制在1-80之间
                        VoiceTimedifference = Math.max(1, Math.min(80, voiceframes.peek().getTime() - framesObject.getTime()));
                    } else {
                        VoiceTimedifference = 0;
                    }

                    if ((framesObject.getTime() - VDtime) > voiceFrameControltime) {//音频帧快了
                        VoiceHandler.postDelayed(this, VoiceTimedifference + frameControltime);
                    } else if ((VDtime - framesObject.getTime()) > voiceFrameControltime) {//音频帧慢了
                        if ((VDtime - framesObject.getTime()) > (voiceFrameControltime * 3)) {//音频帧过慢
                            while (voiceframes.size() > 0) {
                                if ((voiceframes.poll().getTime() - VDtime) < voiceFrameControltime + frameControltime) {
                                    break;
                                }
                            }
                        }
                        VoiceHandler.post(this);
                    } else {
                        VoiceHandler.postDelayed(this, VoiceTimedifference);
                    }
                    if (cachingStrategyCallback != null) {
                        cachingStrategyCallback.voiceStrategy(framesObject.getData());
                    }
                } else {
                    voiceObservation = true;
                }
            }
        }
    };

    public void start() {
        videoframes.clear();
        voiceframes.clear();
        isStart = true;

        handlerVideoThread = new HandlerThread("VideoPlayer");
        handlerVoiceThread = new HandlerThread("VoicePlayer");
        handlerVideoThread.start();
        handlerVoiceThread.start();
        VideoHandler = new Handler(handlerVideoThread.getLooper());
        VoiceHandler = new Handler(handlerVoiceThread.getLooper());

        VideoHandler.post(videoRunnable);
        VoiceHandler.post(voiceRunnable);
    }

    public void stop() {
        isStart = false;
        if (handlerVideoThread != null) {
            VideoHandler.removeCallbacksAndMessages(null);
            VoiceHandler.removeCallbacksAndMessages(null);
            handlerVideoThread.quitSafely();
            handlerVoiceThread.quitSafely();
        }
    }

    public void destroy() {
        isInBuffer = null;
        cachingStrategyCallback = null;
    }


    public void setVideoFrameCacheMin(int videoFrameCacheMin) {
        videomin = videoFrameCacheMin;
    }

    /*
     缓冲接口，用于PlayerView判断是否正在缓冲，根据需要决定是否需要使用
     */
    public void setIsInBuffer(IsInBuffer isInBuffer) {
        this.isInBuffer = isInBuffer;
    }


    private class FramesObject {
        private int time;
        private byte[] data;

        public FramesObject(int time, byte[] data) {
            this.time = time;
            this.data = data;
        }

        public int getTime() {
            return time;
        }

        public byte[] getData() {
            return data;
        }
    }
}
