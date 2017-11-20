#include <jni.h>

extern "C"
JNIEXPORT jint JNICALL
Java_com_library_nativec_NativeC_getCrcInt(JNIEnv *env, jclass type, jbyteArray buf_, jint offset,
                                           jint length) {
    jbyte *buf = env->GetByteArrayElements(buf_, NULL);

    // TODO
    int remain = 0;
    jbyte val;
    for (int i = offset; i < offset + length; i++) {
        val = buf[i];
        for (int j = 0; j < 8; j++) {
            if (((val ^ remain) & 0x0001) != 0) {
                remain ^= 0x0810;
                remain >>= 1;
                remain |= 0x8000;
            } else {
                remain >>= 1;
            }
            val >>= 1;
        }
    }


    env->ReleaseByteArrayElements(buf_, buf, 0);
    return remain;
}