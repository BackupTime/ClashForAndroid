package com.github.kr328.clash.service;

import com.github.kr328.clash.service.ipc.IPCParcelables;

interface IClashProfileManager {
    ParcelableCompletedFuture addProfile(String url);
    ParcelableCompletedFuture updateProfile(int id);
    ParcelableCompletedFuture queryAllProfiles();
}
