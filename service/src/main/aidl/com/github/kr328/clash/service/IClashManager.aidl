package com.github.kr328.clash.service;

import com.github.kr328.clash.service.ipc.IPCParcelables;
import com.github.kr328.clash.core.model.Packet;

interface IClashManager {
    // Control
    boolean setSelectProxy(String proxy, String selected);
    ParcelableCompletedFuture startHealthCheck(String group);

    // Query
    ProxyGroup[] queryAllProxies();
    General queryGeneral();

    // Events
    ParcelablePipe openBandwidthEvent();
    ParcelablePipe openLogEvent();

    // Settings
    boolean putSetting(String key, String value);
    String getSetting(String key);
}
