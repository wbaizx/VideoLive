package com.library.talk;

import com.library.common.UdpControlInterface;
import com.library.talk.coder.SpeakRecord;
import com.library.talk.stream.SpeakSend;

/**
 * Created by android1 on 2017/12/23.
 */

public class Speak {
    private SpeakRecord speakRecord;
    private SpeakSend speakSend;

    public Speak(int collectionBitrate, int publishBitrate, UdpControlInterface udpControl, SpeakSend speakSend) {
        this.speakSend = speakSend;
        speakSend.setUdpControl(udpControl);
        speakRecord = new SpeakRecord(collectionBitrate, publishBitrate, speakSend);
    }

    public void start() {
        speakSend.start();
        speakRecord.start();
    }

    public void stop() {
        speakRecord.stop();
        speakSend.stop();
    }

    public int getDecibel() {
        return speakRecord.getDecibel();
    }

    public void startJustSend() {
        speakSend.startJustSend();
    }

    public void stopJustSend() {
        speakSend.stop();
    }

    public void addbytes(byte[] voice) {
        speakSend.addbytes(voice);
    }

    public void destroy() {
        speakRecord.destroy();
        speakSend.destroy();
    }

    public static class Buider {
        private int collectionBitrate = 64 * 1024;
        private int publishBitrate = 20 * 1024;
        private SpeakSend speakSend;
        private UdpControlInterface udpControl;

        public Buider setPushMode(SpeakSend speakSend) {
            this.speakSend = speakSend;
            return this;
        }

        public Buider setCollectionBitrate(int collectionBitrate) {
            this.collectionBitrate = collectionBitrate;
            return this;
        }

        public Buider setPublishBitrate(int publishBitrate) {
            this.publishBitrate = publishBitrate;
            return this;
        }


        public Buider setUdpControl(UdpControlInterface udpControl) {
            this.udpControl = udpControl;
            return this;
        }

        public Speak build() {
            return new Speak(collectionBitrate, publishBitrate, udpControl, speakSend);
        }
    }
}
