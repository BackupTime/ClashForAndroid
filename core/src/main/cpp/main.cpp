#include "main.h"

Master *Master::master = nullptr;

Master::Master(JNIEnv *env) {
    master = this;

    cClashException = env->FindClass("com/github/kr328/clash/core/bridge/ClashException");
    cTraffic = env->FindClass("com/github/kr328/clash/core/model/Traffic");
    cGeneral = env->FindClass("com/github/kr328/clash/core/model/General");
    cCompletableFuture = env->FindClass("java/util/concurrent/CompletableFuture");
    cClashExceptionConstructor = env->GetMethodID(cClashException, "<init>",
                                                  "(Ljava/lang/String;)V");
    cTrafficConstructor = env->GetMethodID(cTraffic, "<init>", "(JJ)V");
    cGeneralConstructor = env->GetMethodID(cGeneral, "<init>", "(Ljava/lang/String;IIII)V");
    cCompletableFutureConstructor = env->GetMethodID(cCompletableFuture, "<init>", "()V");
    mCompletableFutureComplete = env->GetMethodID(cCompletableFuture, "complete",
                                                  "(Ljava/lang/Object;)Z");
    mCompletableFutureCompleteExceptionally = env->GetMethodID(cCompletableFuture,
                                                               "completeExceptionally",
                                                               "(Ljava/lang/Throwable;)Z");
}

Master::Context::Context(JNIEnv *env) {
    this->env = env;
}

jthrowable Master::Context::newClashException(std::string const &reason) {
    return reinterpret_cast<jthrowable>(env->NewObject(master->cClashException,
                                                       master->cClashExceptionConstructor,
                                                       env->NewStringUTF(reason.c_str())));
}

void Master::Context::throwThrowable(jthrowable throwable) {
    env->Throw(throwable);
}

jobject Master::Context::newTraffic(long upload, long download) {
    return env->NewObject(master->cTraffic, master->cTrafficConstructor, upload, download);
}

jobject Master::Context::newGeneral(std::string const &mode, int http, int socks, int redirect,
                                    int mixed) {
    return env->NewObject(master->cGeneral, master->cGeneralConstructor,
                          env->NewStringUTF(mode.c_str()), http, socks, redirect, mixed);
}

jobject Master::Context::newCompletableFuture() {
    return env->NewObject(master->cCompletableFuture, master->cCompletableFutureConstructor);
}

jobject Master::Context::newGlobalReference(jobject obj) {
    return env->NewGlobalRef(obj);
}

jobject Master::Context::removeGlobalReference(jobject obj) {
    env->DeleteGlobalRef(obj);

    return obj;
}

bool Master::Context::completeCompletableFuture(jobject completable, jobject object) {
    return env->CallBooleanMethod(completable, master->mCompletableFutureComplete, object);
}

bool
Master::Context::completeExceptionallyCompletableFuture(jobject completable, jthrowable throwable) {
    return env->CallBooleanMethod(completable, master->mCompletableFutureCompleteExceptionally,
                                  throwable);
}

buffer_t Master::Context::createBufferFromByteArray(jbyteArray array) {
    return {
        .buffer = env->GetByteArrayElements(array, nullptr),
        .length = env->GetArrayLength(array)
    };
}

void Master::Context::releaseBufferFromByteArray(jbyteArray array, buffer_t &buffer) {
    env->ReleaseByteArrayElements(array, reinterpret_cast<jbyte*>(buffer.buffer), JNI_COMMIT);
}

const_buffer_t Master::Context::createConstBufferFromByteArray(jbyteArray array) {
    return {
        .buffer = env->GetByteArrayElements(array, nullptr),
        .length = env->GetArrayLength(array)
    };
}

void Master::Context::releaseConstBufferFromByteArray(jbyteArray array, const_buffer_t &buffer) {
    env->ReleaseByteArrayElements(array, const_cast<jbyte*>(reinterpret_cast<const jbyte*>(buffer.buffer)), JNI_ABORT);
}

const char *Master::Context::getString(jstring str) {
    return env->GetStringUTFChars(str, nullptr);
}

void Master::Context::releaseString(jstring str, const char *c) {
    env->ReleaseStringUTFChars(str, c);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *unused) {
    UNUSED(unused);

    JNIEnv *env = nullptr;

    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK)
        return -1;

    new Master(env);

    return JNI_VERSION_1_6;
}