package com.library.stream.upd;

import com.library.util.data.ByteTurn;

/**
 * UDP协议内容：
 * 1字节 音视频tag  0音频 1视频
 * 1字节 帧标记tag  0帧头 1帧中间 2帧尾 3独立帧
 * 2字节 内容长度
 * 4字节 包序号
 * 4字节 时间
 * <p>
 * 数据内容
 */
public class UdpBytes {
    private byte[] data;
    private int num;
    private int time;

    public UdpBytes(byte[] data) {
        this.data = data;
        num = ByteTurn.byte_to_int(new byte[]{data[4], data[5], data[6], data[7]});
        time = ByteTurn.byte_to_int(new byte[]{data[8], data[9], data[10], data[11]});
    }

    public int getNum() {
        return num;
    }

    public int getTime() {
        return time;
    }

    public int getFrameTag() {
        return data[1];
    }

    public int getTag() {
        return data[0];
    }

    public byte[] getData() {
        //删除udp头
        byte[] result = new byte[ByteTurn.byte_to_short(data[2], data[3])];
        System.arraycopy(data, 12, result, 0, result.length);
        return result;
    }
}
