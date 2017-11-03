package com.library.vc;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.library.stream.BaseSend;
import com.library.util.WriteMp4;
import com.library.util.data.Value;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by android1 on 2017/9/22.
 */

public class VCEncoder {
    private final String AAC_MIME = MediaFormat.MIMETYPE_AUDIO_AAC;
    private MediaCodec mediaCodec;
    private boolean isencoder = false;
    private BaseSend baseSend;
    //文件录入类
    private WriteMp4 writeMp4;

    public VCEncoder(int samplerate, int bitrate, int recBufSize, BaseSend baseSend, WriteMp4 writeMp4) {
        //UDP实例
        this.baseSend = baseSend;
        this.writeMp4 = writeMp4;
        try {
            mediaCodec = MediaCodec.createEncoderByType(AAC_MIME);
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, AAC_MIME);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, samplerate);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, recBufSize * 2);

        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
    }

    public void star() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                isencoder = true;
                byte[] buffer;
                ByteBuffer[] inputBuffers;
                ByteBuffer[] outputBuffers;
                ByteBuffer outputBuffer;
                byte[] outData;
                while (isencoder) {
                    if (VoiceRecord.PCMQueue.size() > 0) {
                        buffer = VoiceRecord.PCMQueue.poll();
                        inputBuffers = mediaCodec.getInputBuffers();
                        outputBuffers = mediaCodec.getOutputBuffers();
                        int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
                        if (inputBufferIndex >= 0) {
                            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                            inputBuffer.clear();
                            inputBuffer.put(buffer);
                            mediaCodec.queueInputBuffer(inputBufferIndex, 0, buffer.length, Value.getFPS(), 0);
                        }
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);

                        if (MediaCodec.INFO_OUTPUT_FORMAT_CHANGED == outputBufferIndex) {
                            writeMp4.addTrack(mediaCodec.getOutputFormat(), WriteMp4.voice);
                        }

                        while (outputBufferIndex >= 0) {
                            outputBuffer = outputBuffers[outputBufferIndex];
                            outputBuffer.position(bufferInfo.offset);
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                            outData = new byte[bufferInfo.size + 7];
                            addADTStoPacket(outData, bufferInfo.size + 7);
                            outputBuffer.get(outData, 7, bufferInfo.size);
                            //添加将要发送的音频数据
                            //写文件
                            writeMp4.write(WriteMp4.voice, outputBuffer, bufferInfo);

                            baseSend.addVoice(outData);
                            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
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

    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;  //AAC LC
        //39=MediaCodecInfo.CodecProfileLevel.AACObjectELD;
        int freqIdx = 4;  //44.1KHz
        int chanCfg = 2;  //CPE
        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    public void destroy() {
        isencoder = false;
        mediaCodec.stop();
        mediaCodec.release();
        mediaCodec = null;
    }

}
