package com.TfPSR.ToTheTop.network;

/**
 * Protocol version shared with the client repository.
 *
 * Both repositories mirror this value by hand: if the client and the server do
 * not agree on it, the handshake still succeeds but the match must not start.
 */
public final class ProtocolVersion {

    public static final byte CURRENT = 1;

    private ProtocolVersion() {
    }
}
