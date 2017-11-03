package com.library.stream.upd;

import android.util.Log;

import com.library.stream.BaseRecive;
import com.library.util.data.ByteTurn;
import com.library.util.data.Value;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by android1 on 2017/9/23.
 */

public class UdpRecive extends BaseRecive {
    private DatagramSocket socket = null;
    private DatagramPacket packetreceive;
    private int videoUdpLength = 80;

    private ArrayBlockingQueue<UdpBytes> videoPacket = new ArrayBlockingQueue<>(Value.QueueNum);
    private ArrayBlockingQueue<UdpBytes> voicePacket = new ArrayBlockingQueue<>(Value.QueueNum);

    private LinkedList<UdpBytes> videoList = new LinkedList<>();
    private LinkedList<UdpBytes> voiceList = new LinkedList<>();

    public UdpRecive() {
        if (Value.IP != null) {
            try {
                socket = new DatagramSocket(Value.PORT);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            byte[] tmpBuf1 = new byte[548];
            packetreceive = new DatagramPacket(tmpBuf1, tmpBuf1.length);
        }
    }

    @Override
    public void starRevice() {
        videoList.clear();
        voiceList.clear();
        voicePacket.clear();
        reciveAACQueue.clear();
        videoPacket.clear();
        reciveFrameQueue.clear();
        isrevice = true;
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
                    byte[] bytes;
                    while (isrevice) {
                        try {
                            socket.receive(packetreceive);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        bytes = new byte[548];
                        System.arraycopy(packetreceive.getData(), 0, bytes, 0, 548);
                        write(bytes);
                    }
                }
            }).start();
        }
    }

    //丢包率计算
    int vd = 0;
    int vdnum = 0;
    int vc = 0;
    int vcnum = 0;

    //添加解码数据
    public void write(byte[] bytes) {
        if (udpControl != null) {
            bytes = udpControl.Control(bytes);
        }
        UdpBytes udpBytes = new UdpBytes(bytes);
        Log.d("UdpPackage_app_size", "--" + videoList.size()
                + "--" + voiceList.size()
                + "--" + videoPacket.size()
                + "--" + voicePacket.size()
                + "--" + reciveFrameQueue.size()
                + "--" + reciveAACQueue.size());

        if (udpBytes.getTag() == (byte) 0x01) {
            //按序号有序插入
            addudp(videoList, udpBytes);

            //计算丢包率--------------------------------
            if (videoList.size() > 80) {
                if ((udpBytes.getNum() - vd) != 1) {
                    vdnum += udpBytes.getNum() - vd;
                    Log.d("UdpLoss", "视频丢包率 :  " + (float) vdnum * (float) 100 / (float) udpBytes.getNum() + "%");
                }
                vd = udpBytes.getNum();
            }
            //--------------------------------

            //添加到待拼接队列
            while (videoList.size() > videoUdpLength) {
                //由于与读取的并发操作存在问题,所以只能在建一个列表
                if (videoPacket.size() >= Value.QueueNum) {
                    videoPacket.poll();
                }
                videoPacket.add(videoList.removeFirst());
            }
        } else if (udpBytes.getTag() == (byte) 0x00) {
            //按序号有序插入
            addudp(voiceList, udpBytes);

            //计算丢包率--------------------------------
            if (voiceList.size() > 30) {
                if ((udpBytes.getNum() - vc) != 1) {
                    vcnum += udpBytes.getNum() - vc;
                    Log.d("UdpLoss", "音频丢包率 :  " + (float) vcnum * (float) 100 / (float) udpBytes.getNum() + "%");
                }
                vc = udpBytes.getNum();
            }
            //--------------------------------

            //添加到待拼接队列
            if (voiceList.size() > 30) {
                //由于与读取的并发操作存在问题,所以只能在建一个列表
                if (voicePacket.size() >= Value.QueueNum) {
                    voicePacket.poll();
                }
                voicePacket.add(voiceList.removeFirst());
            }
        }
    }

    int oldudptime_vd = 0;//记录上一个帧的时间

    /*
     将链表数据拼接成帧
     */
    private void starFrame() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isFrameBegin = false;
                byte[] frame = null;
                UdpBytes udpBytes;
                int oldtime_vd = 0;//记录上一次拼接时间
                while (isrevice) {
                    if (videoPacket.size() > 0) {
                        //获取并移除数据
                        udpBytes = videoPacket.poll();
                        if (udpBytes.getFrameTag() == (byte) 0x00 || udpBytes.getFrameTag() == (byte) 0x03) {
                            if ((Value.getTime() - oldtime_vd) < (udpBytes.getTime() - oldudptime_vd)) {
                                try {
                                    Thread.sleep(Math.min(70,
                                            Math.max(0,
                                                    (udpBytes.getTime() - oldudptime_vd) - (Value.getTime() - oldtime_vd))));
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            //根据包堆积数量控制播放速率
                            oldtime_vd = controlTiem(videoPacket.size());
                            oldudptime_vd = udpBytes.getTime();
                        }

                        if (udpBytes.getFrameTag() == (byte) 0x00) {//帧头
                            isFrameBegin = true;
                            frame = udpBytes.getData();
                            //将帧头（480字节）信息回调给解码器，提取例如SPS，PPS之类的信息
                            CheckInformation(frame);
                        } else if (udpBytes.getFrameTag() == (byte) 0x01) {//帧中间
                            if (isFrameBegin) {
                                frame = ByteTurn.byte_add(frame, udpBytes.getData());
                            }
                        } else if (udpBytes.getFrameTag() == (byte) 0x02) {//帧尾
                            if (isFrameBegin) {
                                frame = ByteTurn.byte_add(frame, udpBytes.getData());
                                if (reciveFrameQueue.size() >= Value.QueueNum) {
                                    reciveFrameQueue.poll();
                                }
                                reciveFrameQueue.add(frame);
                                isFrameBegin = false;
                            }
                        } else if (udpBytes.getFrameTag() == (byte) 0x03) {//独立帧
                            if (reciveFrameQueue.size() >= Value.QueueNum) {
                                reciveFrameQueue.poll();
                            }
                            reciveFrameQueue.add(udpBytes.getData());
                            isFrameBegin = false;
                        }
                    }
                }
            }
        }).start();
    }

    /*
    检测关键帧，回调配置信息
     */
    private void CheckInformation(byte[] frame) {
//        HEVC[00 00 00 01 40 01 0c 01 ff ff 01 60 00 00 03 00 b0 00 00 03 00 00 03 00 3f ac 59 00 00 00 01 42 01 01 01 60 00 00 03 00 b0 00 00 03 00 00 03 00 3f a0 0a 08 07 85 96 bb 93 24 bb 94 82 81 01 01 76 85 09 40 00 00 00 01 44 01 c0 f1 80 04 20]后面00 00 00 01 26为帧数据开始，普通帧为00 00 00 01 02
//        AVC 00 00 00 01 67 42 80 15 da 05 03 da 52 0a 04 04 0d a1 42 6a 00 00 00 01 68 ce 06 e2后面00 00 00 01 65为帧数据开始，普通帧为41
        if (frame[4] == (byte) 0x67 || frame[4] == (byte) 0x40) {
            getInformation(frame);
        }
    }

    int oldudptime_vc = 0;//记录上一个包的时间

    /*
    将链表数据拼接成AAC帧
     */
    private void starAAC() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                UdpBytes udpBytes;
                int oldtime_vc = 0;//记录上一次拼接时间
                while (isrevice) {
                    if (voicePacket.size() > 0) {
                        udpBytes = voicePacket.poll();
                        //计算时间戳
                        if ((Value.getTime() - oldtime_vc) < (udpBytes.getTime() - oldudptime_vc)) {
                            try {
                                //使用绝对值防止偶尔因线程问题导致时间增加而变成负数，如果出现问题一般误差1ms左右
                                Thread.sleep(Math.min(70,
                                        Math.max(0,
                                                (udpBytes.getTime() - oldudptime_vc) - (Value.getTime() - oldtime_vc))));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        //根据包堆积数量控制播放速率
                        oldtime_vc = controlTiem(voicePacket.size());
                        oldudptime_vc = udpBytes.getTime();
                        //控制音视频同步
                        controlSynchronization();

                        if (udpBytes.getFrameTag() == (byte) 0x03) {//独立音频帧
                            if (reciveAACQueue.size() >= Value.QueueNum) {
                                reciveAACQueue.poll();
                            }
                            reciveAACQueue.add(udpBytes.getData());
                        }
                    }
                }
            }
        }).start();
    }

    //控制音视频同步
    private void controlSynchronization() {
        int value = oldudptime_vc - oldudptime_vd;
        if (value > 100) {
            //音频快了
            if (videoUdpLength > 40) {
                videoUdpLength = videoUdpLength - 1;
            }
        } else if (value < -100) {
            //视频快了
            if (videoUdpLength < 300) {
                videoUdpLength = videoUdpLength + 1;
            }
        }
    }

    //根据包堆积数量控制播放速率
    private int controlTiem(int size) {
       if (size > 50) {
            return Value.getTime() - 15;
        } else if (size > 40) {
            return Value.getTime() - 10;
        } else if (size > 30) {
            return Value.getTime() - 5;
        } else if (size > 20) {
            return Value.getTime();
        } else if (size > 10) {
            return Value.getTime() + 5;
        } else {
            return Value.getTime() + 10;
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
    }

    @Override
    public void destroy() {
        isrevice = false;
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }
}
