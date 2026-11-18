package com.TfPSR.ToTheTop.network;

/**
 * Packet headers of the v2 protocol, mirrored by hand in the client repository.
 * Every packet after {@code CONNECT} carries the protocol version and the
 * session byte the server assigned, so a stray datagram is dropped instead of
 * derailing the match.
 */
public final class NetworkHeaders {

    private NetworkHeaders() {
    }

    // Client -> server
    public static final byte CONNECT = 0x01;
    public static final byte READY = 0x02;
    public static final byte UNREADY = 0x03;
    public static final byte DISCONNECT = 0x04;
    public static final byte INPUT = 0x05;
    public static final byte DISCOVER = 0x06;
    public static final byte START_REQUEST = 0x07;
    public static final byte PING = 0x08;
    /** After a match: the client leaves the result screen and waits in the room. */
    public static final byte RETURN_TO_LOBBY = 0x09;

    // Server -> client
    public static final byte ACCEPT = 0x11;
    public static final byte REJECT = 0x12;
    public static final byte ROSTER = 0x13;
    public static final byte START = 0x14;
    public static final byte STATE = 0x15;
    public static final byte ANNOUNCE = 0x16;
    public static final byte PONG = 0x18;

    // REJECT reasons
    public static final byte REJECT_VERSION = 1;
    public static final byte REJECT_ROOM = 2;
    public static final byte REJECT_FULL = 3;
    public static final byte REJECT_STARTED = 4;
}
