package com.library.vd;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.library.stream.BaseSend;
import com.library.util.WriteMp4;
import com.library.util.data.ByteTurn;
import com.library.util.data.Value;
import com.library.util.image.ImageUtil;

import java.io.IOException;
import java.nio.ByteBuffer;


public class VDEncoder {
    public static final String H264 = MediaFormat.MIMETYPE_VIDEO_AVC;
    public static final String H265 = MediaFormat.MIMETYPE_VIDEO_HEVC;
    //解码格式
    private String MIME_TYPE = H264;
    private int TIMEOUT_USEC = 12000;
    private MediaCodec mediaCodec;
    private BaseSend baseSend;

    //编码参数
    private int width;
    private int height;
    private int framerate;
    private byte[] information;
    private boolean isRuning = false;

    //文件录入类
    private WriteMp4 writeMp4;

    public VDEncoder(int width, int height, int framerate, int bitrate, WriteMp4 writeMp4, String codetype, BaseSend baseSend) {
        this.width = width;
        this.height = height;
        this.framerate = framerate;
        this.writeMp4 = writeMp4;
        //UPD实例
        this.baseSend = baseSend;
        MIME_TYPE = codetype;
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        try {
            mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
    }

    public void destroy() {
        isRuning = false;
        mediaCodec.stop();
        mediaCodec.release();
        mediaCodec = null;
    }

    public void StartEncoderThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                isRuning = true;
                byte[] input;
                ByteBuffer[] inputBuffers;
                ByteBuffer[] outputBuffers;
                byte[] outData;
                ByteBuffer outputBuffer;
                int outputBufferIndex;
                while (isRuning) {
                    if (Publish.YUVQueue.size() > 0) {
                        input = new byte[width * height * 3 / 2];
                        ImageUtil.NV21ToNV12(Publish.YUVQueue.poll(), input, width, height);
                        if (input != null) {
                            try {
                                inputBuffers = mediaCodec.getInputBuffers();
                                outputBuffers = mediaCodec.getOutputBuffers();
                                int inputBufferIndex = mediaCodec.dequeueInputBuffer(50);
                                if (inputBufferIndex >= 0) {
                                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                                    inputBuffer.clear();
                                    inputBuffer.put(input);
                                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, Value.getFPS(), 0);
                                }

                                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);

                                if (MediaCodec.INFO_OUTPUT_FORMAT_CHANGED == outputBufferIndex) {
                                    writeMp4.addTrack(mediaCodec.getOutputFormat(), WriteMp4.video);
                                }
                                while (outputBufferIndex >= 0) {
                                    outputBuffer = outputBuffers[outputBufferIndex];
                                    outData = new byte[bufferInfo.size];

                                    //写文件
                                    writeMp4.write(WriteMp4.video, outputBuffer, bufferInfo);

                                    outputBuffer.get(outData);
                                    if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                                        //sps pps信息
                                        information = outData;
                                        Log.d("sps_pps", ByteTurn.byte_to_16(information));

                                    } else if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                                        //关键帧
                                        byte[] keyframe = ByteTurn.byte_add(information, outData);
                                        //添加将要发送的视频数据
                                        Log.d("frame_length", "帧长度为  --   " + keyframe.length);
                                        baseSend.addVideo(keyframe);
                                    } else {
                                        //普通帧
                                        //添加将要发送的视频数据
                                        Log.d("frame_length", "帧长度为  --   " + outData.length);
                                        baseSend.addVideo(outData);
                                    }
                                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                                    outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                                }
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }
                        }
                    } else {
                        try {
                            Thread.sleep(Value.sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }
}