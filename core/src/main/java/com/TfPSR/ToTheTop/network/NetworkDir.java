package com.TfPSR.ToTheTop.network;

import java.net.InetAddress;

public class NetworkDir {
    private InetAddress ip;
    private int port;
    private final int session;
    private boolean ready;
    private String name;
    private volatile InputState lastInput;

    public NetworkDir(InetAddress ip, int port, int session) {
        this.ip = ip;
        this.port = port;
        this.session = session;
        this.ready = false;
        this.name = "Player";
    }

    /** Name the client sent on CONNECT, or the server fallback. */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** Id the server assigned to this client; every message after CONNECT echoes it. */
    public int getSession() {
        return session;
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
