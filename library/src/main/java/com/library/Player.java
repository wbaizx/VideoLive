package com.library;

import android.view.SurfaceView;

import com.library.stream.BaseRecive;
import com.library.stream.UdpControlInterface;
import com.library.file.WriteMp4;
import com.library.vc.VoiceTrack;
import com.library.vd.VDDecoder;

/**
 * Created by android1 on 2017/10/13.
 */

public class Player {
    private VDDecoder vdDecoder;
    private VoiceTrack voiceTrack;
    private BaseRecive baseRecive;
    private WriteMp4 writeMp4;

    public Player(SurfaceView surfaceView, String codetype, BaseRecive baseRecive, UdpControlInterface udpControl, String path) {
        this.baseRecive = baseRecive;
        this.baseRecive.setUdpControl(udpControl);
        //文件录入类
        writeMp4 = new WriteMp4(path);

        vdDecoder = new VDDecoder(surfaceView, codetype, baseRecive, writeMp4);
        voiceTrack = new VoiceTrack(baseRecive, writeMp4);
    }

    public void star() {
        vdDecoder.star();
        voiceTrack.star();
        baseRecive.starRevice();
    }

    public void stop() {
        voiceTrack.stop();
        vdDecoder.stop();
        baseRecive.stopRevice();
    }

    public void destroy() {
        voiceTrack.destroy();
        vdDecoder.destroy();
        baseRecive.destroy();
        writeMp4.destroy();
    }

    public void starRecode() {
        writeMp4.star();
    }

    public void stopRecode() {
        writeMp4.destroy();
    }

    public static class Buider {
        private SurfaceView surfaceView;
        private BaseRecive baseRecive;
        private String codetype = VDDecoder.H264;
        private UdpControlInterface udpControl = null;
        //录制地址
        private String path = null;

        public Buider(SurfaceView surfaceView) {
            this.surfaceView = surfaceView;
        }

        public Buider setVideoCode(String codetype) {
            this.codetype = codetype;
            return this;
        }


        public Buider setPullMode(BaseRecive baseRecive) {
            this.baseRecive = baseRecive;
            return this;
        }

        public Buider setUdpControl(UdpControlInterface udpControl) {
            this.udpControl = udpControl;
            return this;
        }

        public Buider setVideoPath(String path) {
            this.path = path;
            return this;
        }

        public Player build() {
            return new Player(surfaceView, codetype, baseRecive, udpControl, path);
        }
    }
}
