package com.library.talk.file;

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
 * Created by android1 on 2017/12/28.
 */

public class WriteMp3 {
    private MediaMuxer mMediaMuxer = null;

    private final String dirpath = Environment.getExternalStorageDirectory().getPath() + File.separator + "VideoTalk";
    private String path = null;
    private boolean isHasPath = false;
    private MediaFormat voiceFormat = null;

    private int voiceTrackIndex;
    private long presentationTimeUsVE = 0;

    private boolean agreeWrite = false;
    private boolean isShouldStart = false;

    private int frameNum = 0;
    private final Object lock = new Object();

    public WriteMp3(String path) {
        if (!TextUtils.isEmpty(path) && !path.equals("")) {
            this.path = path;
            isHasPath = true;
        }
    }

    public void addTrack(MediaFormat mediaFormat) {
        voiceFormat = mediaFormat;
        if (isShouldStart) {
            start();
        }
    }

    public void start() {
        synchronized (lock) {
            if (voiceFormat != null && mMediaMuxer == null) {
                isShouldStart = false;
                setPath();
                try {
                    mMediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    voiceTrackIndex = mMediaMuxer.addTrack(voiceFormat);
                    mMediaMuxer.start();
                    presentationTimeUsVE = 0;
                    frameNum = 0;
                    agreeWrite = true;
                    mLog.log("app_WriteMp3", "文件录制启动");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                isShouldStart = true;
            }
        }
    }

    public void write(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
        if (agreeWrite) {
            if (bufferInfo.presentationTimeUs > presentationTimeUsVE) {//容错
                presentationTimeUsVE = bufferInfo.presentationTimeUs;
                mMediaMuxer.writeSampleData(voiceTrackIndex, outputBuffer, bufferInfo);
                frameNum++;
            }
        }
    }

    private void setPath() {
        if (isHasPath) {
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        } else {
            File dirfile = new File(dirpath);
            if (!dirfile.exists()) {
                dirfile.mkdirs();
            }
            path = dirpath + File.separator + System.currentTimeMillis() + ".mp3";
        }
    }

    public void stop() {
        synchronized (lock) {
            if (agreeWrite) {
                agreeWrite = false;
                try {
                    mMediaMuxer.release();
                    mLog.log("app_WriteMp3", "文件录制关闭");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mMediaMuxer = null;
                    //文件过短或异常，删除文件
                    File file = new File(path);
                    if (frameNum < 10 && file.exists()) {
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

