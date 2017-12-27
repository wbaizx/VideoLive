package com.library.live.vc;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.library.live.file.WriteMp4;
import com.library.live.stream.BaseSend;
import com.library.util.OtherUtil;
import com.library.util.SingleThreadExecutor;

/**
 * Created by android1 on 2017/9/22.
 */

public class VoiceRecord {
    private AudioRecord audioRecord;
    private int recBufSize;
    //音频推流编码
    private VCEncoder vencoder;
    //音频录制编码
    private RecordEncoderVC recordEncoderVC;

    private SingleThreadExecutor singleThreadExecutor;

    public VoiceRecord(BaseSend baseSend, int collectionbitrate_vc, int publishbitrate_vc, WriteMp4 writeMp4) {
        recBufSize = AudioRecord.getMinBufferSize(
                OtherUtil.samplerate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,//降噪配置
                OtherUtil.samplerate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                recBufSize);
        vencoder = new VCEncoder(publishbitrate_vc, recBufSize, baseSend);
        recordEncoderVC = new RecordEncoderVC(collectionbitrate_vc, recBufSize, writeMp4);
        singleThreadExecutor = new SingleThreadExecutor();
    }

    /*
     * 得到语音原始数据
     */
    public void start() {
        singleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (audioRecord != null) {
                    byte[] buffer = new byte[recBufSize];
                    int bufferReadResult;
                    audioRecord.startRecording();//开始录制
                    while (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        /*
                        两针间采集大概40ms，编码发送大概10ms，单线程顺序执行没有问题
                         */
                        bufferReadResult = audioRecord.read(buffer, 0, recBufSize);

                        if (bufferReadResult == AudioRecord.ERROR_INVALID_OPERATION || bufferReadResult == AudioRecord.ERROR_BAD_VALUE || bufferReadResult == 0 || bufferReadResult == -1) {
                            continue;
                        }
                        vencoder.encode(buffer, bufferReadResult);
                        recordEncoderVC.encode(buffer, bufferReadResult);
                    }
                }
            }
        });
    }


    public void destroy() {
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
        recordEncoderVC.destroy();
        vencoder.destroy();
        singleThreadExecutor.shutdownNow();
    }
}
