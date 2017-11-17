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

/**
 * Created by android1 on 2017/9/25.
 */

public class UdpSend extends BaseSend {
    private boolean issend = false;
    private DatagramSocket socket = null;
    private DatagramPacket packetsendPush;
    private int voiceNum = 0;
    private int videoNum = 0;
    private final int sendUdplength = 480;//视频包长度固定480
    private ByteBuffer buffvideo = ByteBuffer.allocate(548);
    private ByteBuffer buffvoice = ByteBuffer.allocate(548);
    private boolean ismysocket = false;//用于判断是否需要销毁socket

    public UdpSend(String ip, int port) {
        try {
            socket = new DatagramSocket(port);
            socket.setSendBufferSize(1024 * 1024 * 5);
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
        byte[] pushBytes;
        //当前截取位置
        int nowPosition = 0;
        //是否首次进入
        boolean isOne = true;
        //记录时间值
        int time_vd_vaule = OtherUtil.getTime();

        while ((video.length - nowPosition) >= sendUdplength) {
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
            buffvideo.put(video, nowPosition, sendUdplength);

            pushBytes = new byte[buffvideo.position()];
            System.arraycopy(buffvideo.array(), 0, pushBytes, 0, buffvideo.position());

            if (udpControl != null) {
                //如果自定义UPD发送
                sendbytes(udpControl.Control(pushBytes));
            } else {
                sendbytes(pushBytes);
            }
            isOne = false;
            buffvideo.clear();
            nowPosition += sendUdplength;
        }
        if ((video.length - nowPosition) > 0) {
            buffvideo.put((byte) 1);//视频TAG
            if (isOne) {
                buffvideo.put((byte) 3);//完整帧
            } else {
                buffvideo.put((byte) 2);//结束帧
            }
            buffvideo.putShort((short) (video.length - nowPosition));
            buffvideo.putInt(videoNum++);//序号
            buffvideo.putInt(time_vd_vaule);//时戳
//            buffvideo.putInt(Crc.getCrcInt(poll, nowPosition, poll.length - nowPosition));//CRC校验位
            buffvideo.putInt(0);//CRC校验位暂时关闭，用0填充
            buffvideo.put(video, nowPosition, video.length - nowPosition);

            pushBytes = new byte[buffvideo.position()];
            System.arraycopy(buffvideo.array(), 0, pushBytes, 0, buffvideo.position());
            if (udpControl != null) {
                //如果自定义UPD发送
                sendbytes(udpControl.Control(pushBytes));
            } else {
                sendbytes(pushBytes);
            }
            buffvideo.clear();
        }
    }

    /*
    发送音频
     */
    public void writeVoice(byte[] voice) {
        buffvoice.put((byte) 0);//音频TAG
        buffvoice.put((byte) 3);//完整帧
        buffvoice.putShort((short) voice.length);//长度
        buffvoice.putInt(voiceNum++);//序号
        buffvoice.putInt(OtherUtil.getTime());//时戳
//        buffvoice.putInt(Crc.getCrcInt(poll, 0, poll.length));//CRC校验位
        buffvoice.putInt(0);//CRC校验位暂时关闭，用0填充
        buffvoice.put(voice);//数据

        byte[] pushBytes = new byte[buffvoice.position()];
        System.arraycopy(buffvoice.array(), 0, pushBytes, 0, buffvoice.position());
        if (udpControl != null) {
            //如果自定义UPD发送
            sendbytes(udpControl.Control(pushBytes));
        } else {
            sendbytes(pushBytes);
        }
        buffvoice.clear();
    }

    /*
    真正发送数据
     */
    private synchronized void sendbytes(byte[] pushBytes) {
        if (issend) {
            packetsendPush.setData(pushBytes);
            try {
                socket.send(packetsendPush);
            } catch (IOException e) {
                Log.d("senderror", "发送失败");
                e.printStackTrace();
            }
        }
    }

}
