package com.TfPSR.ToTheTop.network;

import java.net.InetAddress;

public class NetworkDir {
    private InetAddress ip;
    private int port;
    private boolean ready;

    public NetworkDir(InetAddress ip, int port) {
        this.ip = ip;
        this.port = port;
        this.ready = false;
    }

    public InetAddress getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }
}
