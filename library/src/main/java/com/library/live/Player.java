package com.library.live;

import com.library.common.UdpControlInterface;
import com.library.live.stream.UdpRecive;
import com.library.live.vc.VoiceTrack;
import com.library.live.vd.VDDecoder;
import com.library.live.view.PlayerView;

/**
 * Created by android1 on 2017/10/13.
 */

public class Player {
    private VDDecoder vdDecoder;
    private VoiceTrack voiceTrack;
    private UdpRecive udpRecive;
    private PlayerView playerView;

    private Player(PlayerView playerView, String codetype, UdpRecive udpRecive, UdpControlInterface udpControl, int multiple) {
        this.udpRecive = udpRecive;
        this.playerView = playerView;
        this.udpRecive.setUdpControl(udpControl);

        vdDecoder = new VDDecoder(playerView, codetype, udpRecive);
        voiceTrack = new VoiceTrack(udpRecive);
        voiceTrack.setIncreaseMultiple(multiple);
    }

    public void setVoiceIncreaseMultiple(int multiple) {
        voiceTrack.setIncreaseMultiple(multiple);
    }


    public void start() {
        voiceTrack.start();
        vdDecoder.start();
        udpRecive.startRevice();
    }

    public void stop() {
        udpRecive.stopRevice();
        vdDecoder.stop();
        voiceTrack.stop();
        playerView.stop();
    }

    public void destroy() {
        udpRecive.destroy();
        vdDecoder.destroy();
        voiceTrack.destroy();
    }

    public void write(byte[] bytes) {
        udpRecive.write(bytes);
    }

    public int getReciveStatus() {
        return udpRecive.getReciveStatus();
    }

    public static class Buider {
        private PlayerView playerView;
        private UdpRecive udpRecive;
        private String codetype = VDDecoder.H264;
        private UdpControlInterface udpControl = null;
        private int multiple = 1;

        private IsOutBuffer isOutBuffer = null;//缓冲接口回调

        public Buider(PlayerView playerView) {
            this.playerView = playerView;
        }

        public Buider setVideoCode(String codetype) {
            this.codetype = codetype;
            return this;
        }


        public Buider setPullMode(UdpRecive udpRecive) {
            this.udpRecive = udpRecive;
            return this;
        }

        public Buider setUdpControl(UdpControlInterface udpControl) {
            this.udpControl = udpControl;
            return this;
        }

        public Buider setMultiple(int multiple) {
            this.multiple = multiple;
            return this;
        }

        public Buider setUdpPacketCacheMin(int udpPacketCacheMin) {
            udpRecive.setUdpPacketCacheMin(udpPacketCacheMin);
            return this;
        }

        public Buider setVideoFrameCacheMin(int videoFrameCacheMin) {
            udpRecive.setOther(videoFrameCacheMin);
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

        public Buider setCenterScaleType(boolean isCenterScaleType) {
            playerView.setCenterScaleType(isCenterScaleType);
            return this;
        }

        public Player build() {
            udpRecive.setWeightCallback(playerView);//将playerView接口设置给baseRecive用以回调图像比例
            udpRecive.setIsInBuffer(playerView);//将playerView接口设置给baseRecive用以回调缓冲状态
            playerView.setIsOutBuffer(isOutBuffer);//给playerView设置isOutBuffer接口用以将缓冲状态回调给客户端
            return new Player(playerView, codetype, udpRecive, udpControl, multiple);
        }
    }
}
