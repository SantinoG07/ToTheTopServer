package com.TfPSR.ToTheTop.network;

/**
 * One decoded player input, exactly as the client sends it: the cursor in world
 * meters and the buttons/keys held during that tick.
 */
public record InputState(float mouseX, float mouseY,
                         boolean left, boolean right,
                         boolean a, boolean d) {
}
