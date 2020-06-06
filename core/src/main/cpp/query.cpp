#include "main.h"

extern "C"
JNIEXPORT jobject JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_queryGeneral(JNIEnv *env, jclass clazz) {
    UNUSED(clazz);

    return Master::runWithContext<jobject>(env, [&](Master::Context *context) -> jobject {
        general_t general;

        queryGeneral(&general);

        const char *mode = nullptr;

        switch (general.mode) {
            case MODE_DIRECT:
                mode = "Direct";
                break;
            case MODE_GLOBAL:
                mode = "Global";
                break;
            case MODE_RULE:
                mode = "Rule";
                break;
            case MODE_SCRIPT:
                mode = "Script";
                break;
        }

        return context->newGeneral(mode,
                general.http_port, general.socks_port,
                general.redirect_port, general.mixed_port);
    });
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_queryBandwidth(JNIEnv *env, jclass clazz) {
    UNUSED(clazz);

    return Master::runWithContext<jobject>(env, [&](Master::Context *context) -> jobject {
        traffic_t traffic;

        queryBandwidth(&traffic);

        return context->newTraffic(traffic.upload, traffic.download);
    });
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_querySpeed(JNIEnv *env, jclass clazz) {
    UNUSED(clazz);

    return Master::runWithContext<jobject>(env, [&](Master::Context *context) -> jobject {
        traffic_t traffic;

        querySpeed(&traffic);

        return context->newTraffic(traffic.upload, traffic.download);
    });
}