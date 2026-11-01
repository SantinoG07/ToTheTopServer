package com.TfPSR.ToTheTop.network;

import java.net.InetAddress;

public class NetworkDir {
    private InetAddress ip;
    private int port;
    private boolean ready;
    private volatile InputState lastInput;

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

    /** Last input received from this client, or {@code null} if it never sent one. */
    public InputState getLastInput() {
        return lastInput;
    }

    public void setLastInput(InputState lastInput) {
        this.lastInput = lastInput;
    }
}
