package com.library.talk.file;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.text.TextUtils;

import com.library.common.WriteCallback;
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

    private WriteCallback writeCallback;

    private int voiceTrackIndex;
    private long presentationTimeUsVE = 0;

    private boolean agreeWrite = false;
    private boolean isSendReady = true;
    private boolean isReady = false;

    private int frameNum = 0;

    public WriteMp3(String path) {
        if (!TextUtils.isEmpty(path) && !path.equals("")) {
            this.path = path;
            isHasPath = true;
        }
    }

    public void addTrack(MediaFormat mediaFormat) {
        voiceFormat = mediaFormat;
        setReady();
    }

    private void setReady() {
        isReady = true;
        if (writeCallback != null && isSendReady) {
            isSendReady = false;
            writeCallback.isReady();
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

    public void start() {
        if (isReady) {
            setPath();
            try {
                mMediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                voiceTrackIndex = mMediaMuxer.addTrack(voiceFormat);
                mMediaMuxer.start();
                presentationTimeUsVE = 0;
                frameNum = 0;
                agreeWrite = true;
                if (writeCallback != null) {
                    writeCallback.isStart();
                }
                mLog.log("app_WriteMp3", "文件录制启动");
            } catch (IOException e) {
                e.printStackTrace();
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
        mLog.log("app_WriteMp3", path);
    }

    public void stop() {
        if (agreeWrite) {
            agreeWrite = false;
            mMediaMuxer.release();
            mMediaMuxer = null;
            if (writeCallback != null) {
                writeCallback.isDestroy();
            }
            mLog.log("app_WriteMp3", "文件录制关闭");
            //文件过短删除
            if (frameNum < 10) {
                new File(path).delete();
                if (writeCallback != null) {
                    writeCallback.fileShort();
                }
            }
        }
    }

    public void destroy() {
        stop();
        writeCallback = null;
    }

    public void setWriteCallback(WriteCallback writeCallback) {
        this.writeCallback = writeCallback;
        if (isReady && isSendReady) {
            isSendReady = false;
            writeCallback.isReady();
        }
    }
}

