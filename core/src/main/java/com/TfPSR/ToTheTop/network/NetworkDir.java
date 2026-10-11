package com.TfPSR.ToTheTop.network;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkDir {
    private InetAddress ip;
    private int port;

    public NetworkDir(InetAddress ip, int port) {
        this.ip = ip;
        this.port= port;
    }

    public InetAddress getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
