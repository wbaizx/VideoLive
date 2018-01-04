package com.library.common;

import com.library.util.ByteUtil;
import com.library.util.OtherUtil;

import java.util.Arrays;

/**
 * UDP协议内容：
 * <p>
 * UDP头：----
 * 1字节 音视频tag  0音频 1视频
 * 4字节 包序号
 * <p>
 * 视频：----
 * 1字节 帧标记tag  0帧头 1帧中间 2帧尾 3独立帧
 * 1字节 图像比例
 * 4字节 时间戳
 * 2字节 内容长度（纯数据部分）
 * length 数据内容
 * <p>
 * 音频：----
 * 4字节 时间戳
 * 2字节 内容长度（纯数据部分）
 * length 数据内容
 * ......
 * 4字节 时间戳
 * 2字节 内容长度（纯数据部分）
 * length 数据内容
 * ......音频5帧一包，所以5个相同数据段
 */
public class UdpBytes {
    private int tag;
    private int num;
    private byte[] bytes;
    private byte[] data;
    private int time;
    private double weight;

    private int frameTag;
    private int lengthnum;//记录音频偏移长度

    public UdpBytes(byte[] bytes) {
        tag = bytes[0];
        num = ByteUtil.byte_to_int(bytes[1], bytes[2], bytes[3], bytes[4]);

        if (tag == (byte) 0x01) {//视频
            frameTag = bytes[5];
            weight = OtherUtil.getWeight(bytes[6]);
            time = ByteUtil.byte_to_int(bytes[7], bytes[8], bytes[9], bytes[10]);
            //13是视频UDP包头+视频协议字长,data得到数据可能不足一帧视频
            data = Arrays.copyOfRange(bytes, 13, ByteUtil.byte_to_short(bytes[11], bytes[12]) + 13);
        } else if (tag == (byte) 0x00) {//音频
            this.bytes = bytes;
            time = ByteUtil.byte_to_int(bytes[5], bytes[6], bytes[7], bytes[8]);
            //11是音频UDP包头+音频协议字长，data得到的是包中第一帧音频
            data = Arrays.copyOfRange(bytes, 11, ByteUtil.byte_to_short(bytes[9], bytes[10]) + 11);
            lengthnum = data.length + 11;//记录偏移量
        }
    }

    public int getTag() {
        return tag;
    }

    public int getNum() {
        return num;
    }

    public int getTime() {
        return time;
    }

    public byte[] getData() {
        return data;
    }

    //视频帧标记
    public int getFrameTag() {
        return frameTag;
    }

    //图像比
    public double getWeight() {
        return weight;
    }

    //音频定位下一帧
    public void nextVoice() {
        time = ByteUtil.byte_to_int(bytes[lengthnum], bytes[lengthnum + 1], bytes[lengthnum + 2], bytes[lengthnum + 3]);
        //6是音频协议字长，data得到的是包中下一帧音频
        data = Arrays.copyOfRange(bytes, lengthnum + 6, ByteUtil.byte_to_short(bytes[lengthnum + 4], bytes[lengthnum + 5]) + lengthnum + 6);
        lengthnum = lengthnum + data.length + 6;//记录偏移量
    }
}
