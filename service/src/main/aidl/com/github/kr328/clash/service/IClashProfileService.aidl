package com.github.kr328.clash.service;

import com.github.kr328.clash.service.data.ClashProfileEntity;

interface IClashProfileService {
    ClashProfileEntity[] queryProfiles();
    void setActiveProfile(int id);
    void addProfile(in ClashProfileEntity profile);
    void removeProfile(int id);
    void touchProfile(int id);
    ClashProfileEntity queryActiveProfile();
}
