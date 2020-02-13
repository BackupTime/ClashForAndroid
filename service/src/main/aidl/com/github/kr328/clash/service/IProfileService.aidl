package com.github.kr328.clash.service;

import com.github.kr328.clash.service.transact.ProfileRequest;
import com.github.kr328.clash.service.data.ClashProfileEntity;

interface IProfileService {
    void enqueueRequest(in ProfileRequest request);
    String requestProfileEditUri(long id);
    void commitProfileEditUri(String uri);

    ClashProfileEntity[] queryProfiles();
    ClashProfileEntity queryActiveProfile();

    void setActiveProfile(long id);
}
