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

    // Profiles
    ParcelableCompletedFuture addProfile(String url);
    ParcelableCompletedFuture updateProfile(int id);
    ParcelableCompletedFuture queryAllProfiles();

    // Events
    ParcelablePipe openBandwidthEvent();
    ParcelablePipe openLogEvent();

    // Settings

}
