package com.github.kr328.clash.service;

import com.github.kr328.clash.service.ipc.IPCParcelables;
import com.github.kr328.clash.service.data.ClashProfileEntity;

interface IClashProfileManager {
    ParcelableCompletedFuture addProfile(String name, int type, String uri);
    ParcelableCompletedFuture updateProfile(int id);
    ClashProfileEntity[] queryAllProfiles();
}
