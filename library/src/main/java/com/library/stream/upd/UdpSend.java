package com.library.stream.upd;

import android.util.Log;

import com.library.stream.BaseSend;
import com.library.util.data.Crc;
import com.library.util.data.Value;

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
    private ByteBuffer buffvoice = ByteBuffer.allocate(256);
    private ArrayBlockingQueue<byte[]> push = new ArrayBlockingQueue<>(Value.QueueNum);
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
                            Thread.sleep(Value.sleepTime);
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
                            Thread.sleep(Value.sleepTime);
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
                        packetsendPush.setData(push.poll());//尾部添加CRC校验位2字节
                        try {
                            socket.send(packetsendPush);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            Thread.sleep(Value.sleepTime);
                        } catch (InterruptedException e) {
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
        byte[] bytes;
        byte[] pushBytes;
        boolean isOne = true;
        //记录时间值
        int time_vd_vaule = Value.getTime();

        while (poll.length >= sendUdplength) {
            bytes = new byte[sendUdplength];
            System.arraycopy(poll, 0, bytes, 0, sendUdplength);
            buffvideo.put((byte) 1);//视频TAG
            if (isOne) {
                buffvideo.put((byte) 0);//起始帧
            } else {
                buffvideo.put((byte) 1);//中间帧
            }
            buffvideo.putShort((short) sendUdplength);
            buffvideo.putInt(videoNum++);//序号
            buffvideo.putInt(time_vd_vaule);//时戳
            buffvideo.putInt(Crc.getCrcInt(bytes));//CRC校验位
            buffvideo.put(bytes);

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
            bytes = new byte[poll.length - sendUdplength];
            System.arraycopy(poll, sendUdplength, bytes, 0, poll.length - sendUdplength);
            poll = bytes;
        }
        if (poll.length > 0) {
            buffvideo.put((byte) 1);//视频TAG
            if (isOne) {
                buffvideo.put((byte) 3);//完整帧
            } else {
                buffvideo.put((byte) 2);//结束帧
            }
            buffvideo.putShort((short) poll.length);
            buffvideo.putInt(videoNum++);//序号
            buffvideo.putInt(time_vd_vaule);//时戳
            buffvideo.putInt(Crc.getCrcInt(poll));//CRC校验位
            buffvideo.put(poll);

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
        buffvoice.putInt(Value.getTime());//时戳
        buffvoice.putInt(Crc.getCrcInt(poll));//CRC校验位
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
        if (push.size() >= Value.QueueNum - 1) {
            push.poll();
        }
        push.add(pushBytes);
    }

}
