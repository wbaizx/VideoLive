package com.library.file;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;

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

    private final String dirpath = Environment.getExternalStorageDirectory().getPath() + File.separator + "VideoLive";
    private String path = null;
    private MediaFormat videoFormat = null;
    private MediaFormat voiceFormat = null;

    private int videoTrackIndex;
    private int voiceTrackIndex;
    private long presentationTimeUsVD = 0;
    private long presentationTimeUsVE = 0;
    private boolean isStart = false;

    public WriteMp4(String path) {
        this.path = path;
    }

    public void addTrack(MediaFormat mediaFormat, int flag) {
        if (flag == video) {
            videoFormat = mediaFormat;
        } else if (flag == voice) {
            voiceFormat = mediaFormat;
        }
    }

    public void write(int flag, ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
        if (isStart) {
            if (flag == video) {
                if (bufferInfo.presentationTimeUs > presentationTimeUsVD) {//容错
                    presentationTimeUsVD = bufferInfo.presentationTimeUs;

                    mMediaMuxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo);
//                    Log.d("app_WriteMp4", "写了视频数据----------------");
                }
            } else if (flag == voice) {
                if (bufferInfo.presentationTimeUs > presentationTimeUsVE) {//容错
                    presentationTimeUsVE = bufferInfo.presentationTimeUs;

                    mMediaMuxer.writeSampleData(voiceTrackIndex, outputBuffer, bufferInfo);
//                    Log.d("app_WriteMp4", "写了音频数据----");
                }
            }
        }
    }

    public void star() {
        if (videoFormat != null && voiceFormat != null) {
            if (path == null) {
                File dirfile = new File(dirpath);
                if (!dirfile.exists()) {
                    dirfile.mkdirs();
                }
                path = dirpath + File.separator + System.currentTimeMillis() + ".mp4";
            } else {
                File file = new File(path);
                if (file.exists()) {
                    file.delete();
                }
            }
            try {
                mMediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                videoTrackIndex = mMediaMuxer.addTrack(videoFormat);
                voiceTrackIndex = mMediaMuxer.addTrack(voiceFormat);
                mMediaMuxer.start();
                presentationTimeUsVE = 0;
                presentationTimeUsVD = 0;
//                Log.d("app_WriteMp4", "启动");
            } catch (IOException e) {
                e.printStackTrace();
            }
            isStart = true;
        }
    }

    public void destroy() {
        if (isStart) {
            //从开启到关闭中间必须写入一段数据，否则崩溃，开启->关闭不能连续调用
            isStart = false;
            mMediaMuxer.release();
            mMediaMuxer = null;
//            Log.d("app_WriteMp4", "关闭");
        }
    }
}
