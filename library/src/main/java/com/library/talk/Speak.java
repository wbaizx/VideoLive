package com.library.talk;

import com.library.common.UdpControlInterface;
import com.library.common.WriteFileCallback;
import com.library.talk.coder.SpeakRecord;
import com.library.talk.stream.SpeakSend;

/**
 * Created by android1 on 2017/12/23.
 */

public class Speak {
    private SpeakRecord speakRecord;
    private SpeakSend speakSend;

    public Speak(int publishBitrate, UdpControlInterface udpControl, SpeakSend speakSend, String dirpath) {
        this.speakSend = speakSend;
        speakSend.setUdpControl(udpControl);
        speakRecord = new SpeakRecord(publishBitrate, speakSend, dirpath);
    }

    public void start() {
        speakSend.start();
        speakRecord.start();
    }

    public void stop() {
        speakRecord.stop();
        speakSend.stop();
    }


    public void startRecord() {
        speakRecord.startRecord();
    }

    public void stopRecord() {
        speakRecord.stopRecord();
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

    public int getPublishStatus() {
        return speakSend.getPublishStatus();
    }

    public int getRecodeStatus() {
        return speakRecord.getRecodeStatus();
    }

    public void setWriteFileCallback(WriteFileCallback writeFileCallback) {
        speakRecord.setWriteFileCallback(writeFileCallback);
    }

    public static class Buider {
        private int publishBitrate = 24 * 1024;
        private SpeakSend speakSend;
        private UdpControlInterface udpControl;
        private String dirpath = null;

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

        public Buider setVoiceDirPath(String dirpath) {
            this.dirpath = dirpath;
            return this;
        }

        public Speak build() {
            return new Speak(publishBitrate, udpControl, speakSend, dirpath);
        }
    }
}
