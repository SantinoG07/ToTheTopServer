package com.TfPSR.ToTheTop.network;

/**
 * One player inside a {@link StatePacket}: its slot, the state of every body
 * part in wire order and the flags the client needs to render it.
 *
 * @param slot          client slot this player occupies
 * @param parts         body parts, see the order documented in {@link StatePacket}
 * @param leftGripped   left hand is gripped to a surface
 * @param rightGripped  right hand is gripped to a surface
 * @param unconscious   player is knocked out; nothing sets it yet
 */
public record PlayerState(int slot,
                          PartState[] parts,
                          boolean leftGripped,
                          boolean rightGripped,
                          boolean unconscious) {
}
