package com.library.live.vd;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Size;

import com.library.live.file.WriteMp4;
import com.library.util.ImagUtil;
import com.library.util.OtherUtil;
import com.library.util.SingleThreadExecutor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by android1 on 2017/12/2.
 */

public class RecordEncoderVD {
    //编码参数
    private boolean isRuning = false;
    private MediaCodec mediaCodec;
    private WriteMp4 writeMp4;
    private ArrayBlockingQueue<byte[]> YUVQueue = new ArrayBlockingQueue<>(OtherUtil.QueueNum);
    private int width;
    private int height;
    private int COLOR_FORMAT;
    private SingleThreadExecutor singleThreadExecutor;
    private MediaFormat mediaFormat;

    public RecordEncoderVD(Size csize, int framerate, int collectionBitrate, WriteMp4 writeMp4, String codetype) {
        this.writeMp4 = writeMp4;
        //由于图片旋转过，所以高度宽度需要对调
        width = csize.getHeight();
        height = csize.getWidth();

        try {
            mediaCodec = MediaCodec.createEncoderByType(codetype);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaFormat = MediaFormat.createVideoFormat(codetype, width, height);
        COLOR_FORMAT = ImagUtil.showSupportedColorFormat(mediaCodec, codetype);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FORMAT);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, collectionBitrate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        singleThreadExecutor = new SingleThreadExecutor();
    }

    /*
    视频数据队列，等待编码，视频数据处理比较耗时，所以存放队列另起线程等待编码
     */
    public void addFrame(byte[] bytes) {
        if (isRuning) {
            OtherUtil.addQueue(YUVQueue, bytes);
        }
    }

    public void start() {
        YUVQueue.clear();
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
        isRuning = true;
        StartEncoderThread();
    }

    public void stop() {
        isRuning = false;
        mediaCodec.stop();
    }

    public void StartEncoderThread() {
        singleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                byte[] input = new byte[width * height * 3 / 2];
                byte[] take;
                int outputBufferIndex;
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                while (isRuning) {
                    try {
                        take = YUVQueue.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }

                    if (COLOR_FORMAT == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
                        input = take;
                    } else {
                        ImagUtil.yuvI420ToNV12(take, input, width, height);
                    }
                    try {
                        int inputBufferIndex = mediaCodec.dequeueInputBuffer(OtherUtil.waitTime);
                        if (inputBufferIndex >= 0) {
                            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                            inputBuffer.clear();
                            inputBuffer.put(input);
                            mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, OtherUtil.getFPS(), 0);
                        }

                        outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, OtherUtil.waitTime);

                        if (MediaCodec.INFO_OUTPUT_FORMAT_CHANGED == outputBufferIndex) {
                            writeMp4.addTrack(mediaCodec.getOutputFormat(), WriteMp4.video);
                        }
                        while (outputBufferIndex >= 0) {
                            //写文件
                            writeMp4.write(WriteMp4.video, mediaCodec.getOutputBuffer(outputBufferIndex), bufferInfo);

                            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, OtherUtil.waitTime);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void destroy() {
        stop();
        mediaCodec.release();
        mediaCodec = null;
        singleThreadExecutor.shutdownNow();
    }
}
