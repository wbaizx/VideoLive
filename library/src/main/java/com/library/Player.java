package com.library;

import com.library.file.WriteMp4;
import com.library.stream.BaseRecive;
import com.library.stream.UdpControlInterface;
import com.library.vc.VoiceTrack;
import com.library.vd.VDDecoder;
import com.library.view.PlayerView;

/**
 * Created by android1 on 2017/10/13.
 */

public class Player {
    private VDDecoder vdDecoder;
    private VoiceTrack voiceTrack;
    private BaseRecive baseRecive;
    private WriteMp4 writeMp4;
    private PlayerView playerView;

    public Player(PlayerView playerView, String codetype, BaseRecive baseRecive, UdpControlInterface udpControl, String path) {
        this.baseRecive = baseRecive;
        this.playerView = playerView;
        this.baseRecive.setUdpControl(udpControl);
        //文件录入类
        writeMp4 = new WriteMp4(path);

        vdDecoder = new VDDecoder(playerView, codetype, baseRecive, writeMp4);
        voiceTrack = new VoiceTrack(baseRecive, writeMp4);
    }

    public void setWriteCallback(WriteMp4.writeCallback writeCallback) {
        writeMp4.setWriteCallback(writeCallback);
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
        playerView.stop();
    }

    public void destroy() {
        vdDecoder.destroy();
        voiceTrack.destroy();
        baseRecive.destroy();
        writeMp4.destroy();
    }

    public void starRecode() {
        writeMp4.star();
    }

    public void stopRecode() {
        writeMp4.stop();
    }

    public static class Buider {
        private PlayerView playerView;
        private BaseRecive baseRecive;
        private String codetype = VDDecoder.H264;
        private UdpControlInterface udpControl = null;
        //录制地址
        private String path = null;

        private int udpPacketCacheMin = 5;//udp包最小缓存数量，用于udp包排序
        private int videoFrameCacheMin = 6;//视频帧达到播放标准的数量
        private int videoCarltontime = 400;//视频帧缓冲时间
        private int voiceCarltontime = 400;//音频帧缓冲时间

        private IsOutBuffer isOutBuffer = null;//缓冲接口回调

        public Buider(PlayerView playerView) {
            this.playerView = playerView;
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

        public Buider setIsOutBuffer(IsOutBuffer isOutBuffer) {
            this.isOutBuffer = isOutBuffer;
            return this;
        }

        public Buider setBufferAnimator(boolean bufferAnimator) {
            playerView.setBufferAnimator(bufferAnimator);
            return this;
        }

        public Player build() {
            baseRecive.setUdpPacketCacheMin(udpPacketCacheMin);
            baseRecive.setIsInBuffer(playerView);//将playerView接口设置给baseRecive用以回调缓冲状态
            playerView.setIsOutBuffer(isOutBuffer);//给playerView设置isOutBuffer接口用以将缓冲状态回调给客户端
            baseRecive.setOther(videoFrameCacheMin, videoCarltontime, voiceCarltontime);
            return new Player(playerView, codetype, baseRecive, udpControl, path);
        }
    }
}
