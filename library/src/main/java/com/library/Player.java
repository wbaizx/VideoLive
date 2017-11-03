package com.library;

import android.view.SurfaceView;

import com.library.stream.BaseRecive;
import com.library.stream.upd.UdpControlInterface;
import com.library.util.WriteMp4;
import com.library.util.data.Value;
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

    public Player(SurfaceView surfaceView, int width, int height, String codetype, BaseRecive baseRecive, UdpControlInterface udpControl, String path) {
        this.baseRecive = baseRecive;
        this.baseRecive.setUdpControl(udpControl);
        //文件录入类
        writeMp4 = new WriteMp4(path);

        vdDecoder = new VDDecoder(surfaceView, width, height, codetype, baseRecive, writeMp4);
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
        //解码分辨率
        private int width = 480;
        private int height = 720;
        private BaseRecive baseRecive;
        private String codetype = VDDecoder.H264;
        private UdpControlInterface udpControl = null;
        //录制地址
        private String path = null;

        public Buider(SurfaceView surfaceView) {
            this.surfaceView = surfaceView;
        }

        //编码分辨率
        public Buider setVideoSize(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Buider setVideoCode(String codetype) {
            this.codetype = codetype;
            return this;
        }

        public Buider setUrl(String ip) {
            Value.IP = ip;
            return this;
        }

        public Buider setPort(int port) {
            Value.PORT = port;
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
            return new Player(surfaceView, width, height, codetype, baseRecive, udpControl, path);
        }


    }
}
