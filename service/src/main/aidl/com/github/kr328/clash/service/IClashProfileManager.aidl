package com.github.kr328.clash.service;

import com.github.kr328.clash.service.data.ClashProfileEntity;
import com.github.kr328.clash.service.ipc.IStreamCallback;

interface IClashProfileManager {
    void addProfile(String name, int type, String uri, IStreamCallback callback);
    void updateProfile(int id, IStreamCallback callback);
    ClashProfileEntity[] queryAllProfiles();
}
