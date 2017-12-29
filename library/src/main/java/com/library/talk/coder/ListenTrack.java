package com.library.talk.coder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.library.common.VoicePlayer;
import com.library.talk.stream.ListenRecive;
import com.library.util.OtherUtil;
import com.library.util.VoiceUtil;

/**
 * Created by android1 on 2017/12/25.
 */

public class ListenTrack implements VoicePlayer {
    private AudioTrack audioTrack;
    private ListenDecoder listenDecoder;
    private int multiple;

    public ListenTrack(ListenRecive listenRecive) {
        int recBufSize = AudioTrack.getMinBufferSize(
                OtherUtil.samplerate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                OtherUtil.samplerate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                recBufSize,
                AudioTrack.MODE_STREAM);

        listenDecoder = new ListenDecoder();
        listenRecive.setVoiceCallback(listenDecoder);
        listenDecoder.register(this);
    }

    public void start() {
        if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_STOPPED) {
            audioTrack.play();
            listenDecoder.start();
        }
    }

    public void setVoiceIncreaseMultiple(int multiple) {
        this.multiple = Math.max(1, Math.min(8, multiple));
    }

    @Override
    public void voicePlayer(byte[] voicebyte) {
        if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            //控制音频音量
            audioTrack.write(VoiceUtil.increasePCM(voicebyte, multiple), 0, voicebyte.length);
        }
    }


    public void stop() {
        if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            listenDecoder.stop();
            audioTrack.stop();
        }
    }

    public void destroy() {
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
        listenDecoder.destroy();
    }
}

