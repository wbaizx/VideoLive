#include <jni.h>

extern "C"
JNIEXPORT jint JNICALL
Java_com_library_nativec_NativeC_test(JNIEnv *env, jclass type, jbyteArray buf_, jint offset,
                                      jint length) {
    jbyte *buf = env->GetByteArrayElements(buf_, NULL);

    // TODO

    env->ReleaseByteArrayElements(buf_, buf, 0);
    return 0;
}