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

    private Listen(ListenRecive listenRecive, int udpPacketCacheMin, int voiceFrameCacheMin, UdpControlInterface udpControl) {
        listenTrack = new ListenTrack(listenRecive);
        listenRecive.setUdpControl(udpControl);
        listenRecive.setUdpPacketMin(udpPacketCacheMin);
        listenRecive.setVoiceFrameCacheMin(voiceFrameCacheMin);
        this.listenRecive = listenRecive;
    }

    public void start() {
        listenTrack.start();
        listenRecive.start();
    }

    public void stop() {
        listenRecive.stop();
        listenTrack.stop();
    }


    public void destroy() {
        listenRecive.destroy();
        listenTrack.destroy();
    }

    public void write(byte[] bytes) {
        listenRecive.write(bytes);
    }


    public static class Buider {
        private ListenRecive listenRecive;
        private int udpPacketCacheMin = 2;
        private int voiceFrameCacheMin = 5;
        private UdpControlInterface udpControl;

        public Buider setPullMode(ListenRecive listenRecive) {
            this.listenRecive = listenRecive;
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
            return new Listen(listenRecive, udpPacketCacheMin, voiceFrameCacheMin, udpControl);
        }
    }
}

