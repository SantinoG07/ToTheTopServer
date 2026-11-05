package com.TfPSR.ToTheTop.network;

/**
 * State of one simulated body part, in the units the client renders it:
 * world meters and degrees.
 */
public record PartState(float x, float y, float angleDegrees) {
}
