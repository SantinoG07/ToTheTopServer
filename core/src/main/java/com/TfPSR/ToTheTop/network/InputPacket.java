package com.TfPSR.ToTheTop.network;

import java.nio.ByteBuffer;

/**
 * Wire format of a player input packet, mirroring the client encoder.
 *
 * Layout (13 bytes): header, protocol version, float mouseX, float mouseY,
 * button mask, key mask, event mask. Coordinates are world meters, not screen
 * pixels. Event bits are transient: the client repeats them for a few ticks.
 */
public final class InputPacket {

    public static final int SIZE = 13;

    public static final int BUTTON_LEFT = 1;
    public static final int BUTTON_RIGHT = 1 << 1;

    public static final int KEY_A = 1;
    public static final int KEY_D = 1 << 1;

    /** One-off events: the client repeats each bit for a few ticks. */
    public static final int EVENT_IMPULSE = 1;
    public static final int EVENT_PAUSE = 1 << 1;
    public static final int EVENT_RESUME = 1 << 2;
    public static final int EVENT_LEAVE = 1 << 3;

    private InputPacket() {
    }

    /**
     * Decodes a received datagram payload.
     *
     * @return the decoded input, or {@code null} when the packet is too short,
     *         has another header or was written with a protocol version we do
     *         not speak. Callers drop the packet; a client must never be able
     *         to derail the simulation with a malformed datagram.
     */
    public static InputState decode(byte[] data, int offset, int length) {
        if (data == null || length < SIZE)
            return null;

        ByteBuffer buffer = ByteBuffer.wrap(data, offset, SIZE);
        if (buffer.get() != NetworkHeaders.INPUT)
            return null;
        if (buffer.get() != ProtocolVersion.CURRENT)
            return null;

        float mouseX = buffer.getFloat();
        float mouseY = buffer.getFloat();
        byte buttons = buffer.get();
        byte keys = buffer.get();
        byte events = buffer.get();

        return new InputState(
                mouseX,
                mouseY,
                (buttons & BUTTON_LEFT) != 0,
                (buttons & BUTTON_RIGHT) != 0,
                (keys & KEY_A) != 0,
                (keys & KEY_D) != 0,
                events & 0xFF);
    }
}
