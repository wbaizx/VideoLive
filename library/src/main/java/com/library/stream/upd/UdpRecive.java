package com.library.stream.upd;

import android.os.Handler;
import android.os.HandlerThread;

import com.library.stream.BaseRecive;
import com.library.stream.IsInBuffer;
import com.library.util.OtherUtil;

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

public class UdpRecive extends BaseRecive implements CachingStrategyCallback {
    private boolean isrevice = false;
    private DatagramSocket socket = null;
    private DatagramPacket packetreceive;

    private LinkedList<UdpBytes> videoList = new LinkedList<>();
    private LinkedList<UdpBytes> voiceList = new LinkedList<>();

    private Strategy strategy;

    private HandlerThread handlerUdpThread;
    private Handler udpHandler;

    private ArrayBlockingQueue<byte[]> udpQueue = new ArrayBlockingQueue<>(OtherUtil.QueueNum);

    public UdpRecive(int port) {
        try {
            socket = new DatagramSocket(port);
            socket.setReceiveBufferSize(1024 * 1024);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        byte[] tmpBuf1 = new byte[1024];
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
        udpQueue.clear();
        isrevice = true;
        frameBuffer.clear();
        strategy.star();
        starReciveUdp();
    }

    /*
     接收UDP包
     */
    long a = 0;

    private void starReciveUdp() {
        //如果socket为空，则需要手动调用write方法送入数据
        if (socket != null) {

            handlerUdpThread = new HandlerThread("Udp");
            handlerUdpThread.start();
            udpHandler = new Handler(handlerUdpThread.getLooper());

            //获取
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isrevice) {
                        try {
                            socket.receive(packetreceive);
                            OtherUtil.addQueue(udpQueue, Arrays.copyOfRange(packetreceive.getData(), 0, packetreceive.getLength()));
                            udpHandler.post(udprunnable);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }

    private Runnable udprunnable = new Runnable() {
        @Override
        public void run() {
            while (udpQueue.size() > 0) {
                write(udpQueue.poll());
            }
        }
    };

    //丢包率计算
//    int vdnum = 0;
//    int vcnum = 0;

    //添加解码数据
    public void write(byte[] bytes) {
        if (udpControl != null) {
            bytes = udpControl.Control(bytes, 0, bytes.length - 0);
        }
        UdpBytes udpBytes = new UdpBytes(bytes);

        if (udpBytes.getTag() == (byte) 0x01) {
            //按序号有序插入
            addudp(videoList, udpBytes);

            //计算丢包率--------------------------------
//            if ((vdnum % 500) == 0) {//每500个包输出一次
//                Log.d("UdpLoss", "视频丢包率 :  " +
//                        ((float) udpBytes.getNum() - (float) vdnum) * (float) 100 / (float) udpBytes.getNum() + "%");
//            }
//            vdnum++;
            //--------------------------------

            //从排好序的队列中取出数据
            if (videoList.size() > (UdpPacketMin * 5 * 4)) {//视频帧包数量本来就多，并且音频5帧一包，这里可以多存一点，确保策略处理时音频帧比视频帧多
                //由于与读取的并发操作存在问题,这里单线程执行
                mosaicVideoFrame(videoList.removeFirst());
            }
        } else if (udpBytes.getTag() == (byte) 0x00) {
            //按序号有序插入
            addudp(voiceList, udpBytes);

            //计算丢包率--------------------------------
//            if ((vcnum % 50) == 0) {//每50个包输出一次
//                Log.d("UdpLoss", "音频丢包率 :  " +
//                        ((float) udpBytes.getNum() - (float) vcnum) * (float) 100 / (float) udpBytes.getNum() + "%");
//            }
//            vcnum++;
            //--------------------------------

            //从排好序的队列中取出数据
            if (voiceList.size() > UdpPacketMin) {
                //由于与读取的并发操作存在问题,这里单线程执行
                mosaicVoiceFrame(voiceList.removeFirst());
            }
        }
    }

    private int oldudptime_vd = 0;//记录上一个包的时间
    private int oneFrame = 0;//同帧标识符(使用时间戳当同帧标识)
    private ByteBuffer frameBuffer = ByteBuffer.allocate(1024 * 60);

    /*
     将链表数据拼接成帧
     */
    private void mosaicVideoFrame(UdpBytes udpBytes) {
        //获取并移除数据
        if (udpBytes.getFrameTag() == (byte) 0x00) {//帧头
            frameBuffer.clear();
            //将帧头（480字节）信息回调给解码器，提取例如SPS，PPS之类的信息
            CheckInformation(udpBytes.getData());
            frameBuffer.put(udpBytes.getData());
            oneFrame = udpBytes.getTime();
        } else if (udpBytes.getFrameTag() == (byte) 0x01) {//帧中间
            if (udpBytes.getTime() == oneFrame) {//因为一帧的时间戳相同，利用时间判断是否为同一帧
                frameBuffer.put(udpBytes.getData());
            }
        } else if (udpBytes.getFrameTag() == (byte) 0x02) {//帧尾
            if (udpBytes.getTime() == oneFrame) {//因为一帧的时间戳相同，利用时间判断是否为同一帧
                frameBuffer.put(udpBytes.getData());
                //完整一帧，交个策略处理
                strategy.addVideo(udpBytes.getTime() - oldudptime_vd, udpBytes.getTime(),
                        Arrays.copyOfRange(frameBuffer.array(), 0, frameBuffer.position()));
                oldudptime_vd = udpBytes.getTime();
            }
        } else if (udpBytes.getFrameTag() == (byte) 0x03) {//独立帧
            //完整一帧，交个策略处理
            strategy.addVideo(udpBytes.getTime() - oldudptime_vd, udpBytes.getTime(), udpBytes.getData());
            oldudptime_vd = udpBytes.getTime();
        }
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
        if (handlerUdpThread != null) {
            handlerUdpThread.quitSafely();
        }
    }

    @Override
    public void destroy() {
        stopRevice();
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    /*
    可以通过这个方法获得一些策略参数，根据需要决定是否需要,
    3个参数分别为 视频帧达到播放条件的缓存帧数，视频帧缓冲时间，音频帧缓冲时间
     */
    @Override
    public void setOther(int videoFrameCacheMin, int videoCarltontime, int voiceCarltontime) {
        strategy.setVideoFrameCacheMin(videoFrameCacheMin);
        strategy.setVideoCarltontime(videoCarltontime);
        strategy.setVoiceCarltontime(voiceCarltontime);
    }

    @Override
    public void setIsInBuffer(IsInBuffer isInBuffer) {
        strategy.setIsInBuffer(isInBuffer);
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
