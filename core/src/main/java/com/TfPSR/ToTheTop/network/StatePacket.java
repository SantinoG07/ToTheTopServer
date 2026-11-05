package com.TfPSR.ToTheTop.network;

import java.nio.ByteBuffer;

/**
 * Wire format of the match state the server broadcasts at a fixed tick once the
 * match started. Mirrored by hand in the client repository: see
 * {@code odd/tasks/server-state-sync.md} for the full contract table.
 *
 * <p>Coordinates are world meters and angles are degrees, because that is what
 * the client renders. Body parts are always sent in this order:
 * head, torso, left upper arm, left forearm, left hand, right upper arm,
 * right forearm, right hand, left leg, right leg.
 *
 * <p>Layout: header, protocol version, match state, player count, then one block
 * per player with its slot, its part count, three floats per part, the grip mask
 * and the flags byte.
 */
public final class StatePacket {

    /** The match is still running; no other value is produced yet. */
    public static final byte MATCH_RUNNING = 0;

    public static final int HEADER_SIZE = 4;
    public static final int PLAYER_HEADER_SIZE = 2;
    public static final int PART_SIZE = 12;             // float x, float y, float angleDegrees
    public static final int PLAYER_TRAILER_SIZE = 2;    // grip mask, flags

    public static final int GRIP_LEFT = 1;
    public static final int GRIP_RIGHT = 1 << 1;
    public static final int FLAG_UNCONSCIOUS = 1;

    private StatePacket() {
    }

    /** Exact size in bytes of the packet {@link #encode} would produce. */
    public static int size(PlayerState[] players) {
        int total = HEADER_SIZE;
        if (players == null)
            return total;

        for (PlayerState player : players) {
            if (player == null)
                continue;
            int partCount = player.parts() == null ? 0 : player.parts().length;
            total += PLAYER_HEADER_SIZE + partCount * PART_SIZE + PLAYER_TRAILER_SIZE;
        }
        return total;
    }

    /**
     * Encodes the snapshot, or returns {@code null} when there is no player to
     * report: sending an empty frame would only wake the client up to draw
     * nothing.
     */
    public static byte[] encode(PlayerState[] players, byte matchState) {
        if (players == null || players.length == 0)
            return null;

        ByteBuffer buffer = ByteBuffer.allocate(size(players));
        buffer.put(NetworkHeaders.STATE);
        buffer.put(ProtocolVersion.CURRENT);
        buffer.put(matchState);
        buffer.put((byte) players.length);

        for (PlayerState player : players) {
            PartState[] parts = player.parts();
            buffer.put((byte) player.slot());
            buffer.put((byte) parts.length);

            for (PartState part : parts) {
                buffer.putFloat(part.x());
                buffer.putFloat(part.y());
                buffer.putFloat(part.angleDegrees());
            }

            buffer.put((byte) gripMask(player));
            buffer.put((byte) flags(player));
        }

        return buffer.array();
    }

    private static int gripMask(PlayerState player) {
        int mask = 0;
        if (player.leftGripped())
            mask |= GRIP_LEFT;
        if (player.rightGripped())
            mask |= GRIP_RIGHT;
        return mask;
    }

    private static int flags(PlayerState player) {
        return player.unconscious() ? FLAG_UNCONSCIOUS : 0;
    }
}
