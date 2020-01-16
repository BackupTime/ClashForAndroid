package com.github.kr328.clash.service;

import com.github.kr328.clash.service.IClashEventObserver;
import com.github.kr328.clash.service.IClashEventService;
import com.github.kr328.clash.service.IClashProfileService;
import com.github.kr328.clash.service.IClashSettingService;
import com.github.kr328.clash.service.ipc.IPCParcelables;
import com.github.kr328.clash.callback.IUrlTestCallback;
import com.github.kr328.clash.core.event.Event;
import com.github.kr328.clash.core.model.Packet;

interface IClashManager {
    // Control
    ParcelableCompletedFuture setSelectProxy(String proxy, String selected);

    // Query
    CompressedProxyList queryAllProxies();
    General queryGeneral();
    ParcelableCompletedFuture startUrlTest(String group);
}
