package com.library.live.stream;

import android.os.Handler;
import android.os.HandlerThread;

import com.library.common.UdpBytes;
import com.library.common.UdpControlInterface;
import com.library.common.VoiceCallback;
import com.library.live.vd.VideoInformationInterface;
import com.library.util.OtherUtil;
import com.library.util.SingleThreadExecutor;
import com.library.util.mLog;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by android1 on 2017/9/23.
 */

public class UdpRecive implements CachingStrategyCallback {
    public static final int RECIVE_STATUS_START = 0;
    public static final int RECIVE_STATUS_STOP = 1;
    private int RECIVE_STATUS = RECIVE_STATUS_STOP;

    private VideoInformationInterface informaitonInterface;
    private UdpControlInterface udpControl = null;
    private VoiceCallback voiceCallback = null;
    private VideoCallback videoCallback = null;
    private WeightCallback weightCallback;

    private boolean ismysocket = false;//用于判断是否需要销毁socket
    private DatagramSocket socket = null;
    private DatagramPacket packetreceive;
    private int udpPacketCacheMin = 3;//udp包最小缓存数量，用于udp包排序

    private LinkedList<UdpBytes> videoList = new LinkedList<>();
    private LinkedList<UdpBytes> voiceList = new LinkedList<>();
    private int videoUdpPacketMin = 70;
    private boolean achieveVideoUdpPacketMin = true;

    private Strategy strategy;

    private HandlerThread handlerUdpThread;
    private Handler udpHandler;

    private SingleThreadExecutor singleThreadExecutor = null;
    private double weight = 0;

    private ArrayBlockingQueue<byte[]> udpQueue = new ArrayBlockingQueue<>(OtherUtil.QueueNum);

    public UdpRecive(int port) {
        try {
            socket = new DatagramSocket(port);
            socket.setReceiveBufferSize(1024 * 1024);
            ismysocket = true;
        } catch (SocketException e) {
            e.printStackTrace();
        }
        byte[] tmpBuf1 = new byte[1024];
        packetreceive = new DatagramPacket(tmpBuf1, tmpBuf1.length);
        strategy = new Strategy();
        strategy.setCachingStrategyCallback(this);
        singleThreadExecutor = new SingleThreadExecutor();
    }

    public UdpRecive() {
        strategy = new Strategy();
        strategy.setCachingStrategyCallback(this);
    }

    public void startRevice() {
        RECIVE_STATUS = RECIVE_STATUS_START;
        videoList.clear();
        voiceList.clear();
        udpQueue.clear();
        frameBuffer.clear();
        strategy.start();
        starReciveUdp();
    }

    /**
     * 接收UDP包
     */
    private void starReciveUdp() {
        if (socket != null) {
            handlerUdpThread = new HandlerThread("Udp");
            handlerUdpThread.start();
            udpHandler = new Handler(handlerUdpThread.getLooper());

            singleThreadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    while (RECIVE_STATUS == RECIVE_STATUS_START) {
                        try {
                            socket.receive(packetreceive);
                            OtherUtil.addQueue(udpQueue, Arrays.copyOfRange(packetreceive.getData(), 0, packetreceive.getLength()));
                            udpHandler.post(udprunnable);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    mLog.log("interrupt_Thread", "关闭接收线程");
                }
            });
        }
    }

    private Runnable udprunnable = new Runnable() {
        @Override
        public void run() {
            while (!udpQueue.isEmpty()) {
                write(udpQueue.poll());
            }
        }
    };

    public void stopRevice() {
        RECIVE_STATUS = RECIVE_STATUS_STOP;
        strategy.stop();
        if (handlerUdpThread != null) {
            udpHandler.removeCallbacksAndMessages(null);
            handlerUdpThread.quitSafely();
        }
    }

    //添加解码数据
    public void write(byte[] bytes) {
        if (RECIVE_STATUS == RECIVE_STATUS_START) {
            if (udpControl != null) {
                bytes = udpControl.Control(bytes, 0, bytes.length);
            }
            UdpBytes udpBytes = new UdpBytes(bytes);

            if (udpBytes.getTag() == (byte) 0x01) {
                if (weight != udpBytes.getWeight()) {
                    weight = udpBytes.getWeight();
                    if (weightCallback != null) {
                        weightCallback.getWeight(weight);
                    }
                }
                addudp(videoList, udpBytes);

                //从排好序的队列中取出数据
                if (videoList.size() >= videoUdpPacketMin) {
//                    mLog.log("videoUdpPacket", "当前数量 " + videoList.size() + " 允许数量 " + videoUdpPacketMin);
                    mosaicVideoFrame(videoList.removeFirst());
                }
            } else if (udpBytes.getTag() == (byte) 0x00) {
                addudp(voiceList, udpBytes);

                if (voiceList.size() >= udpPacketCacheMin) {
                    if (achieveVideoUdpPacketMin) {
                        //音频达到条件后重置视频条件
                        achieveVideoUdpPacketMin = false;
                        videoUdpPacketMin = videoList.size() + 5;
                    }
                    mosaicVoiceFrame(voiceList.removeFirst());
                }
            }
        }
    }

    private int oneFrame = 0;//同帧标识符(使用时间戳当同帧标识)

    private ByteBuffer frameBuffer = ByteBuffer.allocate(1024 * 80);


    /**
     * 将链表数据拼接成帧
     */
    private void mosaicVideoFrame(UdpBytes udpBytes) {
        if (udpBytes.getFrameTag() == (byte) 0x00) {//帧头
            frameBuffer.clear();
            //将帧头（480字节）信息回调给解码器，提取例如SPS，PPS之类的信息
            checkInformation(udpBytes.getData());
            frameBuffer.put(udpBytes.getData());
            oneFrame = udpBytes.getTime();
        } else if (udpBytes.getFrameTag() == (byte) 0x01) {//帧中间
            //因为一帧的时间戳相同，利用时间判断是否为同一帧，加上长度强行限制溢出
            if (udpBytes.getTime() == oneFrame &&
                    ((frameBuffer.position() + udpBytes.getData().length) < frameBuffer.capacity())) {
                frameBuffer.put(udpBytes.getData());
            } else {
                frameBuffer.clear();
                oneFrame = -1;
            }
        } else if (udpBytes.getFrameTag() == (byte) 0x02) {//帧尾
            if (udpBytes.getTime() == oneFrame) {
                frameBuffer.put(udpBytes.getData());
                //完整一帧
                strategy.addVideo(udpBytes.getTime(), Arrays.copyOfRange(frameBuffer.array(), 0, frameBuffer.position()));
            }
        } else if (udpBytes.getFrameTag() == (byte) 0x03) {//独立帧
            //完整一帧
            strategy.addVideo(udpBytes.getTime(), udpBytes.getData());
        }
    }

    /**
     * 检测关键帧，回调配置信息
     */
    private void checkInformation(byte[] frame) {
//        HEVC 00 00 00 01 40 01 0c 01 ff ff 01 60 00 00 03 00 b0 00 00 03 00 00 03 00 3f ac 59 00 00 00 01 42 01 01 01 60 00 00 03 00 b0 00 00 03 00 00 03 00 3f a0 0a 08 07 85 96 bb 93 24 bb 94 82 81 01 01 76 85 09 40 00 00 00 01 44 01 c0 f1 80 04 20 后面 00 00 00 01 26 为帧数据开始，普通帧为 00 00 00 01 02
//        AVC 00 00 00 01 67 42 80 15 da 05 03 da 52 0a 04 04 0d a1 42 6a 00 00 00 01 68 ce 06 e2 后面 00 00 00 01 65 为帧数据开始，普通帧为 41
        if (frame[4] == (byte) 0x67 || frame[4] == (byte) 0x40) {
            if (informaitonInterface != null) {
                informaitonInterface.Information(frame);
            }
        }
    }

    /**
     * 将链表数据拼接成帧
     */
    private void mosaicVoiceFrame(UdpBytes udpBytes) {
        //从一个包中取出5帧数据
        strategy.addVoice(udpBytes.getTime(), udpBytes.getData());
        for (int i = 0; i < 4; i++) {
            udpBytes.nextVoice();
            strategy.addVoice(udpBytes.getTime(), udpBytes.getData());
        }
    }

    /**
     * 有序插入数据
     */
    private void addudp(LinkedList<UdpBytes> list, UdpBytes udpbyte) {
        if (list.size() == 0) {
            list.add(udpbyte);
        } else {
            for (int i = list.size() - 1; i >= 0; i--) {
                if (udpbyte.getNum() > list.get(i).getNum()) {
                    list.add(i + 1, udpbyte);
                    return;
                }
            }
            //序号最小，插在头部
            list.addFirst(udpbyte);
        }
    }

    public void destroy() {
        if (ismysocket) {
            OtherUtil.close(socket);
        }
        stopRevice();
        strategy.destroy();
        if (singleThreadExecutor != null) {
            singleThreadExecutor.shutdownNow();
            singleThreadExecutor = null;
        }
    }

    //控制缓存包数量 用于解决udp乱序
    public void setUdpPacketCacheMin(int udpPacketCacheMin) {
        this.udpPacketCacheMin = udpPacketCacheMin;
    }

    /**
     * 可以通过这个方法获得一些策略参数，根据需要决定是否需要
     */
    public void setOther(int videoFrameCacheMin) {
        strategy.setVideoFrameCacheMin(videoFrameCacheMin);
    }

    /**
     * 缓冲接口，用于PlayerView判断是否正在缓冲，根据需要决定是否需要使用
     */
    public void setIsInBuffer(IsInBuffer isInBuffer) {
        strategy.setIsInBuffer(isInBuffer);
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

    public void setWeightCallback(WeightCallback weightCallback) {
        this.weightCallback = weightCallback;
    }

    public int getReciveStatus() {
        return RECIVE_STATUS;
    }

    @Override
    public void videoStrategy(byte[] video) {
        if (videoCallback != null) {
            //回调给解码器
            videoCallback.videoCallback(video);
        }
    }

    @Override
    public void voiceStrategy(byte[] voice) {
        if (voiceCallback != null) {
            //回调给解码器
            voiceCallback.voiceCallback(voice);
        }
    }
}
