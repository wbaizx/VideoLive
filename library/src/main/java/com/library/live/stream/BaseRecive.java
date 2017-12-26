package com.library.live.stream;

import com.library.common.VoiceCallback;
import com.library.live.vd.VideoInformationInterface;
import com.library.common.UdpControlInterface;

/**
 * Created by android1 on 2017/9/27.
 */

public abstract class BaseRecive {
    protected VideoInformationInterface informaitonInterface;
    protected UdpControlInterface udpControl = null;
    protected VoiceCallback voiceCallback = null;
    protected VideoCallback videoCallback = null;
    protected int UdpPacketMin = 5;

    public abstract void startRevice();

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

    //控制缓存包数量 用于解决udp乱序
    public void setUdpPacketCacheMin(int udpPacketCacheMin) {
        UdpPacketMin = udpPacketCacheMin;
    }

    /*
    子类可以通过这个方法获得一些策略参数，根据需要决定是否使用,
    参数为:视频帧达到播放条件的缓存帧数
     */
    public abstract void setOther(int videoFrameCacheMin);

    /*
    缓冲接口，用于PlayerView判断是否正在缓冲，根据需要决定是否需要使用
     */
    public abstract void setIsInBuffer(IsInBuffer isInBuffer);
}
