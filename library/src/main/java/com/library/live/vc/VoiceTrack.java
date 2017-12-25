package com.library.live.vc;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.library.live.file.WriteMp4;
import com.library.live.stream.BaseRecive;
import com.library.util.OtherUtil;

/**
 * Created by android1 on 2017/9/23.
 */

public class VoiceTrack implements VoicePlayer {
    private VCDecoder vcdecoder;
    private AudioTrack audioTrack;

    public VoiceTrack(BaseRecive baseRecive, WriteMp4 writeMp4) {
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

        vcdecoder = new VCDecoder(baseRecive, writeMp4);
        //注册回调接口
        vcdecoder.register(this);
    }

    public void start() {
        if (audioTrack != null) {
            audioTrack.play();
            vcdecoder.start();
        }
    }

    @Override
    public void voicePlayer(byte[] voicebyte) {
        if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.write(voicebyte, 0, voicebyte.length);
        }
    }

    public void stop() {
        if (audioTrack != null) {
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
