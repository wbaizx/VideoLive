package com.library.vc;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.library.file.WriteMp4;
import com.library.stream.BaseSend;
import com.library.util.VoiceUtil;

import java.util.Arrays;

/**
 * Created by android1 on 2017/9/22.
 */

public class VoiceRecord {
    private AudioRecord audioRecord;
    private boolean isrecord = false;
    private int recBufSize;
    private int samplerate = 44100;
    //音频推流编码
    private VCEncoder vencoder;
    //音频录制编码
    private RecordEncoderVC recordEncoderVC;

    private int multiple = 1;

    /*
     *初始化
     */
    public VoiceRecord(BaseSend baseSend, int collectionbitrate_vc, int publishbitrate_vc, WriteMp4 writeMp4) {
        recBufSize = AudioRecord.getMinBufferSize(
                samplerate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,//降噪配置
                samplerate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                recBufSize);
        vencoder = new VCEncoder(samplerate, publishbitrate_vc, recBufSize, baseSend);
        recordEncoderVC = new RecordEncoderVC(samplerate, collectionbitrate_vc, recBufSize, writeMp4);
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
                    int bufferReadResult;
                    audioRecord.startRecording();//开始录制
                    while (isrecord) {
                        /*
                        两针间采集大概40ms，编码发送大概10ms，单线程顺序执行没有问题
                         */
                        bufferReadResult = audioRecord.read(buffer, 0, recBufSize);

                        if (bufferReadResult == AudioRecord.ERROR_INVALID_OPERATION || bufferReadResult == AudioRecord.ERROR_BAD_VALUE || bufferReadResult == 0 || bufferReadResult == -1) {
                            continue;
                        }
                        byte[] bytes;
                        if (multiple == 1) {
                            bytes = Arrays.copyOfRange(buffer, 0, bufferReadResult);
                        } else {
                            bytes = VoiceUtil.increasePCM(buffer, bufferReadResult, multiple);
                        }
                        vencoder.encode(bytes);
                        recordEncoderVC.encode(bytes);
                    }
                }
            }
        }).start();
    }


    public void setIncreaseMultiple(int multiple) {
        this.multiple = Math.max(1, Math.min(8, multiple));
    }

    public void destroy() {
        isrecord = false;
        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;
        recordEncoderVC.destroy();
        vencoder.destroy();
    }
}
