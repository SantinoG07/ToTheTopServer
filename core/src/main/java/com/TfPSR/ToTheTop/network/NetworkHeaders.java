package com.TfPSR.ToTheTop.network;

public final class NetworkHeaders {

    private NetworkHeaders() {
    }
    public static final byte CONNECT = 0x01;
    public static final byte READY = 0x02;
    public static final byte UNREADY = 0x03;
    public static final byte DISCONNECT = 0x04;
    public static final byte ACCEPT = 0x11;
    public static final byte REJECT = 0x12;
    public static final byte ROSTER = 0x13;
    public static final byte START = 0x14;
}
