package com.library.talk;

import com.library.common.UdpControlInterface;
import com.library.talk.coder.ListenTrack;
import com.library.talk.stream.ListenRecive;

/**
 * Created by android1 on 2017/12/23.
 */

public class Listen {
    private ListenTrack listenTrack;
    private ListenRecive listenRecive;
    private boolean isStartRevice = false;

    private Listen(ListenRecive listenRecive, int udpPacketCacheMin, int voiceFrameCacheMin, UdpControlInterface udpControl, int multiple) {
        listenTrack = new ListenTrack(listenRecive);
        listenTrack.setVoiceIncreaseMultiple(multiple);
        listenRecive.setUdpControl(udpControl);
        listenRecive.setUdpPacketMin(udpPacketCacheMin);
        listenRecive.setVoiceFrameCacheMin(voiceFrameCacheMin);
        this.listenRecive = listenRecive;
    }

    public void start() {
        isStartRevice = true;
        listenTrack.start();
        listenRecive.start();
    }

    public void stop() {
        isStartRevice = false;
        listenRecive.stop();
        listenTrack.stop();
    }

    public void setVoiceIncreaseMultiple(int multiple) {
        listenTrack.setVoiceIncreaseMultiple(multiple);
    }

    public void destroy() {
        isStartRevice = false;
        listenRecive.destroy();
        listenTrack.destroy();
    }

    public void write(byte[] bytes) {
        listenRecive.write(bytes);
    }

    public boolean isStartRevice() {
        return isStartRevice;
    }


    public static class Buider {
        private ListenRecive listenRecive;
        private int udpPacketCacheMin = 2;
        private int voiceFrameCacheMin = 5;
        private UdpControlInterface udpControl;
        private int multiple = 1;

        public Buider setPullMode(ListenRecive listenRecive) {
            this.listenRecive = listenRecive;
            return this;
        }

        public Buider setMultiple(int multiple) {
            this.multiple = multiple;
            return this;
        }

        public Buider setUdpPacketCacheMin(int udpPacketCacheMin) {
            this.udpPacketCacheMin = udpPacketCacheMin;
            return this;
        }

        public Buider setUdpControl(UdpControlInterface udpControl) {
            this.udpControl = udpControl;
            return this;
        }

        public Buider setVoiceFrameCacheMin(int voiceFrameCacheMin) {
            this.voiceFrameCacheMin = voiceFrameCacheMin;
            return this;
        }

        public Listen build() {
            return new Listen(listenRecive, udpPacketCacheMin, voiceFrameCacheMin, udpControl, multiple);
        }
    }
}

