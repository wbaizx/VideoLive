package com.library.vc;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.library.stream.BaseSend;
import com.library.file.WriteMp4;

/**
 * Created by android1 on 2017/9/22.
 */

public class VoiceRecord {
    private AudioRecord audioRecord;
    private boolean isrecord = false;
    private int recBufSize;
    private int samplerate = 44100;
    //音频编码
    private VCEncoder vencoder;

    /*
     *初始化
     */
    public VoiceRecord(BaseSend baseSend, int bitrate_vc, WriteMp4 writeMp4) {
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
        vencoder = new VCEncoder(samplerate, bitrate_vc, recBufSize, baseSend, writeMp4);
    }

    /*
     * 得到语音原始数据
     */
    public void star() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (audioRecord != null) {
                    isrecord = true;
                    byte[] buffer = new byte[recBufSize];
                    byte[] result;
                    int bufferReadResult;
                    audioRecord.startRecording();//开始录制
                    while (isrecord) {
                        bufferReadResult = audioRecord.read(buffer, 0, recBufSize);
                        if (bufferReadResult == AudioRecord.ERROR_INVALID_OPERATION || bufferReadResult == AudioRecord.ERROR_BAD_VALUE || bufferReadResult == 0 || bufferReadResult == -1) {
                            continue;
                        }
                        result = new byte[bufferReadResult];
                        System.arraycopy(buffer, 0, result, 0, bufferReadResult);
                        vencoder.encode(result);
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
