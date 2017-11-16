package com.library.stream.upd;

import android.util.Log;

import com.library.stream.BaseRecive;
import com.library.util.OtherUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by android1 on 2017/9/23.
 */

public class UdpRecive extends BaseRecive implements CachingStrategyCallback {
    private boolean isrevice = false;
    private DatagramSocket socket = null;
    private DatagramPacket packetreceive;
    private int UdpPacketMax = 50;

    private ArrayBlockingQueue<UdpBytes> videoPacket = new ArrayBlockingQueue<>(OtherUtil.QueueNum);
    private ArrayBlockingQueue<UdpBytes> voicePacket = new ArrayBlockingQueue<>(OtherUtil.QueueNum);

    private LinkedList<UdpBytes> videoList = new LinkedList<>();
    private LinkedList<UdpBytes> voiceList = new LinkedList<>();

    private Strategy strategy;

    public UdpRecive(int port) {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        byte[] tmpBuf1 = new byte[548];
        packetreceive = new DatagramPacket(tmpBuf1, tmpBuf1.length);
        strategy = new Strategy();
        strategy.setCachingStrategyCallback(this);
    }

    public UdpRecive() {
        strategy = new Strategy();
        strategy.setCachingStrategyCallback(this);
    }

    @Override
    public void starRevice() {
        videoList.clear();
        voiceList.clear();
        voicePacket.clear();
        videoPacket.clear();
        isrevice = true;
        strategy.star();
        starReciveUdp();
        starFrame();
        starAAC();
    }

    /*
     接收UDP包
     */
    private void starReciveUdp() {
        //如果socket为空，则需要手动调用write方法送入数据
        if (socket != null) {
            //获取
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isrevice) {
                        try {
                            socket.receive(packetreceive);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //由于后面会使用UdpBytes装下并记录所有数据，所以这里不需要重新拷贝一份也不会造成数据覆盖
                        write(packetreceive.getData());
                    }
                }
            }).start();
        }
    }

    //丢包率计算
//    int vd = 0;
//    int vdnum = 0;
//    int vc = 0;
//    int vcnum = 0;

    //添加解码数据
    public void write(byte[] bytes) {
        if (udpControl != null) {
            bytes = udpControl.Control(bytes);
        }
        UdpBytes udpBytes = new UdpBytes(bytes);
//        if (!udpBytes.isCrcRight()) {
//            Log.d("checkCRC", "--有包错误了");
//        }
        Log.d("UdpPackage_app_size", "--" + videoList.size() + "--" + voiceList.size() + "--" + videoPacket.size() + "--" + voicePacket.size());

        if (udpBytes.getTag() == (byte) 0x01) {
            //按序号有序插入
            addudp(videoList, udpBytes);

            //计算丢包率--------------------------------
//            if (videoList.size() > UdpPacketMax) {
//                if ((udpBytes.getNum() - vd) != 1) {
//                    vdnum += udpBytes.getNum() - vd;
//                    Log.d("UdpLoss", "视频丢包率 :  " + (float) vdnum * (float) 100 / (float) udpBytes.getNum() + "%");
//                }
//                vd = udpBytes.getNum();
//            }
            //--------------------------------

            //添加到待拼接队列
            while (videoList.size() > UdpPacketMax) {
                //由于与读取的并发操作存在问题,所以只能在建一个列表
                OtherUtil.addQueue(videoPacket, videoList.removeFirst());
            }
        } else if (udpBytes.getTag() == (byte) 0x00) {
            //按序号有序插入
            addudp(voiceList, udpBytes);

            //计算丢包率--------------------------------
//            if (voiceList.size() > UdpPacketMax) {
//                if ((udpBytes.getNum() - vc) != 1) {
//                    vcnum += udpBytes.getNum() - vc;
//                    Log.d("UdpLoss", "音频丢包率 :  " + (float) vcnum * (float) 100 / (float) udpBytes.getNum() + "%");
//                }
//                vc = udpBytes.getNum();
//            }
            //--------------------------------

            //添加到待拼接队列
            if (voiceList.size() > UdpPacketMax) {
                //由于与读取的并发操作存在问题,所以只能在建一个列表
                OtherUtil.addQueue(voicePacket, voiceList.removeFirst());
            }
        }
    }

    /*
     将链表数据拼接成帧
     */

    private void starFrame() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isFrameBegin = false;
                UdpBytes udpBytes;
                ByteBuffer frameBuffer = ByteBuffer.allocate(1024 * 60);
                int oldudptime_vd = 0;//记录上一个包的时间
                while (isrevice) {
                    if (videoPacket.size() > 0) {
                        //获取并移除数据
                        udpBytes = videoPacket.poll();
                        if (udpBytes.getFrameTag() == (byte) 0x00) {//帧头
                            isFrameBegin = true;
                            frameBuffer.clear();
                            //将帧头（480字节）信息回调给解码器，提取例如SPS，PPS之类的信息
                            CheckInformation(udpBytes.getData());
                            frameBuffer.put(udpBytes.getData());
                        } else if (udpBytes.getFrameTag() == (byte) 0x01) {//帧中间
                            if (isFrameBegin) {
                                frameBuffer.put(udpBytes.getData());
                            }
                        } else if (udpBytes.getFrameTag() == (byte) 0x02) {//帧尾
                            if (isFrameBegin) {
                                frameBuffer.put(udpBytes.getData());
                                byte[] frame = new byte[frameBuffer.position()];
                                System.arraycopy(frameBuffer.array(), 0, frame, 0, frameBuffer.position());
                                strategy.addVideo(udpBytes.getTime() - oldudptime_vd, udpBytes.getTime(), frame);
                                oldudptime_vd = udpBytes.getTime();
                                isFrameBegin = false;
                                frameBuffer.clear();
                            }
                        } else if (udpBytes.getFrameTag() == (byte) 0x03) {//独立帧
                            strategy.addVideo(udpBytes.getTime() - oldudptime_vd, udpBytes.getTime(), udpBytes.getData());
                            oldudptime_vd = udpBytes.getTime();
                            isFrameBegin = false;
                            frameBuffer.clear();
                        }
                    } else {
                        OtherUtil.sleepShortTime();
                    }
                }
            }
        }).start();
    }

    /*
    检测关键帧，回调配置信息
     */
    private void CheckInformation(byte[] frame) {
//        HEVC 00 00 00 01 40 01 0c 01 ff ff 01 60 00 00 03 00 b0 00 00 03 00 00 03 00 3f ac 59 00 00 00 01 42 01 01 01 60 00 00 03 00 b0 00 00 03 00 00 03 00 3f a0 0a 08 07 85 96 bb 93 24 bb 94 82 81 01 01 76 85 09 40 00 00 00 01 44 01 c0 f1 80 04 20 后面 00 00 00 01 26 为帧数据开始，普通帧为 00 00 00 01 02
//        AVC 00 00 00 01 67 42 80 15 da 05 03 da 52 0a 04 04 0d a1 42 6a 00 00 00 01 68 ce 06 e2 后面 00 00 00 01 65 为帧数据开始，普通帧为 41
        if (frame[4] == (byte) 0x67 || frame[4] == (byte) 0x40) {
            getInformation(frame);
        }
    }


    /*
    将链表数据拼接成AAC帧
     */
    private void starAAC() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int oldudptime_vc = 0;//记录上一个包的时间
                UdpBytes udpBytes;
                while (isrevice) {
                    if (voicePacket.size() > 0) {
                        udpBytes = voicePacket.poll();
                        strategy.addVoice(udpBytes.getTime() - oldudptime_vc, udpBytes.getTime(), udpBytes.getData());
                        oldudptime_vc = udpBytes.getTime();
                    } else {
                        OtherUtil.sleepShortTime();
                    }
                }
            }
        }).start();
    }

    /*
    有序插入数据
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

    @Override
    public void stopRevice() {
        isrevice = false;
        strategy.stop();
    }

    @Override
    public void destroy() {
        isrevice = false;
        strategy.destroy();
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    @Override
    public void videoStrategy(byte[] video) {
        if (videoCallback != null) {
            //通过接口回调给解码器
            videoCallback.videoCallback(video);
        }
    }

    @Override
    public void voiceStrategy(byte[] voice) {
        if (voiceCallback != null) {
            //通过接口回调给解码器
            voiceCallback.voiceCallback(voice);
        }
    }
}
