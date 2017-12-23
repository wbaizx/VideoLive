package com.library.live.stream.upd;

import com.library.live.stream.BaseSend;
import com.library.util.OtherUtil;
import com.library.util.mLog;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by android1 on 2017/9/25.
 */

public class UdpSend extends BaseSend {
    private boolean issend = false;
    private DatagramSocket socket = null;
    private DatagramPacket packetsendPush = null;
    private int voiceNum = 0;
    private int videoNum = 0;
    private final int sendUdplength = 480;//视频包长度固定480
    private ByteBuffer buffvideo = ByteBuffer.allocate(548);
    private ByteBuffer buffvoice = ByteBuffer.allocate(1024);
    private boolean ismysocket = false;//用于判断是否需要销毁socket

    private ExecutorService executorService = null;

    private ArrayBlockingQueue<byte[]> sendQueue = new ArrayBlockingQueue<>(OtherUtil.QueueNum);

    public UdpSend(String ip, int port) {
        try {
            socket = new DatagramSocket(port);
            socket.setSendBufferSize(1024 * 1024);
            ismysocket = true;
        } catch (SocketException e) {
            e.printStackTrace();
        }
        init(ip, port);
    }


    public UdpSend(DatagramSocket socket, String ip, int port) {
        this.socket = socket;
        ismysocket = false;
        init(ip, port);
    }

    private void init(String ip, int port) {
        try {
            packetsendPush = new DatagramPacket(new byte[10], 10, InetAddress.getByName(ip), port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void starsend() {
        if (packetsendPush != null) {
            buffvoice.clear();
            voiceSendNum = 0;
            issend = true;
            starsendThread();
        }
    }

    @Override
    public void stopsend() {
        issend = false;
    }

    @Override
    public void destroy() {
        stopsend();
        if (ismysocket) {
            socket.close();
            socket = null;
        }
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    @Override
    public void addVideo(byte[] video) {
        if (issend) {
            writeVideo(video);
        }
    }

    @Override
    public void addVoice(byte[] voice) {
        if (issend) {
            writeVoice(voice);
        }
    }

    /*
    发送视频
     */
    public void writeVideo(byte[] video) {
        //当前截取位置
        int nowPosition = 0;
        //是否首次进入
        boolean isOne = true;
        //记录时间值
        int time_vd_vaule = OtherUtil.getTime();

        while ((video.length - nowPosition) > sendUdplength) {
            buffvideo.clear();
            //添加udp头
            buffvideo.put((byte) 1);//视频TAG
            buffvideo.putInt(videoNum++);//序号
            //添加视频头
            if (isOne) {
                buffvideo.put((byte) 0);//起始帧
            } else {
                buffvideo.put((byte) 1);//中间帧
            }
            buffvideo.putInt(time_vd_vaule);//时戳
            buffvideo.putShort((short) sendUdplength);//长度
            //添加视频数据
            buffvideo.put(video, nowPosition, sendUdplength);
            //UPD发送
            addbytes(buffvideo);
            isOne = false;
            nowPosition += sendUdplength;
        }
        if ((video.length - nowPosition) > 0) {
            buffvideo.clear();
            //添加udp头
            buffvideo.put((byte) 1);//视频TAG
            buffvideo.putInt(videoNum++);//序号
            //添加视频头
            if (isOne) {
                buffvideo.put((byte) 3);//完整帧
            } else {
                buffvideo.put((byte) 2);//结束帧
            }
            buffvideo.putInt(time_vd_vaule);//时戳
            buffvideo.putShort((short) (video.length - nowPosition));//长度
            //添加视频数据
            buffvideo.put(video, nowPosition, video.length - nowPosition);
            //UPD发送
            addbytes(buffvideo);
        }
    }

    private int voiceSendNum = 0;//控制语音包合并发送，5个包发送一次

    /*
    发送音频
     */
    public void writeVoice(byte[] voice) {
        if (voiceSendNum == 0) {
            //添加udp头
            buffvoice.put((byte) 0);//音频TAG
            buffvoice.putInt(voiceNum++);//序号
            //添加音频头
            buffvoice.putInt(OtherUtil.getTime());//时戳
            buffvoice.putShort((short) voice.length);//长度
            //添加音频数据
            buffvoice.put(voice);//数据

            voiceSendNum++;
        } else {
            //添加音频头
            buffvoice.putInt(OtherUtil.getTime());//时戳
            buffvoice.putShort((short) voice.length);//长度
            //添加音频数据
            buffvoice.put(voice);//数据
            voiceSendNum++;
        }

        if (voiceSendNum == 5) {
            voiceSendNum = 0;//5帧一包，标志置0
            //UPD发送
            addbytes(buffvoice);
            buffvoice.clear();
        }
    }

    private synchronized void addbytes(ByteBuffer buff) {
        if (udpControl != null) {
            //如果自定义UPD发送
            OtherUtil.addQueue(sendQueue, udpControl.Control(buff.array(), 0, buff.position() - 0));
        } else {
            OtherUtil.addQueue(sendQueue, Arrays.copyOfRange(buff.array(), 0, buff.position()));//复制数组
        }
    }

    /*
    真正发送数据
     */
    private void starsendThread() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                byte[] data;
                while (issend) {
                    if (sendQueue.size() > 0) {
                        data = sendQueue.poll();
                        if (data != null) {
                            packetsendPush.setData(data);
                            try {
                                socket.send(packetsendPush);
                                Thread.sleep(1);
                            } catch (IOException e) {
                                mLog.log("senderror", "发送失败");
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        OtherUtil.sleepShortTime();
                    }
                }
                mLog.log("interrupt_Thread", "关闭发送线程");
            }
        });
    }
}
