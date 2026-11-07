package com.TfPSR.ToTheTop.network;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Wire format of the join request, mirroring the client encoder.
 *
 * Layout: header, protocol version, four ASCII room-code bytes, name length,
 * name in UTF-8. The name is clamped to {@link #NAME_MAX_BYTES} bytes without
 * splitting a UTF-8 character.
 */
public final class ConnectPacket {

    /** Bytes before the name: header, version, code and name length. */
    public static final int BASE_SIZE = 7;

    public static final int NAME_MAX_BYTES = 16;

    private final String roomCode;
    private final String name;

    private ConnectPacket(String roomCode, String name) {
        this.roomCode = roomCode;
        this.name = name;
    }

    /** Four-digit code the client read from the announcement. */
    public String roomCode() {
        return roomCode;
    }

    /** Trimmed player name, empty when the client did not send one. */
    public String name() {
        return name;
    }

    /**
     * @return the decoded request, or {@code null} when the datagram is too
     * short, carries another header or was written by another protocol version
     */
    public static ConnectPacket decode(byte[] data, int offset, int length) {
        if (length < BASE_SIZE)
            return null;

        ByteBuffer buffer = ByteBuffer.wrap(data, offset, length);
        if (buffer.get() != NetworkHeaders.CONNECT)
            return null;
        if (buffer.get() != ProtocolVersion.CURRENT)
            return null;

        byte[] code = new byte[4];
        buffer.get(code);

        int declared = buffer.get() & 0xFF;
        int available = length - BASE_SIZE;
        int nameBytes = Math.min(declared, available);
        int end = Math.min(nameBytes, NAME_MAX_BYTES);

        // Never cut a UTF-8 character in half when the name is clamped.
        while (end > 0 && end < nameBytes && (buffer.get(offset + BASE_SIZE + end) & 0xC0) == 0x80)
            end--;

        byte[] name = new byte[end];
        buffer.get(name);

        return new ConnectPacket(new String(code, StandardCharsets.US_ASCII),
                new String(name, StandardCharsets.UTF_8).trim());
    }
}
