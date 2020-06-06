#include <jni.h>
#include <string>
#include <functional>

#include "libclash.h"

#define UNUSED(v) ((void)v)

class Master {
public:
    class Context;

public:
    Master(JNIEnv *env);

public:
    template <class R>
    static R runWithContext(JNIEnv *env, const std::function<R (Context *)>& func);

private:
    jclass cClashException;
    jclass cTraffic;
    jclass cGeneral;
    jclass cCompletableFuture;
    jmethodID cClashExceptionConstructor;
    jmethodID cTrafficConstructor;
    jmethodID cGeneralConstructor;
    jmethodID cCompletableFutureConstructor;
    jmethodID mCompletableFutureComplete;
    jmethodID mCompletableFutureCompleteExceptionally;

private:
    static Master *master;

private:
    friend class Context;
};

class Master::Context {
public:
    Context(JNIEnv *env);

public:
    jthrowable newClashException(std::string const &message);
    jobject newTraffic(long upload, long download);
    jobject newGeneral(std::string const &mode, int http, int socks, int redirect, int mixed);
    jobject newCompletableFuture();

public:
    void throwThrowable(jthrowable throwable);

public:
    jobject newGlobalReference(jobject obj);
    jobject removeGlobalReference(jobject obj);

public:
    bool completeCompletableFuture(jobject completable, jobject object);
    bool completeExceptionallyCompletableFuture(jobject completable, jthrowable throwable);

public:
    buffer_t createBufferFromByteArray(jbyteArray array);
    void releaseBufferFromByteArray(jbyteArray array, buffer_t &buffer);
    const_buffer_t createConstBufferFromByteArray(jbyteArray array);
    void releaseConstBufferFromByteArray(jbyteArray array, const_buffer_t &buffer);
    const char *getString(jstring str);
    void releaseString(jstring str, const char *c);

private:
    JNIEnv *env;
};

template <class R>
R Master::runWithContext(JNIEnv *env, const std::function<R (Master::Context *)>& func) {
    Master::Context context(env);

    return func(&context);
}