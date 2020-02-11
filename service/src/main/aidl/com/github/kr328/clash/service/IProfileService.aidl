package com.github.kr328.clash.service;

import com.github.kr328.clash.service.transact.ProfileRequest;
import com.github.kr328.clash.service.data.ClashProfileEntity;

interface IProfileService {
    // process
    void enqueueRequest(in ProfileRequest request);

    // query
    ClashProfileEntity[] queryProfiles();
    ClashProfileEntity queryActiveProfile();
}
