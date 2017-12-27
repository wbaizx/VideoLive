package com.library.live.vc;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.library.live.stream.BaseSend;
import com.library.util.OtherUtil;
import com.library.util.VoiceUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by android1 on 2017/9/22.
 */

public class VCEncoder {
    private final String AAC_MIME = MediaFormat.MIMETYPE_AUDIO_AAC;
    private MediaCodec mediaCodec;
    private BaseSend baseSend;

    public VCEncoder(int bitrate, int recBufSize, BaseSend baseSend) {
        this.baseSend = baseSend;
        try {
            mediaCodec = MediaCodec.createEncoderByType(AAC_MIME);
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, AAC_MIME);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, OtherUtil.samplerate);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, recBufSize * 2);

        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
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

            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                outputBuffer.position(bufferInfo.offset);
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                byte[] outData = new byte[bufferInfo.size + 7];
                VoiceUtil.addADTStoPacket(outData, bufferInfo.size + 7);
                outputBuffer.get(outData, 7, bufferInfo.size);
                //添加将要发送的音频数据
                baseSend.addVoice(outData);

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, OtherUtil.waitTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        mediaCodec.stop();
        mediaCodec.release();
        mediaCodec = null;
    }
}
