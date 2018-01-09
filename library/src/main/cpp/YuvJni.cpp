#include <jni.h>
#include <string>
#include "libyuv.h"

void scaleI420(jbyte *src_i420_data, jint width, jint height, jbyte *dst_i420_data, jint dst_width,
               jint dst_height, jint mode) {

    jint src_i420_y_size = width * height;
    jint src_i420_u_size = (width >> 1) * (height >> 1);
    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_i420_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_i420_y_size + src_i420_u_size;

    jint dst_i420_y_size = dst_width * dst_height;
    jint dst_i420_u_size = (dst_width >> 1) * (dst_height >> 1);
    jbyte *dst_i420_y_data = dst_i420_data;
    jbyte *dst_i420_u_data = dst_i420_data + dst_i420_y_size;
    jbyte *dst_i420_v_data = dst_i420_data + dst_i420_y_size + dst_i420_u_size;

    libyuv::I420Scale((const uint8 *) src_i420_y_data, width,
                      (const uint8 *) src_i420_u_data, width >> 1,
                      (const uint8 *) src_i420_v_data, width >> 1,
                      width, height,
                      (uint8 *) dst_i420_y_data, dst_width,
                      (uint8 *) dst_i420_u_data, dst_width >> 1,
                      (uint8 *) dst_i420_v_data, dst_width >> 1,
                      dst_width, dst_height,
                      (libyuv::FilterMode) mode);
}

void rotateI420(jbyte *src_i420_data, jint width, jint height, jbyte *dst_i420_data, jint degree) {
    jint src_i420_y_size = width * height;
    jint src_i420_u_size = (width >> 1) * (height >> 1);

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_i420_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_i420_y_size + src_i420_u_size;

    jbyte *dst_i420_y_data = dst_i420_data;
    jbyte *dst_i420_u_data = dst_i420_data + src_i420_y_size;
    jbyte *dst_i420_v_data = dst_i420_data + src_i420_y_size + src_i420_u_size;

    //要注意这里的width和height在旋转之后是相反的
    libyuv::I420Rotate((const uint8 *) src_i420_y_data, width,
                       (const uint8 *) src_i420_u_data, width >> 1,
                       (const uint8 *) src_i420_v_data, width >> 1,
                       (uint8 *) dst_i420_y_data, height,
                       (uint8 *) dst_i420_u_data, height >> 1,
                       (uint8 *) dst_i420_v_data, height >> 1,
                       width, height,
                       (libyuv::RotationMode) degree);
}

void mirrorI420(jbyte *src_i420_data, jint width, jint height, jbyte *dst_i420_data) {
    jint src_i420_y_size = width * height;
    jint src_i420_u_size = (width >> 1) * (height >> 1);

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_i420_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_i420_y_size + src_i420_u_size;

    jbyte *dst_i420_y_data = dst_i420_data;
    jbyte *dst_i420_u_data = dst_i420_data + src_i420_y_size;
    jbyte *dst_i420_v_data = dst_i420_data + src_i420_y_size + src_i420_u_size;

    libyuv::I420Mirror((const uint8 *) src_i420_y_data, width,
                       (const uint8 *) src_i420_u_data, width >> 1,
                       (const uint8 *) src_i420_v_data, width >> 1,
                       (uint8 *) dst_i420_y_data, width,
                       (uint8 *) dst_i420_u_data, width >> 1,
                       (uint8 *) dst_i420_v_data, width >> 1,
                       width, height);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_library_util_ImagUtil_rotateI420(JNIEnv *env, jclass type, jbyteArray src_, jint width,
                                          jint height, jbyteArray dst_, jint degree,
                                          jboolean isMirror) {
    jbyte *src = env->GetByteArrayElements(src_, NULL);
    jbyte *dst = env->GetByteArrayElements(dst_, NULL);

    // TODO
    if (isMirror) {
        jbyte *rotate_bytes = (jbyte *) malloc(sizeof(jbyte) * width * height * 3 / 2);
        //进行旋转的操作
        rotateI420(src, width, height, rotate_bytes, degree);
        //因为旋转的角度都是90和270，那后面的数据width和height是相反的
        mirrorI420(rotate_bytes, height, width, dst);
        free(rotate_bytes);
    } else {
        rotateI420(src, width, height, dst, degree);
    }

    env->ReleaseByteArrayElements(src_, src, 0);
    env->ReleaseByteArrayElements(dst_, dst, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_library_util_ImagUtil_yuvI420ToNV21(JNIEnv *env, jclass type, jbyteArray i420Src_,
                                             jbyteArray nv21Src_, jint width, jint height) {
    jbyte *src_i420_data = env->GetByteArrayElements(i420Src_, NULL);
    jbyte *src_nv21_data = env->GetByteArrayElements(nv21Src_, NULL);

    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_y_size + src_u_size;

    jbyte *src_nv21_y_data = src_nv21_data;
    jbyte *src_nv21_vu_data = src_nv21_data + src_y_size;

    libyuv::I420ToNV21(
            (const uint8 *) src_i420_y_data, width,
            (const uint8 *) src_i420_u_data, width >> 1,
            (const uint8 *) src_i420_v_data, width >> 1,
            (uint8 *) src_nv21_y_data, width,
            (uint8 *) src_nv21_vu_data, width,
            width, height);

    env->ReleaseByteArrayElements(i420Src_, src_i420_data, 0);
    env->ReleaseByteArrayElements(nv21Src_, src_nv21_data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_library_util_ImagUtil_yuvI420ToNV12(JNIEnv *env, jclass type, jbyteArray i420Src,
                                             jbyteArray nv12Src,
                                             jint width, jint height) {

    jbyte *src_i420_data = env->GetByteArrayElements(i420Src, NULL);
    jbyte *src_nv12_data = env->GetByteArrayElements(nv12Src, NULL);

    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_y_size + src_u_size;

    jbyte *src_nv12_y_data = src_nv12_data;
    jbyte *src_nv12_vu_data = src_nv12_data + src_y_size;

    libyuv::I420ToNV12(
            (const uint8 *) src_i420_y_data, width,
            (const uint8 *) src_i420_u_data, width >> 1,
            (const uint8 *) src_i420_v_data, width >> 1,
            (uint8 *) src_nv12_y_data, width,
            (uint8 *) src_nv12_vu_data, width,
            width, height);

    env->ReleaseByteArrayElements(i420Src, src_i420_data, 0);
    env->ReleaseByteArrayElements(nv12Src, src_nv12_data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_library_util_ImagUtil_scaleI420(JNIEnv *env, jclass type, jbyteArray src_, jint width,
                                         jint height, jbyteArray dst_, jint dst_width,
                                         jint dst_height, jint mode) {
    jbyte *src = env->GetByteArrayElements(src_, NULL);
    jbyte *dst = env->GetByteArrayElements(dst_, NULL);

    // TODO
    scaleI420(src, width, height, dst, dst_width, dst_height, mode);

    env->ReleaseByteArrayElements(src_, src, 0);
    env->ReleaseByteArrayElements(dst_, dst, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_library_util_ImagUtil_cropYUV(JNIEnv *env, jclass type, jbyteArray src_, jint width,
                                       jint height, jbyteArray dst_, jint dst_width,
                                       jint dst_height,
                                       jint left, jint top) {
    //裁剪的区域大小不对
    if (left + dst_width > width || top + dst_height > height) {
        return;
    }

    //left和top必须为偶数，否则显示会有问题
    if (left % 2 != 0 || top % 2 != 0) {
        return;
    }

    jint src_length = env->GetArrayLength(src_);
    jbyte *src_i420_data = env->GetByteArrayElements(src_, NULL);
    jbyte *dst_i420_data = env->GetByteArrayElements(dst_, NULL);


    jint dst_i420_y_size = dst_width * dst_height;
    jint dst_i420_u_size = (dst_width >> 1) * (dst_height >> 1);

    jbyte *dst_i420_y_data = dst_i420_data;
    jbyte *dst_i420_u_data = dst_i420_data + dst_i420_y_size;
    jbyte *dst_i420_v_data = dst_i420_data + dst_i420_y_size + dst_i420_u_size;

    libyuv::ConvertToI420((const uint8 *) src_i420_data, src_length,
                          (uint8 *) dst_i420_y_data, dst_width,
                          (uint8 *) dst_i420_u_data, dst_width >> 1,
                          (uint8 *) dst_i420_v_data, dst_width >> 1,
                          left, top,
                          width, height,
                          dst_width, dst_height,
                          libyuv::kRotate0, libyuv::FOURCC_I420);

    env->ReleaseByteArrayElements(src_, src_i420_data, 0);
    env->ReleaseByteArrayElements(dst_, dst_i420_data, 0);
}
