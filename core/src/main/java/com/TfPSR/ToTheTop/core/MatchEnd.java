package com.TfPSR.ToTheTop.core;

/**
 * End-of-match rule: the match is won as soon as any player's head reaches the
 * summit, the highest climbing surface of the map.
 *
 * <p>Kept apart from the screen so the rule can be exercised without a GL
 * context: the screen only hands it the height of every simulated head.
 */
public final class MatchEnd {

    private MatchEnd() {
    }

    /**
     * @param headHeights height of every simulated head, in world meters
     * @param summitY     height of the highest climbing surface, in world meters
     * @return true when at least one head reached or passed the summit
     */
    public static boolean reachedSummit(float[] headHeights, float summitY) {
        for (float headY : headHeights) {
            if (headY >= summitY)
                return true;
        }
        return false;
    }
}
