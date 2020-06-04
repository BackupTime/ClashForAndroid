#include <jni.h>
#include <stddef.h>
#include <string.h>
#include <malloc.h>

#include "libclash.h"

jclass cClashException;
jclass cTraffic;
jclass cGeneral;
jmethodID cTrafficConstructor;
jmethodID cGeneralConstructor;

JNIEXPORT void JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_initialize(JNIEnv *env, jclass clazz,
                                                          jbyteArray database,
                                                          jstring home,
                                                          jstring version) {
    buffer_t databaseBuffer = {
            .buffer = (*env)->GetByteArrayElements(env, database, NULL),
            .length = (*env)->GetArrayLength(env, database)
    };
    const_string_t homeString = (*env)->GetStringUTFChars(env, home, NULL);
    const_string_t versionString = (*env)->GetStringUTFChars(env, version, NULL);

    initialize(&databaseBuffer, homeString, versionString);

    (*env)->ReleaseByteArrayElements(env, database, databaseBuffer.buffer, databaseBuffer.length);
    (*env)->ReleaseStringUTFChars(env, home, homeString);
    (*env)->ReleaseStringUTFChars(env, version, versionString);
}

JNIEXPORT void JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_reset(JNIEnv *env, jclass clazz) {
    reset();
}

JNIEXPORT jobject JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_queryGeneral(JNIEnv *env, jclass clazz) {
    general_t general;

    queryGeneral(&general);

    const char *mode = "";

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
    }

    return (*env)->NewObject(env,
            cGeneral,
            cGeneralConstructor,
            (*env)->NewStringUTF(env, mode),
            general.http_port,
            general.socks_port,
            general.redirect_port,
            general.mixed_port);
}

JNIEXPORT jobject JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_querySpeed(JNIEnv *env, jclass clazz) {
    traffic_t traffic;

    querySpeed(&traffic);

    return (*env)->NewObject(env, cTraffic, cTrafficConstructor, (jlong)traffic.upload, (jlong)traffic.download);
}

JNIEXPORT jobject JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_queryBandwidth(JNIEnv *env, jclass clazz) {
    traffic_t traffic;

    queryBandwidth(&traffic);

    return (*env)->NewObject(env, cTraffic, cTrafficConstructor, (jlong)traffic.upload, (jlong)traffic.download);
}

JNIEXPORT void JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_startTunDevice(JNIEnv *env, jclass clazz,
        jint fd,
        jint mtu,
        jstring gateway,
        jstring mirror,
        jstring dns,
        jint new_socket) {

    const_string_t gatewayString = (*env)->GetStringUTFChars(env, gateway, NULL);
    const_string_t mirrorString = (*env)->GetStringUTFChars(env, mirror, NULL);
    const_string_t dnsString = (*env)->GetStringUTFChars(env, dns, NULL);

    char *exception = startTunDevice(fd, mtu, gatewayString, mirrorString, dnsString, new_socket);
    if ( exception != NULL )
        (*env)->ThrowNew(env, cClashException, exception);
    free(exception);

    (*env)->ReleaseStringUTFChars(env, gateway, gatewayString);
    (*env)->ReleaseStringUTFChars(env, mirror, mirrorString);
    (*env)->ReleaseStringUTFChars(env, dns, dnsString);
}

JNIEXPORT void JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_stopTunDevice(JNIEnv *env, jclass clazz) {
    stopTunDevice();
}

JNIEXPORT void JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_setDnsOverride(JNIEnv *env, jclass clazz,
                                                              jboolean override_dns,
                                                              jstring append_nameservers) {
    dns_override_t dns = {
            .override_dns = override_dns,
            .append_nameservers = (*env)->GetStringUTFChars(env, append_nameservers, NULL)
    };

    setDnsOverride(&dns);

    (*env)->ReleaseStringUTFChars(env, append_nameservers, dns.append_nameservers);
}

JNIEXPORT void JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_setProxyMode(JNIEnv *env, jclass clazz,
                                                            jstring mode) {
    const char *m = (*env)->GetStringUTFChars(env, mode, NULL);

    if (strcmp(m, "Direct") == 0) {
        setProxyMode(MODE_DIRECT);
    } else if (strcmp(m, "Global") == 0) {
        setProxyMode(MODE_GLOBAL);
    } else if (strcmp(m, "Rule") == 0) {
        setProxyMode(MODE_RULE);
    }

    (*env)->ReleaseStringUTFChars(env, mode, m);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *unused) {
    (void) unused;

    JNIEnv *env = NULL;

    if ( (*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_6) != JNI_OK )
        return -1;

    cClashException = (*env)->FindClass(env, "com/github/kr328/clash/core/bridge/ClashException");
    cTraffic = (*env)->FindClass(env, "com/github/kr328/clash/core/model/Traffic");
    cGeneral = (*env)->FindClass(env, "com/github/kr328/clash/core/model/General");
    cTrafficConstructor = (*env)->GetMethodID(env, cTraffic, "<init>", "(JJ)V");
    cGeneralConstructor = (*env)->GetMethodID(env, cGeneral, "<init>", "(Ljava/lang/String;IIII)V");

    return JNI_VERSION_1_6;
}