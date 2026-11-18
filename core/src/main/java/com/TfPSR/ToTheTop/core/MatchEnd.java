package com.TfPSR.ToTheTop.core;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * End-of-match rule: the match is won as soon as any player's head enters the
 * summit area of the map.
 *
 * <p>Kept apart from the screen so the rule can be exercised without a GL
 * context: the screen only hands it the height of every simulated head.
 */
public final class MatchEnd {

    private MatchEnd() {
    }

    /**
     * @param heads  position of every simulated head, in world meters
     * @param summit summit area, in world meters
     * @return true when at least one head is inside the summit area
     */
    public static boolean reachedSummit(Vector2[] heads, Rectangle summit) {
        for (Vector2 head : heads) {
            if (summit.contains(head))
                return true;
        }
        return false;
    }
}
