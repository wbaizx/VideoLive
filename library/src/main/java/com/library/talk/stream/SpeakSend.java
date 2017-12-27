package com.library.talk.stream;

import com.library.common.UdpControlInterface;
import com.library.util.OtherUtil;
import com.library.util.SingleThreadExecutor;
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

/**
 * Created by android1 on 2017/12/25.
 */

public class SpeakSend {
    private boolean ismysocket = false;//用于判断是否需要销毁socket
    private DatagramSocket socket = null;
    private DatagramPacket packetsendPush = null;
    private SingleThreadExecutor singleThreadExecutor;
    private boolean issend = false;
    private ByteBuffer buffvoice = ByteBuffer.allocate(1024);
    private int voiceSendNum = 0;//控制语音包合并发送，5个包发送一次
    private int voiceNum = 0;//音频序号
    private ArrayBlockingQueue<byte[]> sendQueue = new ArrayBlockingQueue<>(OtherUtil.QueueNum);
    private UdpControlInterface udpControl = null;

    public SpeakSend(String ip, int port) {
        try {
            socket = new DatagramSocket(port);
            socket.setSendBufferSize(1024 * 1024);
            ismysocket = true;
        } catch (SocketException e) {
            e.printStackTrace();
        }
        init(ip, port);
    }


    public SpeakSend(DatagramSocket socket, String ip, int port) {
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
        singleThreadExecutor = new SingleThreadExecutor();
    }

    public void start() {
        if (packetsendPush != null) {
            buffvoice.clear();
            voiceSendNum = 0;
            voiceNum = 0;
            issend = true;
            starsendThread();
        }
    }

    public void startJustSend() {
        if (packetsendPush != null) {
            issend = true;
            starsendThread();
        }
    }

    public void addVoice(byte[] voice) {
        if (issend) {
            writeVoice(voice);
        }
    }

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

    private void addbytes(ByteBuffer buff) {
        if (udpControl != null) {
            //自定义发送
            OtherUtil.addQueue(sendQueue, udpControl.Control(buff.array(), 0, buff.position() - 0));
        } else {
            OtherUtil.addQueue(sendQueue, Arrays.copyOfRange(buff.array(), 0, buff.position()));//复制数组
        }
    }

    /**
     * 对于startJustSend模式,对外提供一个数据输入的方法
     */
    public void addbytes(byte[] voice) {
        if (udpControl != null) {
            //自定义发送
            OtherUtil.addQueue(sendQueue, udpControl.Control(voice, 0, voice.length));
        } else {
            OtherUtil.addQueue(sendQueue, voice);
        }
    }

    /**
     * 真正发送数据
     */
    private void starsendThread() {
        singleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                byte[] data;
                try {
                    while (issend) {
                        data = sendQueue.take();
                        if (data != null) {
                            packetsendPush.setData(data);
                            try {
                                socket.send(packetsendPush);
                            } catch (IOException e) {
                                mLog.log("senderror", "发送失败");
                                e.printStackTrace();
                            }
                            Thread.sleep(1);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mLog.log("interrupt_Thread", "speak关闭发送线程");
            }
        });
    }

    public void stop() {
        issend = false;
    }

    public void destroy() {
        stop();
        if (ismysocket) {
            socket.close();
            socket = null;
        }
        if (singleThreadExecutor != null) {
            singleThreadExecutor.shutdownNow();
            singleThreadExecutor = null;
        }
    }

    public void setUdpControl(UdpControlInterface udpControl) {
        this.udpControl = udpControl;
    }

}
