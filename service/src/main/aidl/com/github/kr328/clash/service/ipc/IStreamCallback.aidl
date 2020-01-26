package com.github.kr328.clash.service.ipc;

import com.github.kr328.clash.service.ipc.ParcelableContainer;

interface IStreamCallback {
    void send(in ParcelableContainer data);
    void complete();
    void completeExceptionally(String reason);
}
