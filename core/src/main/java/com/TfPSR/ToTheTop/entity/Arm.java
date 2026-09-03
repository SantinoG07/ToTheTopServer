package com.TfPSR.ToTheTop.entity;

import com.TfPSR.ToTheTop.physics.JointFactory;
import com.TfPSR.ToTheTop.physics.ShapeFactory;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;

public class Arm {
    private Sprite spriteUpperArm;
    private Sprite spriteForearm;
    private Sprite spriteHand;

    private static final float ARM_HEIGHT_RATIO = 0.47f;
    private static final float FOREARM_HEIGHT_RATIO = 0.4f;
    private static final float HAND_HEIGHT_RATIO = 0.13f;

    private static final float ARM_MASS_RATIO = 0.03f;
    private static final float FOREARM_MASS_RATIO = 0.02f;
    private static final float HAND_MASS_RATIO = 0.01f;

    private static final float MOUSE_SENSITIVITY = 0.1f;
    private static final float VELOCITY_GAIN = 50f;
    private static final float MAX_CONTROL_FORCE = 10000f;

    private final Body hand;
    private final Body forearm;
    private final Body upperArm;
    private final Vector2 shoulderAnchor;

    private final Vector2 targetVelocity = new Vector2();
    private final Vector2 velocityError = new Vector2();
    private final Vector2 controlForce = new Vector2();

    private ArmState state = ArmState.FREE;

    private Body contactedSurface;
    private Vector2 contactPoint;
    private RevoluteJoint gripJoint;

    public Arm(Vector2 position, Vector2 size, float angle, BodyDef.BodyType type, World world, float weight, float friction, float restitution, short groupIndex, Sides side) {

        Vector2 armSize = new Vector2(size.x, size.y * ARM_HEIGHT_RATIO);
        Vector2 forearmSize = new Vector2(size.x, size.y * FOREARM_HEIGHT_RATIO);
        Vector2 handSize = new Vector2(size.x, size.y * HAND_HEIGHT_RATIO);

        Vector2 armPosition = new Vector2(position.x, position.y + (size.y / 2) - (armSize.y / 2));
        Vector2 forearmPosition = new Vector2(position.x, position.y - (size.y / 2) + handSize.y + (forearmSize.y / 2));
        Vector2 handPosition = new Vector2(position.x, position.y - (size.y / 2) + (handSize.y / 2));

        float armMass = weight * ARM_MASS_RATIO;
        float forearmMass = weight * FOREARM_MASS_RATIO;
        float handMass = weight * HAND_MASS_RATIO;

        float armArea = armSize.x * armSize.y;
        float forearmArea = forearmSize.x * forearmSize.y;
        float handArea = handSize.x * handSize.y;

        float armDensity = armMass / armArea;
        float forearmDensity = forearmMass / forearmArea;
        float handDensity = handMass / handArea;

        upperArm = ShapeFactory.createRectangle(armPosition, armSize, angle, type, world, armDensity, friction, restitution, false, groupIndex);
        forearm = ShapeFactory.createRectangle(forearmPosition, forearmSize, angle, type, world, forearmDensity, friction, restitution, false, groupIndex);
        hand = ShapeFactory.createRectangle(handPosition, handSize, angle, type, world, handDensity, friction, restitution, false, groupIndex);

        upperArm.setUserData(null);
        forearm.setUserData(null);
        hand.setUserData(this);

        Vector2 armElbowAnchor = new Vector2(0, - (armSize.y / 2));
        Vector2 forearmElbowAnchor = new Vector2(0, (forearmSize.y / 2));
        Vector2 forearmWristAnchor = new Vector2(0, - (forearmSize.y / 2));
        Vector2 handWristAnchor = new Vector2(0, (handSize.y / 2));

        if (side == Sides.LEFT) {
            this.shoulderAnchor = new Vector2(0, (armSize.y / 2) - (size.x / 2));
            RevoluteJoint elbowJoint = JointFactory.createRevoluteJoint(upperArm, forearm, false, armElbowAnchor, forearmElbowAnchor, world);
            RevoluteJoint wristAnchor = JointFactory.createRevoluteJoint(forearm, hand, false, forearmWristAnchor, handWristAnchor, world, -45, 45);
        } else {
            this.shoulderAnchor = new Vector2(0, (armSize.y / 2) - (size.x / 2));
            RevoluteJoint elbowJoint = JointFactory.createRevoluteJoint(upperArm, forearm, false, armElbowAnchor, forearmElbowAnchor, world);
            RevoluteJoint wristAnchor = JointFactory.createRevoluteJoint(forearm, hand, false, forearmWristAnchor, handWristAnchor, world, -45, 45);
        }

        this.spriteUpperArm = createBodyPartSprite(armSize.x, armSize.y, "sprites/arm.png");
        this.spriteForearm = createBodyPartSprite(forearmSize.x, forearmSize.y, "sprites/forearm.png");
        this.spriteHand = createBodyPartSprite(handSize.x, handSize.y, "sprites/hand.png");

    }

    private Sprite createBodyPartSprite(float width, float height, String spritePath) {
        Texture texture = new Texture(Gdx.files.internal(spritePath));
        Sprite sprite = new Sprite(texture);
        sprite.setSize(width, height);
        sprite.setOrigin(width/2f, height/2f);
        return sprite;
    }

    private void syncSpriteToBody(Sprite sprite, Body body) {
        Vector2 pos = body.getPosition();
        sprite.setPosition(pos.x - sprite.getWidth() / 2f, pos.y - sprite.getHeight() / 2f);
        sprite.setRotation((float) Math.toDegrees(body.getAngle()));
    }

    public void setSurfaceContact(Body surface, Vector2 point) {
        contactedSurface = surface;
        contactPoint = new Vector2(point);
    }

    public void clearSurfaceContact(Body surface) {
        if (contactedSurface == surface && state == ArmState.FREE) {
            contactedSurface = null;
            contactPoint = null;
        }
    }

    public void updateSurfaceContact(Body surface, Vector2 point) {
        if (state == ArmState.FREE && contactedSurface == surface) {
            contactPoint.set(point);
        }
    }

    public void grab(World world) {
        if (state == ArmState.GRABBING) {
            return;
        }

        if (contactedSurface == null || contactPoint == null) {
            return;
        }

        Vector2 localHandPoint =
            hand.getLocalPoint(contactPoint);

        Vector2 localSurfacePoint =
            contactedSurface.getLocalPoint(contactPoint);

        System.out.println("Contact world: " + contactPoint);
        System.out.println("Surface body position: " + contactedSurface.getPosition());
        System.out.println("Surface local anchor: " + localSurfacePoint);
        System.out.println(
            "Local -> World: " +
                contactedSurface.getWorldPoint(localSurfacePoint)
        );

        gripJoint = JointFactory.createRevoluteJoint(
            hand,
            contactedSurface,
            false,
            localHandPoint,
            localSurfacePoint,
            world
        );

        state = ArmState.GRABBING;
    }

    public void release(World world) {
        if (state != ArmState.GRABBING) {
            return;
        }

        world.destroyJoint(gripJoint);

        gripJoint = null;
        state = ArmState.FREE;
    }

    public void draw(Batch batch) {
        syncSpriteToBody(spriteUpperArm, upperArm);
        syncSpriteToBody(spriteForearm, forearm);
        syncSpriteToBody(spriteHand, hand);

        spriteUpperArm.draw(batch);
        spriteForearm.draw(batch);
        spriteHand.draw(batch);
    }

    public Body getUpperArm() {
        return upperArm;
    }

    public Vector2 getShoulderAnchor() {
        return shoulderAnchor;
    }

    public Vector2 update(Vector2 mouseVelocity) {
        if (state == ArmState.GRABBING) {
            return new Vector2();
        }

        targetVelocity.set(mouseVelocity).scl(MOUSE_SENSITIVITY);

        velocityError
            .set(targetVelocity)
            .sub(hand.getLinearVelocity());

        controlForce
            .set(velocityError)
            .scl(VELOCITY_GAIN);

        if (controlForce.len() > MAX_CONTROL_FORCE) {
            controlForce.setLength(MAX_CONTROL_FORCE);
        }

        return controlForce;
    }

    public Body getHand() {
        return hand;
    }
}

