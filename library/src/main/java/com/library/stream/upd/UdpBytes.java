package com.library.stream.upd;

import com.library.util.ByteUtil;

/**
 * UDP协议内容：
 * 1字节 音视频tag  0音频 1视频
 * 1字节 帧标记tag  0帧头 1帧中间 2帧尾 3独立帧
 * 2字节 内容长度（只包含纯数据部分）
 * 4字节 包序号
 * 4字节 时间戳
 * length 数据内容
 */
public class UdpBytes {
    private byte[] data;//纯数据
    private int num;
    //    private int CRC;
    private int time;
    private int frameTag;
    private int tag;

    public UdpBytes(byte[] bytes) {
        tag = bytes[0];
        frameTag = bytes[1];
        num = ByteUtil.byte_to_int(bytes[4], bytes[5], bytes[6], bytes[7]);
        time = ByteUtil.byte_to_int(bytes[8], bytes[9], bytes[10], bytes[11]);
//        CRC = ByteUtil.byte_to_int(bytes[12], bytes[13], bytes[14], bytes[15]);

        int length = ByteUtil.byte_to_short(bytes[2], bytes[3]);
        data = new byte[length];
        System.arraycopy(bytes, 12, data, 0, length);//12是数据前面协议字节长度，如果修改协议记得修改头长度
    }

    public int getNum() {
        return num;
    }

    public int getTime() {
        return time;
    }

    public int getFrameTag() {
        return frameTag;
    }

    public int getTag() {
        return tag;
    }

//    public boolean isCrcRight() {
//        return OtherUtil.getCrcInt(data, 0, data.length) == CRC;
//    }

    public byte[] getData() {
        return data;
    }
}
