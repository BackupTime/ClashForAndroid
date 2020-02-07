package com.github.kr328.clash.service;

import com.github.kr328.clash.service.transact.ProfileRequest;

interface IProfileService {
    void enqueueRequest(in ProfileRequest request);
}
