package com.library.live.vc;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.library.live.file.WriteMp4;
import com.library.util.OtherUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by android1 on 2017/12/12.
 */

public class RecordEncoderVC {
    private final String AAC_MIME = MediaFormat.MIMETYPE_AUDIO_AAC;
    private WriteMp4 writeMp4;
    private MediaCodec mediaCodec;

    public RecordEncoderVC(int bitrate, int recBufSize, WriteMp4 writeMp4) {
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
    public void encode(byte[] result) {
        try {
            inputBufferIndex = mediaCodec.dequeueInputBuffer(OtherUtil.waitTime);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                inputBuffer.put(result);
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, result.length, OtherUtil.getFPS(), 0);
            }

            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, OtherUtil.waitTime);

            if (MediaCodec.INFO_OUTPUT_FORMAT_CHANGED == outputBufferIndex) {
                writeMp4.addTrack(mediaCodec.getOutputFormat(), WriteMp4.voice);
            }

            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                outputBuffer.position(bufferInfo.offset);
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                byte[] outData = new byte[bufferInfo.size + 7];
                addADTStoPacket(outData, bufferInfo.size + 7);
                outputBuffer.get(outData, 7, bufferInfo.size);
                //写文件
                writeMp4.write(WriteMp4.voice, outputBuffer, bufferInfo);

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, OtherUtil.waitTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addADTStoPacket(byte[] packet, int packetLen) {
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((2/*profile AAC LC*/ - 1) << 6) + (4/*freqIdx 44.1KHz*/ << 2) + (3/*chanCfgCPE*/ >> 2));
        packet[3] = (byte) (((3/*chanCfgCPE*/ & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    public void destroy() {
        mediaCodec.stop();
        mediaCodec.release();
        mediaCodec = null;
    }

}
