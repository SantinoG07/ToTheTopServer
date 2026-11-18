package com.TfPSR.ToTheTop.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.physics.box2d.joints.RopeJoint;
import com.badlogic.gdx.physics.box2d.joints.RopeJointDef;

public class JointFactory {
    public JointFactory() {}

    public static RevoluteJoint createRevoluteJoint(Body bodyA, Body bodyB, boolean collideConnected, Vector2 anchorA, Vector2 anchorB, World world) {
        RevoluteJointDef jointDef = new RevoluteJointDef();
        jointDef.bodyA = bodyA;
        jointDef.bodyB = bodyB;
        jointDef.collideConnected = collideConnected;
        jointDef.localAnchorA.set(anchorA);
        jointDef.localAnchorB.set(anchorB);

        return (RevoluteJoint) world.createJoint(jointDef);
    }
    public static RevoluteJoint createRevoluteJoint(Body bodyA, Body bodyB, boolean collideConnected, Vector2 anchorA, Vector2 anchorB, World world, float lowerAngle, float upperAngle) {
        RevoluteJointDef jointDef = new RevoluteJointDef();
        jointDef.bodyA = bodyA;
        jointDef.bodyB = bodyB;
        jointDef.collideConnected = collideConnected;
        jointDef.localAnchorA.set(anchorA);
        jointDef.localAnchorB.set(anchorB);
        jointDef.enableLimit = true;
        jointDef.lowerAngle = (float) Math.toRadians(lowerAngle);
        jointDef.upperAngle = (float) Math.toRadians(upperAngle);

        return (RevoluteJoint) world.createJoint(jointDef);
    }

    /** Keeps two anchors at most {@code maxLength} meters apart, leaving them free inside. */
    public static RopeJoint createRopeJoint(Body bodyA, Body bodyB, Vector2 anchorA, Vector2 anchorB, float maxLength, World world) {
        RopeJointDef jointDef = new RopeJointDef();
        jointDef.bodyA = bodyA;
        jointDef.bodyB = bodyB;
        jointDef.collideConnected = true;
        jointDef.localAnchorA.set(anchorA);
        jointDef.localAnchorB.set(anchorB);
        jointDef.maxLength = maxLength;

        return (RopeJoint) world.createJoint(jointDef);
    }

}
