package com.library.live.vc;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.library.common.VoicePlayer;
import com.library.live.stream.BaseRecive;
import com.library.util.OtherUtil;
import com.library.util.VoiceUtil;

/**
 * Created by android1 on 2017/9/23.
 */

public class VoiceTrack implements VoicePlayer {
    private VCDecoder vcdecoder;
    private AudioTrack audioTrack;
    private int multiple = 1;

    public VoiceTrack(BaseRecive baseRecive) {
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

        vcdecoder = new VCDecoder(baseRecive);
        //注册回调接口
        vcdecoder.register(this);
    }

    public void start() {
        if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_STOPPED) {
            audioTrack.play();
            vcdecoder.start();
        }
    }

    public void setIncreaseMultiple(int multiple) {
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
            vcdecoder.stop();
            audioTrack.stop();
        }
    }

    public void destroy() {
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
        vcdecoder.destroy();
    }

}
