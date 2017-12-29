package com.library.talk.coder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.library.talk.file.WriteMp3;
import com.library.talk.stream.SpeakSend;
import com.library.util.OtherUtil;
import com.library.util.VoiceUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by android1 on 2017/12/25.
 */

public class SpeakEncoder {
    private final String AAC_MIME = MediaFormat.MIMETYPE_AUDIO_AAC;
    private MediaCodec mediaCodec;
    private SpeakSend speakSend;
    private WriteMp3 writeMp3;
    private MediaFormat format;

    public SpeakEncoder(int bitrate, int recBufSize, SpeakSend speakSend, String path) {
        this.speakSend = speakSend;
        writeMp3 = new WriteMp3(path);
        try {
            mediaCodec = MediaCodec.createEncoderByType(AAC_MIME);
        } catch (IOException e) {
            e.printStackTrace();
        }
        format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, AAC_MIME);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, OtherUtil.samplerate);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, recBufSize * 2);

    }

    public void start() {
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
    }

    public void stop() {
        mediaCodec.stop();
    }

    public void startRecord() {
        writeMp3.start();
    }

    public void stopRecord() {
        writeMp3.stop();
    }

    private MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

    private int outputBufferIndex;

    private int inputBufferIndex;

    /*
    音频数据编码，音频数据处理较少，直接编码
     */
    public void encode(byte[] result, int length) {
        try {
            inputBufferIndex = mediaCodec.dequeueInputBuffer(OtherUtil.waitTime);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                inputBuffer.put(result, 0, length);
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, length, OtherUtil.getFPS(), 0);
            }

            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, OtherUtil.waitTime);

            if (MediaCodec.INFO_OUTPUT_FORMAT_CHANGED == outputBufferIndex) {
                writeMp3.addTrack(mediaCodec.getOutputFormat());
            }

            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                outputBuffer.position(bufferInfo.offset);
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                byte[] outData = new byte[bufferInfo.size + 7];
                VoiceUtil.addADTStoPacket(outData, bufferInfo.size + 7);
                outputBuffer.get(outData, 7, bufferInfo.size);
                //写文件
                writeMp3.write(outputBuffer, bufferInfo);
                //添加将要发送的音频数据
                speakSend.addVoice(outData);

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, OtherUtil.waitTime);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void destroy() {
        if (mediaCodec != null) {
            mediaCodec.release();
            mediaCodec = null;
        }
        writeMp3.destroy();
    }
}

