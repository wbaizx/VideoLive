package com.library.vc;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.library.stream.BaseRecive;
import com.library.util.WriteMp4;
import com.library.util.data.Value;

/**
 * Created by android1 on 2017/9/23.
 */

public class VoiceTrack {

    private int recBufSize;
    private int samplerate = 44100;
    private int bitrate = 32000;
    private VCDecoder vdecoder;

    private AudioTrack audioTrack;
    private boolean isTrack = false;

    public VoiceTrack(BaseRecive baseRecive, WriteMp4 writeMp4) {
        recBufSize = AudioTrack.getMinBufferSize(
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

        vdecoder = new VCDecoder(samplerate, bitrate, recBufSize, baseRecive, writeMp4);

        if (audioTrack != null) {
            audioTrack.play();
        }
    }

    public void star() {
        vdecoder.star();
        isTrack = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] poll;
                while (isTrack) {
                    if (VCDecoder.PCMQueue.size() > 0) {
                        poll = VCDecoder.PCMQueue.poll();
                        audioTrack.write(poll, 0, poll.length);
                    } else {
                        try {
                            Thread.sleep(Value.sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    public void stop() {
        isTrack = false;
        vdecoder.stop();
    }

    public void destroy() {
        isTrack = false;
        audioTrack.stop();
        audioTrack.release();
        audioTrack = null;
        vdecoder.destroy();
    }
}
