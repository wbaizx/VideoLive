package com.library.vc;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.library.stream.BaseRecive;
import com.library.file.WriteMp4;

/**
 * Created by android1 on 2017/9/23.
 */

public class VoiceTrack implements VoicePlayer {

    private int samplerate = 44100;
    private VCDecoder vdecoder;

    private AudioTrack audioTrack;

    public VoiceTrack(BaseRecive baseRecive, WriteMp4 writeMp4) {
        int recBufSize = AudioTrack.getMinBufferSize(
                samplerate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                samplerate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                recBufSize,
                AudioTrack.MODE_STREAM);

        vdecoder = new VCDecoder(samplerate, baseRecive, writeMp4);
        //注册回调接口
        vdecoder.register(this);

        if (audioTrack != null) {
            audioTrack.play();
        }
    }

    public void star() {
        vdecoder.star();
    }

    @Override
    public void voicePlayer(byte[] voicebyte) {
        audioTrack.write(voicebyte, 0, voicebyte.length);
    }

    public void stop() {
        vdecoder.stop();
    }

    public void destroy() {
        audioTrack.stop();
        audioTrack.release();
        audioTrack = null;
        vdecoder.destroy();
    }
}
