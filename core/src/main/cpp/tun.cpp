#include "main.h"

struct tunContext {
    jobject callback;
};

static void tunContextOnNewSocket(void *context, void *argument) {
    auto *ctx = reinterpret_cast<tunContext*>(context);
    auto fd = reinterpret_cast<long>(argument);

    Master::runWithAttached<int>([&](JNIEnv *env) -> int {
        Master::runWithContext<void>(env, [&](Master::Context *context) {
            context->tunCallbackNewSocket(ctx->callback, fd);
        });

        return 0;
    });
}

static void tunContextOnStop(void *context, void *argument) {
    UNUSED(argument);

    auto *ctx = reinterpret_cast<tunContext*>(context);

    Master::runWithAttached<int>([&](JNIEnv *env) -> int {
        Master::runWithContext<void>(env, [&](Master::Context *context) {
            context->tunCallbackStop(ctx->callback);
            context->removeGlobalReference(ctx->callback);
        });

        return 0;
    });

    delete ctx;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_startTunDevice(JNIEnv *env, jclass clazz, jint fd,
                                                              jint mtu, jstring gateway,
                                                              jstring mirror, jstring dns,
                                                              jobject callback) {
    UNUSED(clazz);

    Master::runWithContext<void>(env, [&](Master::Context *context) {
        const char *gatewayString = context->getString(gateway);
        const char *mirrorString = context->getString(mirror);
        const char *dnsString = context->getString(dns);

        jobject callbackGlobal = context->newGlobalReference(callback);
        auto *callbackContext = new tunContext();

        callbackContext->callback = callbackGlobal;

        ccall_t onNewSocket = {
                .function = &tunContextOnNewSocket,
                .context = callbackContext
        };
        ccall_t onStop = {
                .function = &tunContextOnStop,
                .context = callbackContext
        };

        char *exception = startTunDevice(fd, mtu, gatewayString, mirrorString, dnsString, onNewSocket, onStop);

        context->releaseString(gateway, gatewayString);
        context->releaseString(mirror, mirrorString);
        context->releaseString(dns, dnsString);

        if ( exception != nullptr ) {
            context->throwThrowable(context->newClashException(exception));
            context->removeGlobalReference(callbackContext->callback);
            free(exception);
            delete callbackContext;
        }
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_stopTunDevice(JNIEnv *env, jclass clazz) {
    stopTunDevice();
}