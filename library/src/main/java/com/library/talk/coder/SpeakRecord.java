package com.library.talk.coder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.library.talk.stream.SpeakSend;
import com.library.util.OtherUtil;
import com.library.util.SingleThreadExecutor;
import com.library.util.mLog;

import java.util.Arrays;

/**
 * Created by android1 on 2017/12/23.
 */

public class SpeakRecord {
    private int recBufSize;
    private AudioRecord audioRecord;
    private SingleThreadExecutor singleThreadExecutor = null;
    private SpeakEncoder speakEncoder;

    public SpeakRecord(int collectionBitrate, int publishBitrate, SpeakSend speakSend) {
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

        singleThreadExecutor = new SingleThreadExecutor();
        speakEncoder = new SpeakEncoder(publishBitrate, recBufSize, speakSend);
    }

    public void start() {
        if (audioRecord != null) {
            audioRecord.startRecording();
            startRecord();
        }
    }

    public void stop() {
        if (audioRecord != null) {
            audioRecord.stop();
        }
    }

    private void startRecord() {
        singleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[recBufSize];
                int bufferReadResult;
                while (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        /*
                        两针间采集大概40ms，编码发送大概10ms，单线程顺序执行没有问题
                         */
                    bufferReadResult = audioRecord.read(buffer, 0, recBufSize);

                    if (bufferReadResult == AudioRecord.ERROR_INVALID_OPERATION || bufferReadResult == AudioRecord.ERROR_BAD_VALUE || bufferReadResult == 0 || bufferReadResult == -1) {
                        continue;
                    }
                    speakEncoder.encode(Arrays.copyOfRange(buffer, 0, bufferReadResult));
                }
                mLog.log("interrupt_Thread", "speak关闭线程");
            }
        });
    }

    public void destroy() {
        singleThreadExecutor.shutdownNow();
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
        speakEncoder.destroy();
    }
}
