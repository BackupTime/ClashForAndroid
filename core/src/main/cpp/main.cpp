#include "main.h"

Master *Master::master = nullptr;

Master::Master(JavaVM *vm, JNIEnv *env): vm(vm) {
    master = this;

    cClashException = env->FindClass("com/github/kr328/clash/core/bridge/ClashException");
    cTraffic = env->FindClass("com/github/kr328/clash/core/model/Traffic");
    cGeneral = env->FindClass("com/github/kr328/clash/core/model/General");
    cCompletableFuture = env->FindClass("java/util/concurrent/CompletableFuture");
    cProxyGroup = env->FindClass("com/github/kr328/clash/core/model/ProxyGroup");
    cProxy = env->FindClass("com/github/kr328/clash/core/model/Proxy");
    iTunCallback = env->FindClass("com/github/kr328/clash/core/bridge/TunCallback");
    cClashExceptionConstructor = env->GetMethodID(cClashException, "<init>",
                                                  "(Ljava/lang/String;)V");
    cTrafficConstructor = env->GetMethodID(cTraffic, "<init>", "(JJ)V");
    cGeneralConstructor = env->GetMethodID(cGeneral, "<init>", "(Ljava/lang/String;IIII)V");
    cCompletableFutureConstructor = env->GetMethodID(cCompletableFuture, "<init>", "()V");
    cProxyGroupConstructor = env->GetMethodID(cProxyGroup, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Lcom/github/kr328/clash/core/model/Proxy;)V");
    cProxyConstructor = env->GetMethodID(cProxy, "<init>", "(Ljava/lang/String;Ljava/lang/String;J)V");
    mCompletableFutureComplete = env->GetMethodID(cCompletableFuture, "complete",
                                                  "(Ljava/lang/Object;)Z");
    mCompletableFutureCompleteExceptionally = env->GetMethodID(cCompletableFuture,
                                                               "completeExceptionally",
                                                               "(Ljava/lang/Throwable;)Z");
    mTunCallbackOnNewSocket = env->GetMethodID(iTunCallback, "onNewSocket", "(I)V");
    mTunCallbackOnStop = env->GetMethodID(iTunCallback, "onStop", "()V");

    sDirect = reinterpret_cast<jstring>(env->NewGlobalRef(env->NewStringUTF("Direct")));
    sReject = reinterpret_cast<jstring>(env->NewGlobalRef(env->NewStringUTF("Reject")));
    sShadowsocks = reinterpret_cast<jstring>(env->NewGlobalRef(env->NewStringUTF("Shadowsocks")));
    sSnell = reinterpret_cast<jstring>(env->NewGlobalRef(env->NewStringUTF("Snell")));
    sSocks5 = reinterpret_cast<jstring>(env->NewGlobalRef(env->NewStringUTF("Socks5")));
    sHttp = reinterpret_cast<jstring>(env->NewGlobalRef(env->NewStringUTF("Http")));
    sVmess = reinterpret_cast<jstring>(env->NewGlobalRef(env->NewStringUTF("Vmess")));
    sTrojan = reinterpret_cast<jstring>(env->NewGlobalRef(env->NewStringUTF("Trojan")));
    sRelay = reinterpret_cast<jstring>(env->NewGlobalRef(env->NewStringUTF("Relay")));
    sSelector = reinterpret_cast<jstring>(env->NewGlobalRef(env->NewStringUTF("Selector")));
    sFallback = reinterpret_cast<jstring>(env->NewGlobalRef(env->NewStringUTF("Fallback")));
    sURLTest = reinterpret_cast<jstring>(env->NewGlobalRef(env->NewStringUTF("URLTest")));
    sLoadBalance = reinterpret_cast<jstring>(env->NewGlobalRef(env->NewStringUTF("LoadBalance")));
    sUnknown = reinterpret_cast<jstring>(env->NewGlobalRef(env->NewStringUTF("Unknown")));
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

void Master::Context::tunCallbackNewSocket(jobject callback, int fd) {
    env->CallVoidMethod(callback, master->mTunCallbackOnNewSocket, fd);
}

void Master::Context::tunCallbackStop(jobject callback) {
    env->CallVoidMethod(callback, master->mTunCallbackOnStop);
}

jobjectArray Master::Context::createProxyGroupArray(int size, jobject elements[]) {
    jobjectArray result = env->NewObjectArray(size, master->cProxyGroup, nullptr);

    for ( int i = 0 ; i < size ; i++ )
        env->SetObjectArrayElement(result, i, elements[i]);

    return result;
}

jobjectArray Master::Context::createProxyArray(int size, jobject elements[]) {
    jobjectArray result = env->NewObjectArray(size, master->cProxy, nullptr);

    for ( int i = 0 ; i < size ; i++ )
        env->SetObjectArrayElement(result, i, elements[i]);

    return result;
}

jobject Master::Context::createProxy(std::string const &name, proxy_type_t type, long delay) {
    jstring ts = nullptr;

    switch (type) {
        case Direct:
            ts = master->sDirect;
            break;
        case Reject:
            ts = master->sReject;
            break;
        case Socks5:
            ts = master->sSocks5;
            break;
        case Http:
            ts = master->sHttp;
            break;
        case Shadowsocks:
            ts = master->sShadowsocks;
            break;
        case Vmess:
            ts = master->sVmess;
            break;
        case Snell:
            ts = master->sSnell;
            break;
        case Trojan:
            ts = master->sTrojan;
            break;
        case Selector:
            ts = master->sSelector;
            break;
        case Fallback:
            ts = master->sFallback;
            break;
        case LoadBalance:
            ts = master->sLoadBalance;
            break;
        case URLTest:
            ts = master->sURLTest;
            break;
        case Relay:
            ts = master->sRelay;
            break;
        case Unknown:
            ts = master->sUnknown;
            break;
        default:
            ts = master->sUnknown;
    }

    return env->NewObject(master->cProxy, master->cProxyConstructor, env->NewStringUTF(name.c_str()), ts, delay);
}

jobject Master::Context::createProxyGroup(std::string const &name, proxy_type_t type,
                                          std::string const &current, jobjectArray proxies) {
    jstring ts = nullptr;

    switch (type) {
        case Selector:
            ts = master->sSelector;
            break;
        case Fallback:
            ts = master->sFallback;
            break;
        case LoadBalance:
            ts = master->sLoadBalance;
            break;
        case URLTest:
            ts = master->sURLTest;
            break;
        case Relay:
            ts = master->sRelay;
            break;
        case Unknown:
            ts = master->sUnknown;
            break;
        default:
            ts = master->sUnknown;
    }

    return env->NewObject(master->cProxyGroup, master->cProxyGroupConstructor, env->NewStringUTF(name.c_str()), ts, env->NewStringUTF(current.c_str()), proxies);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *unused) {
    UNUSED(unused);

    JNIEnv *env = nullptr;

    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK)
        return -1;

    new Master(vm, env);

    return JNI_VERSION_1_6;
}