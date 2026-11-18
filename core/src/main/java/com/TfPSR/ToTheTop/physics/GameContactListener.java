package com.TfPSR.ToTheTop.physics;

import com.TfPSR.ToTheTop.entity.BodyPart;
import com.TfPSR.ToTheTop.map.Rocks;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

/**
 * Tells hands and legs what they touch. A surface is a rock or any body of
 * another character, so a player can stand on and climb another player exactly
 * like a rock. Each contact is checked in both directions: when two hands touch,
 * each one may grab the other.
 */
public class GameContactListener implements ContactListener {

    private enum Phase { BEGIN, UPDATE, END }

    @Override
    public void beginContact(Contact contact) {
        handle(contact, Phase.BEGIN);
    }

    @Override
    public void endContact(Contact contact) {
        handle(contact, Phase.END);
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        handle(contact, Phase.UPDATE);
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
    }

    private void handle(Contact contact, Phase phase) {
        Body bodyA = contact.getFixtureA().getBody();
        Body bodyB = contact.getFixtureB().getBody();
        handleSide(contact, phase, bodyA, bodyB);
        handleSide(contact, phase, bodyB, bodyA);
    }

    /** Applies the contact to {@code self} if it is a hand or a leg touching a surface. */
    private void handleSide(Contact contact, Phase phase, Body self, Body other) {
        if (!(self.getUserData() instanceof BodyPart part) || !isSurfaceFor(other, part))
            return;

        if (part.leg()) {
            if (phase == Phase.BEGIN)
                part.owner().addSupport();
            else if (phase == Phase.END)
                part.owner().removeSupport();
        }

        if (part.arm() != null) {
            switch (phase) {
                case BEGIN -> part.arm().setSurfaceContact(other, contactPoint(contact));
                case UPDATE -> part.arm().updateSurfaceContact(other, contactPoint(contact));
                case END -> part.arm().clearSurfaceContact(other);
            }
        }
    }

    /** A rock, or a body of a character other than the one touching it. */
    private boolean isSurfaceFor(Body body, BodyPart toucher) {
        Object data = body.getUserData();
        if (data instanceof Rocks)
            return true;
        return data instanceof BodyPart part && part.owner() != toucher.owner();
    }

    private Vector2 contactPoint(Contact contact) {
        return contact.getWorldManifold().getPoints()[0];
    }
}
