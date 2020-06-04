package com.github.kr328.clash.core.bridge;

import androidx.annotation.Keep;

import com.github.kr328.clash.core.model.General;
import com.github.kr328.clash.core.model.Traffic;

@Keep
public final class Bridge {
    static {
        System.loadLibrary("clash");
        System.loadLibrary("bridge");
    }

    private Bridge() {
    }

    public static native void initialize(byte[] database, String home, String version);
    public static native void reset();

    public static native General queryGeneral();
    public static native Traffic querySpeed();
    public static native Traffic queryBandwidth();

    public static native void startTunDevice(int fd, int mtu, String gateway, String mirror, String dns, int newSocket) throws ClashException;
    public static native void stopTunDevice();

    public static native void setDnsOverride(boolean overrideDns, String appendNameservers);
    public static native void setProxyMode(String mode);
}
