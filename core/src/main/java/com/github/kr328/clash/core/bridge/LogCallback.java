package com.github.kr328.clash.core.bridge;

import com.github.kr328.clash.core.event.LogEvent;

interface LogCallback {
    void onMessage(LogEvent event);
}
