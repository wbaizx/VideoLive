package com.library.talk;

import com.library.talk.coder.SpeakRecord;

/**
 * Created by android1 on 2017/12/23.
 */

public class Speak {
    private SpeakRecord speakRecord;

    public Speak(int collectionBitrate, int publishBitrate, int multiple) {
        speakRecord = new SpeakRecord(collectionBitrate, collectionBitrate, multiple);
    }

    public void star() {
        speakRecord.start();
    }

    public void stop() {
        speakRecord.stop();
    }

    public void destroy() {
        speakRecord.destroy();
    }

    public static class Buider {
        private int collectionBitrate = 64 * 1024;
        private int publishBitrate = 20 * 1024;
        private int multiple = 1;

        public Buider setCollectionBitrate(int collectionBitrate) {
            this.collectionBitrate = collectionBitrate;
            return this;
        }

        public Buider setPublishBitrate(int publishBitrate) {
            this.publishBitrate = publishBitrate;
            return this;
        }

        public Buider setMultiple(int multiple) {
            this.multiple = multiple;
            return this;
        }

        public Speak build() {
            return new Speak(collectionBitrate, publishBitrate, multiple);
        }
    }
}
