package com.library;

import android.view.SurfaceView;

import com.library.file.WriteMp4;
import com.library.stream.BaseRecive;
import com.library.stream.IsLiveBuffer;
import com.library.stream.UdpControlInterface;
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
        voiceTrack.star();
        vdDecoder.star();
        baseRecive.starRevice();
    }

    public void stop() {
        baseRecive.stopRevice();
        vdDecoder.stop();
        voiceTrack.stop();
    }

    public void destroy() {
        writeMp4.destroy();
        baseRecive.destroy();
        vdDecoder.destroy();
        voiceTrack.destroy();
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

        private int udpPacketCacheMin = 40;//udp包最小缓存数量，用于udp包排序
        private int videoFrameCacheMin = 20;//视频帧达到播放标准的数量
        private int videoCarltontime = 500;//视频帧缓冲时间
        private int voiceCarltontime = 100;//音频帧缓冲时间

        private IsLiveBuffer isLiveBuffer;//缓冲接口回调，用于客户端判断是否正在缓冲

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

        public Buider setUdpPacketCacheMin(int udpPacketCacheMin) {
            this.udpPacketCacheMin = udpPacketCacheMin;
            return this;
        }

        public Buider setVideoFrameCacheMin(int videoFrameCacheMin) {
            this.videoFrameCacheMin = videoFrameCacheMin;
            return this;
        }

        public Buider setVideoCarltontime(int videoCarltontime) {
            this.videoCarltontime = videoCarltontime;
            return this;
        }

        public Buider setVoiceCarltontime(int voiceCarltontime) {
            this.voiceCarltontime = voiceCarltontime;
            return this;
        }

        public Buider setIsLiveBuffer(IsLiveBuffer isLiveBuffer) {
            this.isLiveBuffer = isLiveBuffer;
            return this;
        }

        public Player build() {
            baseRecive.setUdpPacketCacheMin(udpPacketCacheMin);
            baseRecive.setIsLiveBuffer(isLiveBuffer);
            baseRecive.setOther(videoFrameCacheMin, videoCarltontime, voiceCarltontime);
            return new Player(surfaceView, codetype, baseRecive, udpControl, path);
        }
    }
}
