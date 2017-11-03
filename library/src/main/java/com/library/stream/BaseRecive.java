package com.library.stream;

import com.library.stream.upd.UdpControlInterface;
import com.library.util.data.Value;
import com.library.vd.VideoInformationInterface;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by android1 on 2017/9/27.
 */

public abstract class BaseRecive {
    protected ArrayBlockingQueue<byte[]> reciveFrameQueue = new ArrayBlockingQueue<>(Value.QueueNum);
    protected ArrayBlockingQueue<byte[]> reciveAACQueue = new ArrayBlockingQueue<>(Value.QueueNum);
    protected boolean isrevice = false;
    protected VideoInformationInterface informaitonInterface;
    protected UdpControlInterface udpControl = null;

    public abstract void starRevice();

    public abstract void stopRevice();

    public abstract void destroy();

    public byte[] getVideo() {
        if (reciveFrameQueue.size() > 0) {
            return reciveFrameQueue.poll();
        }
        return null;
    }

    public byte[] getVoice() {
        if (reciveAACQueue.size() > 0) {
            return reciveAACQueue.poll();
        }
        return null;
    }


    public void getInformation(byte[] important) {
        informaitonInterface.Information(important);
    }

    public void setInformaitonInterface(VideoInformationInterface informaitonInterface) {
        this.informaitonInterface = informaitonInterface;
    }

    public void setUdpControl(UdpControlInterface udpControl) {
        this.udpControl = udpControl;
    }
}
