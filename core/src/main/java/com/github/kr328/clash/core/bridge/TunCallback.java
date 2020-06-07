package com.github.kr328.clash.core.bridge;

import androidx.annotation.Keep;

@Keep
public interface TunCallback {
    void onNewSocket(int socket);
    void onStop();
}
