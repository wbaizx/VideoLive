package com.library.util;

import android.graphics.ImageFormat;
import android.media.Image;
import android.util.Size;

import java.nio.ByteBuffer;


public class ImagUtil {
    static {
        System.loadLibrary("liveudpnative");
    }

    public static native int test();

    /*
   YUV_420_888转换为NV21或者I420
    */
    public static final int COLOR_FormatI420 = 1;
    public static final int COLOR_FormatNV21 = 2;

    public static byte[] YUV420888toNV21(Image image, int colorFormat) {
        int width = image.getCropRect().width();
        int height = image.getCropRect().height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(image.getFormat()) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (int) (width * height * 1.25);
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height;
                        outputStride = 2;
                    }
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (image.getCropRect().top >> shift) + pixelStride * (image.getCropRect().left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
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
    NV21转换为NV12
     */
    public static byte[] NV21ToNV12(byte[] nv21, int width, int height) {
        byte[] input = new byte[width * height * 3 / 2];
        System.arraycopy(nv21, 0, input, 0, width * height);
        for (int j = 0; j < width * height / 2; j += 2) {
            input[width * height + j - 1] = nv21[j + width * height];
            input[width * height + j] = nv21[j + width * height - 1];
        }
        return input;
    }

    /*
    后置摄像头90度
     */
    public static byte[] rotateYUV90(byte[] data, Size size) {
        byte[] yuv = new byte[size.getWidth() * size.getHeight() * 3 / 2];
        // Rotate the Y luma
        int i = 0;
        for (int x = 0; x < size.getWidth(); x++) {
            for (int y = size.getHeight() - 1; y >= 0; y--) {
                yuv[i] = data[y * size.getWidth() + x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = size.getWidth() * size.getHeight() * 3 / 2 - 1;
        for (int x = size.getWidth() - 1; x > 0; x = x - 2) {
            for (int y = 0; y < size.getHeight() / 2; y++) {
                yuv[i] = data[(size.getWidth() * size.getHeight()) + (y * size.getWidth()) + x];
                i--;
                yuv[i] = data[(size.getWidth() * size.getHeight()) + (y * size.getWidth()) + (x - 1)];
                i--;
            }
        }
        return yuv;
    }

    /*
    前置摄像头270度&&镜像
     */
    public static byte[] rotateYUV270AndMirror(byte[] data, Size size) {
        byte[] yuv = new byte[size.getWidth() * size.getHeight() * 3 / 2];
        // Rotate and mirror the Y luma
        int i = 0;
        int maxY = 0;
        for (int x = size.getWidth() - 1; x >= 0; x--) {
            maxY = size.getWidth() * (size.getHeight() - 1) + x * 2;
            for (int y = 0; y < size.getHeight(); y++) {
                yuv[i] = data[maxY - (y * size.getWidth() + x)];
                i++;
            }
        }
        // Rotate and mirror the U and V color components
        int uvSize = size.getWidth() * size.getHeight();
        i = uvSize;
        int maxUV = 0;
        for (int x = size.getWidth() - 1; x > 0; x = x - 2) {
            maxUV = size.getWidth() * (size.getHeight() / 2 - 1) + x * 2 + uvSize;
            for (int y = 0; y < size.getHeight() / 2; y++) {
                yuv[i] = data[maxUV - 2 - (y * size.getWidth() + x - 1)];
                i++;
                yuv[i] = data[maxUV - (y * size.getWidth() + x)];
                i++;
            }
        }
        return yuv;
    }

    /*
    前置摄像头270度
     */
    public static byte[] rotateYUV270(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate the Y luma
        int i = 0;
        for (int x = imageWidth - 1; x >= 0; x--) {
            for (int y = 0; y < imageHeight; y++) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }// Rotate the U and V color components
        i = imageWidth * imageHeight;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i++;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i++;
            }
        }
        return yuv;
    }

}
