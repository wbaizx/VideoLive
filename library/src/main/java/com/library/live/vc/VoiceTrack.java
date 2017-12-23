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
    private VCDecoder vdecoder;
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

        vdecoder = new VCDecoder(baseRecive, writeMp4);
        //注册回调接口
        vdecoder.register(this);
    }

    public void star() {
        if (audioTrack != null) {
            audioTrack.play();
            vdecoder.star();
        }
    }

    @Override
    public void voicePlayer(byte[] voicebyte) {
        audioTrack.write(voicebyte, 0, voicebyte.length);
    }

    public void stop() {
        if (audioTrack != null) {
            vdecoder.stop();
            audioTrack.stop();
        }
    }

    public void destroy() {
        audioTrack.release();
        audioTrack = null;
        vdecoder.destroy();
    }
}
