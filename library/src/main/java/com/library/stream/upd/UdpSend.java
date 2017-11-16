package com.library.stream.upd;

import android.util.Log;

import com.library.stream.BaseSend;
import com.library.util.OtherUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by android1 on 2017/9/25.
 */

public class UdpSend extends BaseSend {
    private DatagramSocket socket = null;
    private DatagramPacket packetsendPush;
    private int voiceNum = 0;
    private int videoNum = 0;
    private final int sendUdplength = 480;//视频包长度固定480
    private ByteBuffer buffvideo = ByteBuffer.allocate(548);
    private ByteBuffer buffvoice = ByteBuffer.allocate(548);
    private ArrayBlockingQueue<byte[]> push = new ArrayBlockingQueue<>(OtherUtil.QueueNum);
    private boolean ismysocket = false;//用于判断是否需要销毁socket

    public UdpSend(String ip, int port) {
        try {
            socket = new DatagramSocket(port);
            packetsendPush = new DatagramPacket(new byte[10], 10, InetAddress.getByName(ip), port);
            ismysocket = true;
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public UdpSend(DatagramSocket socket, String ip, int port) {
        this.socket = socket;
        try {
            packetsendPush = new DatagramPacket(new byte[10], 10, InetAddress.getByName(ip), port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        ismysocket = false;
    }

    @Override
    public void starsend() {
        issend = true;
        push.clear();
        sendAACQueue.clear();
        sendFrameQueue.clear();
        //视频发送
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (issend) {
                    if (sendFrameQueue.size() > 0) {
                        writeVideo(sendFrameQueue.poll());
                    } else {
                        try {
                            Thread.sleep(OtherUtil.sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();

        //音频发送
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (issend) {
                    if (sendAACQueue.size() > 0) {
                        writeVoice(sendAACQueue.poll());
                    } else {
                        try {
                            Thread.sleep(OtherUtil.sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();

        //socket发送
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (issend) {
                    if (push.size() > 0) {
                        packetsendPush.setData(push.poll());
                        try {
                            socket.send(packetsendPush);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    @Override
    public void stopsend() {
        issend = false;
    }

    @Override
    public void destroy() {
        issend = false;
        if (ismysocket) {
            socket.close();
            socket = null;
        }
    }

    /*
    发送视频
     */
    public void writeVideo(byte[] poll) {
        byte[] pushBytes;
        //当前截取位置
        int nowPosition = 0;
        //是否首次进入
        boolean isOne = true;
        //记录时间值
        int time_vd_vaule = OtherUtil.getTime();

        while ((poll.length - nowPosition) >= sendUdplength) {
            buffvideo.put((byte) 1);//视频TAG
            if (isOne) {
                buffvideo.put((byte) 0);//起始帧
            } else {
                buffvideo.put((byte) 1);//中间帧
            }
            buffvideo.putShort((short) sendUdplength);//长度
            buffvideo.putInt(videoNum++);//序号
            buffvideo.putInt(time_vd_vaule);//时戳
//            buffvideo.putInt(Crc.getCrcInt(poll, nowPosition, sendUdplength));//CRC校验位
            buffvideo.putInt(0);//CRC校验位暂时关闭，用0填充
            buffvideo.put(poll, nowPosition, sendUdplength);

            pushBytes = new byte[buffvideo.position()];
            System.arraycopy(buffvideo.array(), 0, pushBytes, 0, buffvideo.position());

            if (udpControl != null) {
                //如果自定义UPD发送
                pushAdd(udpControl.Control(pushBytes));
            } else {
                pushAdd(pushBytes);
            }
            isOne = false;
            buffvideo.clear();
            nowPosition += sendUdplength;
        }
        if ((poll.length - nowPosition) > 0) {
            buffvideo.put((byte) 1);//视频TAG
            if (isOne) {
                buffvideo.put((byte) 3);//完整帧
            } else {
                buffvideo.put((byte) 2);//结束帧
            }
            buffvideo.putShort((short) (poll.length - nowPosition));
            buffvideo.putInt(videoNum++);//序号
            buffvideo.putInt(time_vd_vaule);//时戳
//            buffvideo.putInt(Crc.getCrcInt(poll, nowPosition, poll.length - nowPosition));//CRC校验位
            buffvideo.putInt(0);//CRC校验位暂时关闭，用0填充
            buffvideo.put(poll, nowPosition, poll.length - nowPosition);

            pushBytes = new byte[buffvideo.position()];
            System.arraycopy(buffvideo.array(), 0, pushBytes, 0, buffvideo.position());
            if (udpControl != null) {
                //如果自定义UPD发送
                pushAdd(udpControl.Control(pushBytes));
            } else {
                pushAdd(pushBytes);
            }
            buffvideo.clear();
        }
    }

    /*
    发送音频
     */
    public void writeVoice(byte[] poll) {
        buffvoice.put((byte) 0);//音频TAG
        buffvoice.put((byte) 3);//完整帧
        buffvoice.putShort((short) poll.length);//长度
        buffvoice.putInt(voiceNum++);//序号
        buffvoice.putInt(OtherUtil.getTime());//时戳
//        buffvoice.putInt(Crc.getCrcInt(poll, 0, poll.length));//CRC校验位
        buffvoice.putInt(0);//CRC校验位暂时关闭，用0填充
        buffvoice.put(poll);//数据

        byte[] pushBytes = new byte[buffvoice.position()];
        System.arraycopy(buffvoice.array(), 0, pushBytes, 0, buffvoice.position());
        if (udpControl != null) {
            //如果自定义UPD发送
            pushAdd(udpControl.Control(pushBytes));
        } else {
            pushAdd(pushBytes);
        }
        buffvoice.clear();
    }

    private void pushAdd(byte[] pushBytes) {
        Log.d("UdpPackage_app_size", "--" + push.size());
        if (push.size() >= OtherUtil.QueueNum - 1) {
            push.poll();
        }
        push.add(pushBytes);
    }

}
