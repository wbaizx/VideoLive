package com.library.vc;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.library.stream.BaseRecive;
import com.library.util.WriteMp4;
import com.library.util.data.Value;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by android1 on 2017/9/23.
 */

public class VCDecoder {
    private final String AAC_MIME = MediaFormat.MIMETYPE_AUDIO_AAC;

    private MediaCodec mDecoder;
    private BaseRecive baseRecive;
    private WriteMp4 writeMp4;
    private boolean isdecoder = false;
    private VoicePlayer voicePlayer;

    public VCDecoder(int samplerate, BaseRecive baseRecive, WriteMp4 writeMp4) {
        this.baseRecive = baseRecive;
        this.writeMp4 = writeMp4;
        try {
            //需要解码数据的类型
            //初始化解码器
            mDecoder = MediaCodec.createDecoderByType(AAC_MIME);
            MediaFormat mediaFormat = new MediaFormat();
            mediaFormat.setString(MediaFormat.KEY_MIME, AAC_MIME);
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
            mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, samplerate);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            //用来标记AAC是否有adts头，1->有
            mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);

            byte[] data = new byte[]{(byte) 0x11, (byte) 0x90};
            ByteBuffer csd_0 = ByteBuffer.wrap(data);
            mediaFormat.setByteBuffer("csd-0", csd_0);

            writeMp4.addTrack(mediaFormat, WriteMp4.voice);

            //解码器配置
            mDecoder.configure(mediaFormat, null, null, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mDecoder.start();
    }

    public void register(VoicePlayer voicePlayer) {
        this.voicePlayer = voicePlayer;
    }

    public void star() {
        isdecoder = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] poll;
                ByteBuffer dstBuf;
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                ByteBuffer outputBuffer;
                byte[] outData;
                while (isdecoder) {
                    poll = baseRecive.getVoice();
                    if (poll != null) {
                        //写文件
                        writeFile(poll, poll.length);
                        try {
                            //返回一个包含有效数据的input buffer的index,-1->不存在
                            int inputBufIndex = mDecoder.dequeueInputBuffer(Value.waitTime);
                            if (inputBufIndex >= 0) {
                                //获取当前的ByteBuffer
                                dstBuf = mDecoder.getInputBuffer(inputBufIndex);
                                dstBuf.clear();
                                dstBuf.put(poll, 0, poll.length);
                                mDecoder.queueInputBuffer(inputBufIndex, 0, poll.length, 0, 0);
                            } else {
                                Log.e("dcoder_failure", "dcoder failure_VC");
                                continue;
                            }
                            int outputBufferIndex = mDecoder.dequeueOutputBuffer(info, Value.waitTime);

                            while (outputBufferIndex >= 0) {
                                outputBuffer = mDecoder.getOutputBuffer(outputBufferIndex);
                                outData = new byte[info.size];
                                outputBuffer.get(outData);
                                outputBuffer.clear();
                                if (voicePlayer != null) {
                                    //通过接口回调播放
                                    voicePlayer.voicePlayer(outData);
                                }
                                mDecoder.releaseOutputBuffer(outputBufferIndex, false);
                                outputBufferIndex = mDecoder.dequeueOutputBuffer(info, Value.waitTime);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
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


    private MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private ByteBuffer writebuffer = ByteBuffer.allocate(548);

    /*
    写入文件
     */
    private void writeFile(byte[] output, int length) {
        writebuffer.clear();
        writebuffer.put(output);
        bufferInfo.size = length;
        bufferInfo.offset = 0;
        bufferInfo.presentationTimeUs = Value.getFPS();
        bufferInfo.flags = MediaCodec.CRYPTO_MODE_UNENCRYPTED;
        writeMp4.write(WriteMp4.voice, writebuffer, bufferInfo);
    }

    /*
     * 释放资源
     */
    public void stop() {
        isdecoder = false;
    }

    public void destroy() {
        isdecoder = false;
        mDecoder.stop();
        mDecoder.release();
        mDecoder = null;
    }
}

