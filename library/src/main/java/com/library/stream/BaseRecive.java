package com.library.stream;

import com.library.vd.VideoInformationInterface;

/**
 * Created by android1 on 2017/9/27.
 */

public abstract class BaseRecive {
    protected VideoInformationInterface informaitonInterface;
    protected UdpControlInterface udpControl = null;
    protected VoiceCallback voiceCallback = null;
    protected VideoCallback videoCallback = null;

    public abstract void starRevice();

    public abstract void stopRevice();

    public abstract void destroy();

    public void getInformation(byte[] important) {
        informaitonInterface.Information(important);
    }

    public void setInformaitonInterface(VideoInformationInterface informaitonInterface) {
        this.informaitonInterface = informaitonInterface;
    }

    public void setUdpControl(UdpControlInterface udpControl) {
        this.udpControl = udpControl;
    }

    public void setVoiceCallback(VoiceCallback voiceCallback) {
        this.voiceCallback = voiceCallback;
    }

    public void setVideoCallback(VideoCallback videoCallback) {
        this.videoCallback = videoCallback;
    }
}
