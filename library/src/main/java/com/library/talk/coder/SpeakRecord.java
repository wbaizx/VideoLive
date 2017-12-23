package com.library.talk.coder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.library.util.OtherUtil;

/**
 * Created by android1 on 2017/12/23.
 */

public class SpeakRecord {
    private int recBufSize;
    private AudioRecord audioRecord;

    private int multiple;

    public SpeakRecord(int collectionBitrate, int collectionBitrate1, int multiple) {
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

        this.multiple = multiple;
    }

    public void start() {
    }

    public void stop() {
    }

    public void destroy() {
    }
}
