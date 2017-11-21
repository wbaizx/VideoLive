#include <jni.h>
#include <sys/time.h>

//获取系统时间毫秒
long getCurrentTime() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_library_util_ImagUtil_test(JNIEnv *env, jclass type) {

    // TODO
    return 1055;
}