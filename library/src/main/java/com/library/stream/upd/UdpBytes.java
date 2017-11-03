package com.library.stream.upd;

import com.library.util.data.ByteTurn;
import com.library.util.data.Crc;

/**
 * UDP协议内容：
 * 1字节 音视频tag  0音频 1视频
 * 1字节 帧标记tag  0帧头 1帧中间 2帧尾 3独立帧
 * 2字节 内容长度（只包含纯数据部分）
 * 4字节 包序号
 * 4字节 时间戳
 * 4字节 CRC校验码（只校验纯数据部分）
 * length 数据内容
 */
public class UdpBytes {
    private byte[] bytes;
    private byte[] data;//纯数据
    private int num;
    private int time;

    public UdpBytes(byte[] bytes) {
        this.bytes = bytes;
        num = ByteTurn.byte_to_int(bytes[4], bytes[5], bytes[6], bytes[7]);
        time = ByteTurn.byte_to_int(bytes[8], bytes[9], bytes[10], bytes[11]);

        int length = ByteTurn.byte_to_short(bytes[2], bytes[3]);
        data = new byte[length];
        System.arraycopy(bytes, 16, data, 0, length);//16是数据前面协议字节长度
    }

    public int getNum() {
        return num;
    }

    public int getTime() {
        return time;
    }

    public int getFrameTag() {
        return bytes[1];
    }

    public int getTag() {
        return bytes[0];
    }

    public boolean isCrcRight() {
        return Crc.isCrcRight(data, ByteTurn.byte_to_int(bytes[12], bytes[13], bytes[14], bytes[15]));
    }

    public byte[] getData() {
        return data;
    }
}
