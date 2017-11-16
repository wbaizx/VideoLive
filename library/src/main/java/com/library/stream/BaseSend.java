package com.library.stream;

import com.library.util.OtherUtil;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by android1 on 2017/9/27.
 */

public abstract class BaseSend {
    protected ArrayBlockingQueue<byte[]> sendFrameQueue = new ArrayBlockingQueue<>(OtherUtil.QueueNum);
    protected ArrayBlockingQueue<byte[]> sendAACQueue = new ArrayBlockingQueue<>(OtherUtil.QueueNum);
    protected boolean issend = false;
    protected UdpControlInterface udpControl = null;

    public abstract void starsend();

    public abstract void stopsend();

    public abstract void destroy();

    public void addVideo(byte[] video) {
        OtherUtil.addQueue(sendFrameQueue, video);
    }

    public void addVoice(byte[] voice) {
        OtherUtil.addQueue(sendAACQueue, voice);
    }

    public void setUdpControl(UdpControlInterface udpControl) {
        this.udpControl = udpControl;
    }
}
