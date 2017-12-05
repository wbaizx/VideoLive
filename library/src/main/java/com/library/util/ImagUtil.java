package com.library.util;

import android.graphics.ImageFormat;
import android.media.Image;
import android.util.Size;

import java.nio.ByteBuffer;


public class ImagUtil {
    static {
        System.loadLibrary("yuvutil");
    }

    /**
     * YUV数据的基本的处理
     *
     * @param src        原始数据
     * @param width      原始的宽
     * @param height     原始的高
     * @param dst        输出数据
     * @param dst_width  输出的宽
     * @param dst_height 输出的高
     * @param mode       压缩模式。这里为0，1，2，3 速度由快到慢，质量由低到高，一般用0就好了，因为0的速度最快
     * @param degree     旋转的角度，90，180和270三种
     * @param isMirror   是否镜像，一般只有270的时候才需要镜像
     **/
    public static native void compressYUV(byte[] src, int width, int height, byte[] dst, int dst_width, int dst_height, int mode, int degree, boolean isMirror);

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

    /**
     * 将I420转化为NV21
     *
     * @param i420Src 原始I420数据
     * @param nv21Src 转化后的NV21数据
     * @param width   输出的宽
     * @param width   输出的高
     **/
    public static native void yuvI420ToNV21(byte[] i420Src, byte[] nv21Src, int width, int height);


    /*
   YUV_420_888转换为NV12
   将方法中case 1:和case 2:对调可以得到NV21格式
   1280*720处理大约15ms
    */
    public static byte[] YUV420888toNV12(Image image) {
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
                    channelOffset = width * height + 1;
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (image.getCropRect().top >> shift) + pixelStride * (image.getCropRect().left >> shift));
            int length;
            for (int row = 0; row < h; row++) {
                if (pixelStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += pixelStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
        return data;
    }

    /*
    后置摄像头90度
    1280*720处理大约35ms
     */
    public static byte[] rotateYUV90(byte[] data, Size size) {
        int imagSize = size.getWidth() * size.getHeight();
        byte[] yuv = new byte[imagSize * 3 / 2];
        // Rotate the Y luma
        int i = 0;

        int num;
        for (int x = 0; x < size.getWidth(); x++) {
            num = imagSize - size.getWidth();
            for (int y = size.getHeight() - 1; y >= 0; y--) {
                yuv[i++] = data[num + x];
                num -= size.getWidth();
            }
        }
        // Rotate the U and V color components
        i = size.getWidth() * size.getHeight() * 3 / 2 - 1;
        for (int x = size.getWidth() - 1; x > 0; x = x - 2) {
            num = 0;
            for (int y = 0; y < size.getHeight() / 2; y++) {
                yuv[i--] = data[imagSize + num + x];
                yuv[i--] = data[imagSize + num + (x - 1)];
                num += size.getWidth();
            }
        }
        return yuv;
    }

    /*
    前置摄像头270度&&镜像
    1280*720处理大约35ms
     */
    public static byte[] rotateYUV270AndMirror(byte[] data, Size size) {
        int imagSize = size.getWidth() * size.getHeight();
        byte[] yuv = new byte[imagSize * 3 / 2];
        // Rotate and mirror the Y luma
        int i = 0;
        int maxY = 0;
        int num;
        for (int x = size.getWidth() - 1; x >= 0; x--) {
            maxY = imagSize - size.getWidth() + x * 2;
            num = 0;
            for (int y = 0; y < size.getHeight(); y++) {
                yuv[i++] = data[maxY - (num + x)];
                num += size.getWidth();
            }
        }
        // Rotate and mirror the U and V color components
        i = imagSize;
        int maxUV = 0;
        for (int x = size.getWidth() - 1; x > 0; x = x - 2) {
            num = 0;
            maxUV = (imagSize / 2) - size.getWidth() + x * 2 + imagSize;
            for (int y = 0; y < size.getHeight() / 2; y++) {
                yuv[i++] = data[maxUV - 2 - (num + x - 1)];
                yuv[i++] = data[maxUV - (num + x)];
                num += size.getWidth();
            }
        }
        return yuv;
    }
}
