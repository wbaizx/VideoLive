package com.library.talk.stream;

import android.os.Handler;
import android.os.HandlerThread;

import com.library.common.UdpBytes;
import com.library.common.UdpControlInterface;
import com.library.common.VoiceCallback;
import com.library.util.OtherUtil;
import com.library.util.SingleThreadExecutor;
import com.library.util.mLog;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by android1 on 2017/12/25.
 */

public class ListenRecive implements ListenCachingStrategyCallback {
    private DatagramSocket socket = null;
    private DatagramPacket packetreceive;
    private SingleThreadExecutor singleThreadExecutor = null;
    private LinkedList<UdpBytes> voiceList = new LinkedList<>();
    private ArrayBlockingQueue<byte[]> udpQueue = new ArrayBlockingQueue<>(OtherUtil.QueueNum);
    private VoiceCallback voiceCallback;
    private HandlerThread handlerListenThread;
    private Handler ListenHandler;
    private boolean isrecive = false;
    private UdpControlInterface udpControl;
    private ListenStrategy strategy;
    private boolean ismysocket = false;//用于判断是否需要销毁socket
    private int UdpPacketMin = 2;

    public ListenRecive(int port) {
        try {
            socket = new DatagramSocket(port);
            socket.setReceiveBufferSize(1024 * 1024);
            ismysocket = true;
        } catch (SocketException e) {
            e.printStackTrace();
        }
        initPacket();
        initStrategy();
    }


    public ListenRecive(DatagramSocket socket) {
        this.socket = socket;
        ismysocket = false;
        initPacket();
        initStrategy();
    }

    public ListenRecive() {
        ismysocket = false;
        initStrategy();
    }

    private void initPacket() {
        byte[] tmpBuf1 = new byte[1024];
        packetreceive = new DatagramPacket(tmpBuf1, tmpBuf1.length);
        singleThreadExecutor = new SingleThreadExecutor();
    }

    private void initStrategy() {
        strategy = new ListenStrategy();
        strategy.setListenCachingStrategyCallback(this);
    }

    public void start() {
        isrecive = true;
        voiceList.clear();
        udpQueue.clear();
        strategy.start();
        starReciveUdp();
    }

    /**
     * 接收UDP包
     */
    private void starReciveUdp() {
        if (socket != null) {
            handlerListenThread = new HandlerThread("Listen");
            handlerListenThread.start();
            ListenHandler = new Handler(handlerListenThread.getLooper());

            singleThreadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    while (isrecive) {
                        try {
                            socket.receive(packetreceive);
                            OtherUtil.addQueue(udpQueue, Arrays.copyOfRange(packetreceive.getData(), 0, packetreceive.getLength()));
                            ListenHandler.post(listenrunnable);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    mLog.log("interrupt_Thread", "ListenRecive关闭线程");
                }
            });
        }
    }

    private Runnable listenrunnable = new Runnable() {
        @Override
        public void run() {
            while (udpQueue.size() > 0) {
                write(udpQueue.poll());
            }
        }
    };


    public void write(byte[] bytes) {
        if (isrecive) {
            if (udpControl != null) {
                bytes = udpControl.Control(bytes, 0, bytes.length - 0);
            }
            UdpBytes udpBytes = new UdpBytes(bytes);
            if (udpBytes.getTag() == (byte) 0x00) {
                if (udpBytes.getNum() == 0) {//如果收到序号0包，清空排序队列
                    voiceList.clear();
                }
                addudp(voiceList, udpBytes);
                if (voiceList.size() >= UdpPacketMin) {
                    mosaicVoiceFrame(voiceList.removeFirst());
                }
            }
        }
    }

    private int oldudptime_vc = 0;//记录上一个包的时间

    /*
     将链表数据拼接成帧
     */
    private void mosaicVoiceFrame(UdpBytes udpBytes) {
        //从一个包中取出5帧数据，交个策略处理
        strategy.addVoice(udpBytes.getTime() - oldudptime_vc, udpBytes.getTime(), udpBytes.getData());
        oldudptime_vc = udpBytes.getTime();
        for (int i = 0; i < 4; i++) {
            udpBytes.nextVoice();//定位下一帧
            strategy.addVoice(udpBytes.getTime() - oldudptime_vc, udpBytes.getTime(), udpBytes.getData());
            oldudptime_vc = udpBytes.getTime();
        }
    }

    /**
     * 有序插入数据
     */
    private void addudp(LinkedList<UdpBytes> list, UdpBytes udpBytes) {
        if (list.size() == 0) {
            list.add(udpBytes);
        } else {
            for (int i = list.size() - 1; i >= 0; i--) {
                if (udpBytes.getNum() > list.get(i).getNum()) {
                    list.add(i + 1, udpBytes);
                    return;
                }
            }
            //序号最小，插在头部
            list.addFirst(udpBytes);
        }
    }

    public void stop() {
        isrecive = false;
        if (handlerListenThread != null) {
            ListenHandler.removeCallbacksAndMessages(null);
            handlerListenThread.quitSafely();
        }
        strategy.stop();
    }

    public void destroy() {
        if (ismysocket) {
            socket.close();
            socket = null;
        }
        stop();
        strategy.destroy();
        if (singleThreadExecutor != null) {
            singleThreadExecutor.shutdownNow();
            singleThreadExecutor = null;
        }
    }

    public void setVoiceCallback(VoiceCallback voiceCallback) {
        this.voiceCallback = voiceCallback;
    }

    public void setUdpControl(UdpControlInterface udpControl) {
        this.udpControl = udpControl;
    }

    @Override
    public void voiceStrategy(byte[] voice) {
        if (voiceCallback != null) {
            voiceCallback.voiceCallback(voice);
        }
    }

    public void setUdpPacketMin(int udpPacketMin) {
        UdpPacketMin = udpPacketMin;
    }

    public void setVoiceFrameCacheMin(int voiceFrameCacheMin) {
        strategy.setVoiceFrameCacheMin(voiceFrameCacheMin);
    }
}
