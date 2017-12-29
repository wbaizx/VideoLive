package com.library.talk;

import com.library.common.UdpControlInterface;
import com.library.common.WriteCallback;
import com.library.talk.coder.SpeakRecord;
import com.library.talk.file.WriteMp3;
import com.library.talk.stream.SpeakSend;

/**
 * Created by android1 on 2017/12/23.
 */

public class Speak {
    private SpeakRecord speakRecord;
    private SpeakSend speakSend;
    private WriteMp3 writeMp3;

    public Speak( int publishBitrate, UdpControlInterface udpControl, SpeakSend speakSend, String path) {
        this.speakSend = speakSend;
        speakSend.setUdpControl(udpControl);
        writeMp3 = new WriteMp3(path);
        speakRecord = new SpeakRecord(publishBitrate, speakSend, writeMp3);
    }

    public void start() {
        speakSend.start();
        speakRecord.start();
    }

    public void stop() {
        speakRecord.stop();
        speakSend.stop();
    }

    public void setWriteCallback(WriteCallback writeCallback) {
        writeMp3.setWriteCallback(writeCallback);
    }

    public void startRecode() {
        writeMp3.start();
    }

    public void stopRecode() {
        writeMp3.stop();
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
        writeMp3.destroy();
    }

    public static class Buider {
        private int publishBitrate = 20 * 1024;
        private SpeakSend speakSend;
        private UdpControlInterface udpControl;
        private String path = null;

        public Buider setPushMode(SpeakSend speakSend) {
            this.speakSend = speakSend;
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

        public Buider setVoicePath(String path) {
            this.path = path;
            return this;
        }

        public Speak build() {
            return new Speak(publishBitrate, udpControl, speakSend, path);
        }
    }
}
