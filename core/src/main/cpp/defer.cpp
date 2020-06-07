#include "main.h"

#include <android/log.h>

struct deferContext {
    jobject completableFuture;
};

static void downloadContextSuccess(void *context, void *argument) {
    UNUSED(argument);

    auto *ctx = reinterpret_cast<deferContext*>(context);

    Master::runWithAttached<int>([&](JNIEnv *env) -> int {
        Master::runWithContext<void>(env, [&](Master::Context *context) {
            context->completeCompletableFuture(ctx->completableFuture, nullptr);

            context->removeGlobalReference(ctx->completableFuture);
        });

        return 0;
    });

    delete ctx;
}

static void downloadContextFailure(void *context, void *argument) {
    auto *ctx = reinterpret_cast<deferContext*>(context);
    auto *err = reinterpret_cast<char *>(argument);

    Master::runWithAttached<int>([&](JNIEnv *env) -> int {
        Master::runWithContext<void>(env, [&](Master::Context *context) {
            if (err != nullptr) {
                context->completeExceptionallyCompletableFuture(ctx->completableFuture, context->newClashException(err));
            } else {
                context->completeExceptionallyCompletableFuture(ctx->completableFuture, context->newClashException("Unknown"));
            }

            context->removeGlobalReference(ctx->completableFuture);
        });

        return 0;
    });

    free(err);
    delete ctx;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_downloadProfile__ILjava_lang_String_2Ljava_lang_String_2(
        JNIEnv *env, jclass clazz, jint fd, jstring base, jstring output) {
    UNUSED(clazz);

    return Master::runWithContext<jobject>(env, [&](Master::Context *context) -> jobject {
        const char *b = context->getString(base);
        const char *o = context->getString(output);

        auto *ctx = new deferContext();

        ctx->completableFuture = context->newGlobalReference(context->newCompletableFuture());

        ccall_t onSuccess = {
                .context = ctx,
                .function = &downloadContextSuccess
        };
        ccall_t onFailure = {
                .context = ctx,
                .function = &downloadContextFailure
        };

        downloadProfileFromFd(fd, b, o, onSuccess, onFailure);

        context->releaseString(base, b);
        context->releaseString(output, o);

        return ctx->completableFuture;
    });
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_downloadProfile__Ljava_lang_String_2Ljava_lang_String_2Ljava_lang_String_2(
        JNIEnv *env, jclass clazz, jstring url, jstring base, jstring output) {
    UNUSED(clazz);

    return Master::runWithContext<jobject>(env, [&](Master::Context *context) -> jobject {
        const char *u = context->getString(url);
        const char *b = context->getString(base);
        const char *o = context->getString(output);

        auto *ctx = new deferContext();

        ctx->completableFuture = context->newGlobalReference(context->newCompletableFuture());

        ccall_t onSuccess = {
                .context = ctx,
                .function = &downloadContextSuccess
        };
        ccall_t onFailure = {
                .context = ctx,
                .function = &downloadContextFailure
        };

        downloadProfileFromUrl(u, b, o, onSuccess, onFailure);

        context->releaseString(url, u);
        context->releaseString(base, b);
        context->releaseString(output, o);

        return ctx->completableFuture;
    });
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_loadProfile(JNIEnv *env, jclass clazz, jstring path,
                                                           jstring base) {
    UNUSED(clazz);

    return Master::runWithContext<jobject>(env, [&](Master::Context *context) -> jobject {
        const char *p = context->getString(path);
        const char *b = context->getString(base);

        auto *ctx = new deferContext();

        ctx->completableFuture = context->newGlobalReference(context->newCompletableFuture());

        ccall_t onSuccess = {
                .context = ctx,
                .function = &downloadContextSuccess
        };
        ccall_t onFailure = {
                .context = ctx,
                .function = &downloadContextFailure
        };

        loadProfile(p, b, onSuccess, onFailure);

        context->releaseString(path, p);
        context->releaseString(base, b);

        return ctx->completableFuture;
    });
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_performHealthCheck(JNIEnv *env, jclass clazz,
                                                                  jstring group) {
    UNUSED(clazz);

    return Master::runWithContext<jobject>(env, [&](Master::Context *context) -> jobject {
        const char *g = context->getString(group);

        auto *ctx = new deferContext();

        ctx->completableFuture = context->newGlobalReference(context->newCompletableFuture());

        ccall_t onSuccess = {
                .context = ctx,
                .function = &downloadContextSuccess
        };
        ccall_t onFailure = {
                .context = ctx,
                .function = &downloadContextFailure
        };

        performHealthCheck(g, onSuccess, onFailure);

        context->releaseString(group, g);

        return ctx->completableFuture;
    });
}