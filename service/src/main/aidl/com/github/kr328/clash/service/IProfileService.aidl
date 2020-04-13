package com.github.kr328.clash.service;

import com.github.kr328.clash.service.ipc.IStreamCallback;
import com.github.kr328.clash.service.transact.ProfileRequest;
import com.github.kr328.clash.service.model.ProfileMetadata;

interface IProfileService {
    long acquireUnused(String type);
    void updateMetadata(long id, in ProfileMetadata metadata);
    void commit(long id, in IStreamCallback callback);
    void cancel(long id);
    void delete(long id);
    void clear(long id);

    ProfileMetadata queryById(long id);
    ProfileMetadata[] queryAll();
    ProfileMetadata queryActive();

    void setActive(long id);
}
