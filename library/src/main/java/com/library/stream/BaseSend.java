package com.library.stream;

import com.library.stream.upd.UdpControlInterface;
import com.library.util.data.Value;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by android1 on 2017/9/27.
 */

public abstract class BaseSend {
    protected ArrayBlockingQueue<byte[]> sendFrameQueue = new ArrayBlockingQueue<>(Value.QueueNum);
    protected ArrayBlockingQueue<byte[]> sendAACQueue = new ArrayBlockingQueue<>(Value.QueueNum);
    protected boolean issend = false;
    protected UdpControlInterface udpControl = null;

    public abstract void starsend();

    public abstract void stopsend();

    public abstract void destroy();

    public void addVideo(byte[] video) {
        if (sendFrameQueue.size() >= Value.QueueNum) {
            sendFrameQueue.poll();
        }
        sendFrameQueue.add(video);
    }

    public void addVoice(byte[] voice) {
        if (sendAACQueue.size() >= Value.QueueNum) {
            sendAACQueue.poll();
        }
        sendAACQueue.add(voice);
    }

    public void setUdpControl(UdpControlInterface udpControl) {
        this.udpControl = udpControl;
    }
}
