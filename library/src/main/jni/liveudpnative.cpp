#include <jni.h>

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_library_nativec_NativeC_test(JNIEnv *env, jclass type, jbyteArray data_, jint imageWidth,
                                      jint imageHeight) {
    jbyte *data = env->GetByteArrayElements(data_, NULL);

    // TODO

    env->ReleaseByteArrayElements(data_, data, 0);
}