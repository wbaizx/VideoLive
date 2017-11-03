package com.library.vc;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.library.stream.BaseSend;
import com.library.util.WriteMp4;
import com.library.util.data.Value;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by android1 on 2017/9/22.
 */

public class VoiceRecord {
    public static ArrayBlockingQueue<byte[]> PCMQueue = new ArrayBlockingQueue<>(Value.QueueNum);
    private AudioRecord audioRecord;
    private boolean isrecord = false;
    private int recBufSize;
    private int samplerate = 44100;
    private int bitrate = 32000;
    //音频编码
    private VCEncoder vencoder;

    /*
     *初始化
     */
    public VoiceRecord(BaseSend baseSend, WriteMp4 writeMp4) {
        recBufSize = AudioRecord.getMinBufferSize(
                samplerate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                samplerate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                recBufSize);
        vencoder = new VCEncoder(samplerate, bitrate, recBufSize, baseSend, writeMp4);
    }

    /*
     * 得到语音原始数据
     */
    public void star() {
        vencoder.star();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (audioRecord != null) {
                    isrecord = true;
                    byte[] buffer = new byte[recBufSize];
                    byte[] result;
                    int bufferReadResult;
                    audioRecord.startRecording();//开始录制
                    PCMQueue.clear();
                    while (isrecord) {
                        bufferReadResult = audioRecord.read(buffer, 0, recBufSize);
                        if (bufferReadResult == AudioRecord.ERROR_INVALID_OPERATION || bufferReadResult == AudioRecord.ERROR_BAD_VALUE || bufferReadResult == 0 || bufferReadResult == -1) {
                            continue;
                        }
                        result = new byte[bufferReadResult];
                        System.arraycopy(buffer, 0, result, 0, bufferReadResult);
                        if (PCMQueue.size() >= Value.QueueNum) {
                            PCMQueue.poll();
                        }
                        PCMQueue.add(result);
                    }
                }
            }
        }).start();
    }


    public void destroy() {
        isrecord = false;
        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;
        vencoder.destroy();
    }
}
