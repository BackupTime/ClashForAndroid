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

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_queryProxyGroups(JNIEnv *env, jclass clazz) {
    UNUSED(clazz);

    return Master::runWithContext<jobjectArray>(env, [&](Master::Context *context) -> jobjectArray {
        proxy_group_list_t *list = queryProxyGroups();
        auto *jgroups = new jobject[list->size];

        for (int group_index = 0 ; group_index < list->size ; group_index++ ) {
            std::string now;
            proxy_group_t *group = list->groups[group_index];
            auto *jproxies = new jobject[group->proxies_size];

            for ( int proxy_index = 0 ; proxy_index < group->proxies_size ; proxy_index++ ) {
                proxy_t *proxy = &group->proxies[proxy_index];

                jproxies[proxy_index] = context->createProxy(proxy->name, proxy->proxy_type, proxy->delay);

                if ( proxy_index == group->now )
                    now = proxy->name;

                free(proxy->name);
            }

            jgroups[group_index] = context->createProxyGroup(
                    group->base.name,
                    group->base.proxy_type,
                    now,
                    context->createProxyArray(group->proxies_size, jproxies));

            delete[] jproxies;

            free(group->base.name);
            free(group);
        }

        jobjectArray result = context->createProxyGroupArray(list->size, jgroups);

        delete[] jgroups;

        free(list);

        return result;
    });
}