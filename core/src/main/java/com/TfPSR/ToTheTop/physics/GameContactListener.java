package com.TfPSR.ToTheTop.physics;

import com.TfPSR.ToTheTop.entity.Arm;
import com.TfPSR.ToTheTop.map.Rocks;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class GameContactListener implements ContactListener {


    @Override
    public void beginContact(Contact contact) {
        Arm arm = getArm(contact);
        Body surface = getSurface(contact);

        if (arm == null || surface == null) {
            return;
        }

        WorldManifold manifold = contact.getWorldManifold();
        Vector2 point = manifold.getPoints()[0];

        arm.setSurfaceContact(surface, point);
    }

    @Override
    public void endContact(Contact contact) {
        Arm arm = getArm(contact);
        Body surface = getSurface(contact);

        if (arm == null || surface == null) {
            return;
        }

        arm.clearSurfaceContact(surface);
    }

    private Arm getArm(Contact contact) {
        Object dataA = contact.getFixtureA().getBody().getUserData();
        Object dataB = contact.getFixtureB().getBody().getUserData();

        if(dataA instanceof Arm) {
            return (Arm) dataA;
        }

        if(dataB instanceof Arm) {
            return (Arm) dataB;
        }

        return null;
    }

    private Body getSurface(Contact contact) {
        Body bodyA = contact.getFixtureA().getBody();
        Body bodyB = contact.getFixtureB().getBody();

        if (bodyA.getUserData() instanceof Rocks) {
            return bodyA;
        }

        if (bodyB.getUserData() instanceof Rocks) {
            return bodyB;
        }

        return null;
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        Arm arm = getArm(contact);
        Body surface = getSurface(contact);

        if (arm == null || surface == null) {
            return;
        }

        WorldManifold manifold = contact.getWorldManifold();
        Vector2 point = manifold.getPoints()[0];

        arm.updateSurfaceContact(surface, point);
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {

    }
}
