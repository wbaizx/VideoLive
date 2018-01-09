package com.library.util;

import android.graphics.ImageFormat;
import android.media.Image;

import java.nio.ByteBuffer;


public class ImagUtil {
    static {
        System.loadLibrary("yuvutil");
    }

    /**
     * @param src      原始数据
     * @param width    原始的宽
     * @param height   原始的高
     * @param dst      输出数据
     * @param degree   旋转的角度，90，180和270三种
     * @param isMirror 是否镜像，一般只有270的时候才需要镜像
     */
    public static native void rotateI420(byte[] src, int width, int height, byte[] dst, int degree, boolean isMirror);

    /**
     * @param i420Src 原始I420数据
     * @param nv12Src 转化后的NV12数据
     * @param width   输出的宽
     * @param width   输出的高
     **/
    public static native void yuvI420ToNV12(byte[] i420Src, byte[] nv12Src, int width, int height);

    /**
     * @param i420Src 原始I420数据
     * @param nv21Src 转化后的NV21数据
     * @param width   输出的宽
     * @param width   输出的高
     **/
    public static native void yuvI420ToNV21(byte[] i420Src, byte[] nv21Src, int width, int height);

    /**
     * @param src        原始数据
     * @param width      原始的宽
     * @param height     原始的高
     * @param dst        输出数据
     * @param dst_width  输出的宽
     * @param dst_height 输出的高
     * @param mode       压缩模式。这里为0，1，2，3 速度由快到慢，质量由低到高，一般用0就好了，因为0的速度最快
     */
    public static native void scaleI420(byte[] src, int width, int height, byte[] dst, int dst_width, int dst_height, int mode);

    /**
     * yuv数据的裁剪操作
     *
     * @param src        原始数据
     * @param width      原始的宽
     * @param height     原始的高
     * @param dst        输出数据
     * @param dst_width  输出的宽
     * @param dst_height 输出的高
     * @param left       裁剪的x的开始位置，必须为偶数，否则显示会有问题
     * @param top        裁剪的y的开始位置，必须为偶数，否则显示会有问题
     **/
    public static native void cropYUV(byte[] src, int width, int height, byte[] dst, int dst_width, int dst_height, int left, int top);

    /*
     YUV_420_888转换为I420
     */
    public static byte[] YUV420888toI420(Image image) {
        int width = image.getCropRect().width();
        int height = image.getCropRect().height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(image.getFormat()) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        int channelOffset = 0;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    break;
                case 1:
                    channelOffset = width * height;
                    break;
                case 2:
                    channelOffset = (int) (width * height * 1.25);
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (image.getCropRect().top >> shift) + pixelStride * (image.getCropRect().left >> shift));
            if (pixelStride == 1) {
                int length = w;
                for (int row = 0; row < h; row++) {
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                    if (row < h - 1) {
                        buffer.position(buffer.position() + rowStride - length);
                    }
                }
            } else {
                int length = (w - 1) * pixelStride + 1;
                for (int row = 0; row < h; row++) {
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset++;
                    }
                    if (row < h - 1) {
                        buffer.position(buffer.position() + rowStride - length);
                    }
                }
            }
        }
        return data;
    }
}