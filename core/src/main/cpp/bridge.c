#include <jni.h>

#include "libclash.h"

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *unused) {
    initialize();
    return JNI_VERSION_1_6;
}