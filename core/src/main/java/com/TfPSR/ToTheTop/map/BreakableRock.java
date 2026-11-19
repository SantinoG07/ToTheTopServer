package com.TfPSR.ToTheTop.map;

import com.TfPSR.ToTheTop.entity.BodyPart;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;

/**
 * A small rock that crumbles after being held or touched for too long and
 * comes back a while later. The server owns its wear, its breaking and its
 * respawn; clients only receive its state to draw it.
 *
 * <p>Wear grows while a character touches or grips the rock and recovers at a
 * slower rate when it is left alone, so a quick touch never breaks it but a
 * long hold does.
 */
public class BreakableRock {

    /** Seconds of contact that break the rock. */
    public static final float BREAK_TIME = 5f;
    /** Seconds a broken rock stays away before it may come back. */
    public static final float RESPAWN_TIME = 5f;
    /** Wear recovered per second without contact, relative to the wear rate. */
    private static final float RECOVERY_RATE = 0.5f;

    /** Wire value of a broken rock; intact rocks send their wear as 0..{@link #WIRE_MAX_WEAR}. */
    public static final int WIRE_BROKEN = 255;
    public static final int WIRE_MAX_WEAR = 200;

    private final Body body;
    /** World-space bounding box, used to check that nobody stands where it respawns. */
    private final Rectangle bounds;

    private int touches;
    private float wear;
    private boolean broken;
    private float brokenTime;

    BreakableRock(Body body, Rectangle bounds) {
        this.body = body;
        this.bounds = bounds;
    }

    public Body getBody() {
        return body;
    }

    public boolean isBroken() {
        return broken;
    }

    /** Called by the contact listener when a character body starts touching it. */
    public void addTouch() {
        touches++;
    }

    /** Called by the contact listener when a character body stops touching it. */
    public void removeTouch() {
        if (touches > 0)
            touches--;
    }

    /**
     * Advances the wear, or the respawn countdown once broken.
     *
     * @param gripped true when some hand is holding this rock
     * @return true on the step the rock breaks; the caller must release every
     *         grip on it and then call {@link #shatter()}
     */
    public boolean update(float step, boolean gripped) {
        if (broken) {
            brokenTime += step;
            return false;
        }

        if (touches > 0 || gripped)
            wear += step;
        else
            wear = Math.max(0f, wear - step * RECOVERY_RATE);

        return wear >= BREAK_TIME;
    }

    /** Removes the rock from the simulation. Grips on it must be released first. */
    public void shatter() {
        broken = true;
        brokenTime = 0f;
        wear = 0f;
        body.setActive(false);
        touches = 0;   // deactivating ends its contacts
    }

    /**
     * Brings the rock back once its time is up and its place is free: a body
     * appearing inside a character would launch it.
     */
    public void respawnIfReady(World world) {
        if (!broken || brokenTime < RESPAWN_TIME || isOccupied(world))
            return;
        broken = false;
        touches = 0;
        body.setActive(true);
    }

    private boolean isOccupied(World world) {
        boolean[] occupied = {false};
        world.QueryAABB(fixture -> {
            if (fixture.getBody().getUserData() instanceof BodyPart) {
                occupied[0] = true;
                return false;
            }
            return true;
        }, bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height);
        return occupied[0];
    }

    /** State byte for the STATE packet: 255 when broken, else the wear scaled to 0..200. */
    public int wireState() {
        if (broken)
            return WIRE_BROKEN;
        return Math.round(Math.min(1f, wear / BREAK_TIME) * WIRE_MAX_WEAR);
    }
}
