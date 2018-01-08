package com.library.live.file;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.text.TextUtils;

import com.library.util.mLog;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by android1 on 2017/10/20.
 */

public class WriteMp4 {
    private MediaMuxer mMediaMuxer = null;
    public static final int video = 0;
    public static final int voice = 1;

    private String dirpath = Environment.getExternalStorageDirectory().getPath() + File.separator + "VideoLive";
    private String path = null;
    private MediaFormat videoFormat = null;
    private MediaFormat voiceFormat = null;

    private int videoTrackIndex;
    private int voiceTrackIndex;
    private long presentationTimeUsVD = 0;
    private long presentationTimeUsVE = 0;

    private boolean agreeWrite = false;
    private boolean isShouldStart = false;
    private int frameNum = 0;
    private final Object lock = new Object();

    public WriteMp4(String dirpath) {
        if (!TextUtils.isEmpty(dirpath) && !dirpath.equals("")) {
            this.dirpath = dirpath;
        }
        File dirfile = new File(dirpath);
        if (!dirfile.exists()) {
            dirfile.mkdirs();
        }
    }

    public void addTrack(MediaFormat mediaFormat, int flag) {
        if (flag == video) {
            videoFormat = mediaFormat;
        } else if (flag == voice) {
            voiceFormat = mediaFormat;
        }
        if (videoFormat != null && voiceFormat != null) {
            if (isShouldStart) {
                start();
            }
        }
    }


    public void start() {
        synchronized (lock) {
            if (voiceFormat != null && videoFormat != null && mMediaMuxer == null) {
                isShouldStart = false;
                setPath();
                try {
                    mMediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    videoTrackIndex = mMediaMuxer.addTrack(videoFormat);
                    voiceTrackIndex = mMediaMuxer.addTrack(voiceFormat);
                    mMediaMuxer.start();
                    presentationTimeUsVE = 0;
                    presentationTimeUsVD = 0;
                    frameNum = 0;
                    agreeWrite = true;
                    mLog.log("app_WriteMp4", "文件录制启动");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                isShouldStart = true;
            }
        }
    }

    public void write(int flag, ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
        if (agreeWrite) {
            if (flag == video) {
                if (bufferInfo.presentationTimeUs > presentationTimeUsVD) {//容错
                    presentationTimeUsVD = bufferInfo.presentationTimeUs;
                    if (frameNum == 0) {//视频帧第一帧必须为关键帧
                        if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                            mMediaMuxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo);
                            frameNum++;
                        }
                    } else {
                        mMediaMuxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo);
                        frameNum++;
                    }
                }
            } else if (flag == voice) {
                if (bufferInfo.presentationTimeUs > presentationTimeUsVE) {//容错
                    presentationTimeUsVE = bufferInfo.presentationTimeUs;
                    mMediaMuxer.writeSampleData(voiceTrackIndex, outputBuffer, bufferInfo);
                }
            }
        }
    }

    private void setPath() {
        path = dirpath + File.separator + System.currentTimeMillis() + ".mp4";
    }

    public void stop() {
        synchronized (lock) {
            if (agreeWrite) {
                agreeWrite = false;
                try {
                    mMediaMuxer.release();
                    mLog.log("app_WriteMp4", "文件录制关闭");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mMediaMuxer = null;
                    //文件过短或异常，删除文件
                    File file = new File(path);
                    if (frameNum < 20 && file.exists()) {
                        file.delete();
                    }
                }
            } else {
                isShouldStart = false;
            }
        }
    }

    public void destroy() {
        stop();
    }
}
