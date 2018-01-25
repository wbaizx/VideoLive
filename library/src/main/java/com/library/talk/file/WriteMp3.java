package com.library.talk.file;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.text.TextUtils;

import com.library.common.WriteFileCallback;
import com.library.util.OtherUtil;
import com.library.util.mLog;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by android1 on 2017/12/28.
 */

public class WriteMp3 {
    public static final int RECODE_STATUS_START = 0;
    public static final int RECODE_STATUS_STOP = 1;
    public static final int RECODE_STATUS_READY = 2;
    private int RECODE_STATUS = RECODE_STATUS_STOP;

    private MediaMuxer mMediaMuxer = null;

    private String dirpath = Environment.getExternalStorageDirectory().getPath() + File.separator + "VideoTalk";
    private String path = null;
    private MediaFormat voiceFormat = null;

    private int voiceTrackIndex;
    private long presentationTimeUsVE = 0;

    private boolean isShouldStart = false;

    private int frameNum = 0;
    private final Object lock = new Object();
    private WriteFileCallback writeFileCallback = new WriteFileCallback() {
        @Override
        public void success(String name) {

        }

        @Override
        public void failure(String err) {

        }
    };

    public WriteMp3(String dirpath) {
        if (!TextUtils.isEmpty(dirpath) && !dirpath.equals("")) {
            this.dirpath = dirpath;
        }
    }

    public void addTrack(MediaFormat mediaFormat) {
        voiceFormat = mediaFormat;
        if (isShouldStart) {
            start();
        }
    }

    public void start() {
        RECODE_STATUS = RECODE_STATUS_READY;
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
                    RECODE_STATUS = RECODE_STATUS_START;
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
        if (RECODE_STATUS == RECODE_STATUS_START) {
            if (bufferInfo.presentationTimeUs > presentationTimeUsVE) {//容错
                presentationTimeUsVE = bufferInfo.presentationTimeUs;
                mMediaMuxer.writeSampleData(voiceTrackIndex, outputBuffer, bufferInfo);
                frameNum++;
            }
        }
    }

    private void setPath() {
        OtherUtil.CreateDirFile(dirpath);
        path = dirpath + File.separator + System.currentTimeMillis() + ".mp3";
    }

    public void stop() {
        synchronized (lock) {
            if (RECODE_STATUS == RECODE_STATUS_START) {
                boolean iscatch = false;
                try {
                    mMediaMuxer.release();
                    if (frameNum >= 20) {
                        writeFileCallback.success(path);
                    }
                    mLog.log("app_WriteMp3", "文件录制关闭");
                } catch (Exception e) {
                    iscatch = true;
                } finally {
                    mMediaMuxer = null;
                    voiceFormat = null;
                    //文件过短或异常，删除文件
                    File file = new File(path);
                    if (file.exists()) {
                        if (iscatch) {
                            file.delete();
                            writeFileCallback.failure("文件录制失败");
                        } else if (frameNum < 20) {
                            file.delete();
                            writeFileCallback.failure("文件过短");
                        }
                    } else {
                        writeFileCallback.failure("文件录制失败");
                    }
                }
            } else if (RECODE_STATUS == RECODE_STATUS_READY) {
                isShouldStart = false;
                writeFileCallback.failure("文件录制被取消");
            }
            RECODE_STATUS = RECODE_STATUS_STOP;
        }
    }

    public void destroy() {
        stop();
        writeFileCallback = null;
    }

    public int getRecodeStatus() {
        return RECODE_STATUS;
    }

    public void setWriteFileCallback(WriteFileCallback writeFileCallback) {
        this.writeFileCallback = writeFileCallback;
    }
}

