package com.library.live.stream;

import com.library.common.UdpControlInterface;

/**
 * Created by android1 on 2017/9/27.
 */

public abstract class BaseSend {
    public static final int PUBLISH_STATUS_START = 0;
    public static final int PUBLISH_STATUS_STOP = 1;
    protected int PUBLISH_STATUS = PUBLISH_STATUS_STOP;

    protected UdpControlInterface udpControl = null;

    public abstract void startsend();

    public abstract void stopsend();

    public abstract void destroy();

    public abstract void addVideo(byte[] video);

    public abstract void addVoice(byte[] voice);

    public void setUdpControl(UdpControlInterface udpControl) {
        this.udpControl = udpControl;
    }

    public abstract void setWeight(double weight);

    public int getPublishStatus() {
        return PUBLISH_STATUS;
    }
}
