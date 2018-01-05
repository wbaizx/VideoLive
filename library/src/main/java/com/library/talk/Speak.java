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
    private boolean isStartPublish = false;
    private boolean isStartRecode = false;

    public Speak(int publishBitrate, UdpControlInterface udpControl, SpeakSend speakSend, String path) {
        this.speakSend = speakSend;
        speakSend.setUdpControl(udpControl);
        speakRecord = new SpeakRecord(publishBitrate, speakSend, path);
    }

    public void start() {
        isStartPublish = true;
        isStartRecode = true;
        speakSend.start();
        speakRecord.start();
    }

    public void stop() {
        isStartPublish = false;
        isStartRecode = false;
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
        isStartPublish = false;
        isStartRecode = false;
        speakRecord.destroy();
        speakSend.destroy();
    }

    public boolean isStartPublish() {
        return isStartPublish;
    }

    public boolean isStartRecode() {
        return isStartRecode;
    }

    public static class Buider {
        private int publishBitrate = 24 * 1024;
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
