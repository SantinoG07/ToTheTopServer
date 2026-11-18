package com.TfPSR.ToTheTop.entity;

/**
 * User data of every body of a character, so the contact listener knows whose
 * body was touched and what it is.
 *
 * @param owner character the body belongs to
 * @param arm   arm whose hand this body is, or {@code null} if it is not a hand
 * @param leg   true for a leg, used to know when the character stands on something
 */
public record BodyPart(Character owner, Arm arm, boolean leg) {
}
