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
import com.badlogic.gdx.physics.box2d.joints.RopeJoint;

public class Arm {
    private Sprite spriteUpperArm;
    private Sprite spriteForearm;
    private Sprite spriteHand;

    private static final float ARM_HEIGHT_RATIO = 0.47f;
    private static final float FOREARM_HEIGHT_RATIO = 0.4f;
    private static final float HAND_HEIGHT_RATIO = 0.13f;

    // Share of the character weight per segment (see Character for the whole
    // split). Heavier than a real arm on purpose: Box2D chains stay stable when
    // jointed bodies are within ~1:10 of each other, and a real hand is 1:50
    // of the torso. The controllers scale with mass, so the feel is the same.
    private static final float ARM_MASS_RATIO = 0.06f;
    private static final float FOREARM_MASS_RATIO = 0.05f;
    private static final float HAND_MASS_RATIO = 0.04f;

    /** Fraction of the pointer movement (meters) that moves the hand target. */
    private static final float HAND_POINTER_SCALE = 0.2f;

    /** How fast the hand closes the gap to its target, in 1/s. */
    private static final float POSITION_GAIN = 12f;

    /** Top speed the controller asks of the hand, in m/s. */
    private static final float MAX_HAND_SPEED = 6f;

    /**
     * Share of the velocity error corrected in one physics step. The force is
     * scaled by the hand mass and the step, so the loop stays stable for any
     * mass or tick rate as long as this is below 2; below 1 it never overshoots.
     */
    private static final float VELOCITY_RESPONSE = 0.8f;

    /**
     * Time over which the grip force is averaged before judging an overload,
     * in seconds. Catching a fall is a huge but one-tick peak; a ragdoll being
     * torn apart is a huge force that lasts, and only that should make it slip.
     */
    private static final float GRIP_STRAIN_TIME = 0.3f;

    /** Ceiling of the force a hand can exert, as a multiple of the arm weight. */
    private static final float MAX_CONTROL_WEIGHT_FACTOR = 3f;

    private final Body hand;
    private final Body forearm;
    private final Body upperArm;
    private final Vector2 shoulderAnchor;

    /** Distance from the shoulder joint to the hand center with the arm stretched. */
    private final float reach;
    /** Distance from the shoulder joint to the fingertips with the arm stretched. */
    private final float armLength;
    /** Mass of the three segments, held up by the controller while driven. */
    private final float armMass;

    /** Torso this arm hangs from, and the shoulder point on it. */
    private Body torso;
    private Vector2 torsoAnchor;
    /** Ties the shoulder to the grip point while grabbing, see {@link #grab}. */
    private RopeJoint ropeJoint;

    /** Part of the last pointer movement the arm could not follow, in meters. */
    private final Vector2 reachOverflow = new Vector2();
    private final Vector2 requestedOffset = new Vector2();

    /** Hand target relative to the shoulder, moved by the pointer while driven. */
    private final Vector2 targetOffset = new Vector2();
    private boolean driven;

    private final Vector2 target = new Vector2();
    private final Vector2 desiredVelocity = new Vector2();
    private final Vector2 controlForce = new Vector2();

    private ArmState state = ArmState.FREE;

    private Body contactedSurface;
    private final Vector2 contactPoint = new Vector2();
    private int surfaceContacts;
    /** Farthest a contact point can be from the hand center and still be grabbed. */
    private final float gripTolerance;
    private RevoluteJoint gripJoint;
    /** Grip force averaged over {@link #GRIP_STRAIN_TIME}, in newtons. */
    private float gripStrain;

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
        this.armMass = armMass + forearmMass + handMass;
        // the shoulder joint sits half a width below the top of the arm
        this.reach = size.y - (handSize.y / 2) - (size.x / 2);
        this.armLength = size.y - (size.x / 2);
        this.gripTolerance = handSize.len();

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

    /**
     * A rock started touching the hand. Contacts are counted because a rock
     * outline is made of many edges and the hand can touch several at once.
     */
    public void setSurfaceContact(Body surface, Vector2 point) {
        surfaceContacts++;
        contactedSurface = surface;
        contactPoint.set(point);
    }

    /**
     * A rock stopped touching the hand. It is tracked in every state: a contact
     * that ends while gripping must not survive until the next grab, or the
     * hand would latch onto a point it no longer touches.
     */
    public void clearSurfaceContact(Body surface) {
        surfaceContacts = Math.max(0, surfaceContacts - 1);
        if (surfaceContacts == 0)
            contactedSurface = null;
    }

    /** Refreshes the touched point every step the hand stays on a rock. */
    public void updateSurfaceContact(Body surface, Vector2 point) {
        contactedSurface = surface;
        contactPoint.set(point);
    }

    /** Links the arm to the torso point its shoulder joint uses. */
    public void attachTo(Body torso, Vector2 torsoAnchor) {
        this.torso = torso;
        this.torsoAnchor = new Vector2(torsoAnchor);
    }

    /**
     * Holds the hand to the touched surface. Besides the wrist-to-rock joint, a
     * rope ties the shoulder straight to the grip point with the arm length:
     * the whole body hangs on that single link, so the light arm chain is never
     * stretched apart by the much heavier torso.
     */
    public void grab(World world) {
        if (state == ArmState.GRABBING) {
            return;
        }

        // Only a rock the hand touches right now, at a point on the hand itself.
        if (contactedSurface == null || hand.getPosition().dst(contactPoint) > gripTolerance) {
            return;
        }

        Vector2 localHandPoint =
            hand.getLocalPoint(contactPoint);

        Vector2 localSurfacePoint =
            contactedSurface.getLocalPoint(contactPoint);

        gripJoint = JointFactory.createRevoluteJoint(
            hand,
            contactedSurface,
            false,
            localHandPoint,
            localSurfacePoint,
            world
        );

        if (torso != null)
            ropeJoint = JointFactory.createRopeJoint(torso, contactedSurface, torsoAnchor, localSurfacePoint, armLength, world);

        gripStrain = 0f;
        state = ArmState.GRABBING;
    }

    public void release(World world) {
        if (state != ArmState.GRABBING) {
            return;
        }

        world.destroyJoint(gripJoint);
        if (ropeJoint != null)
            world.destroyJoint(ropeJoint);

        gripJoint = null;
        ropeJoint = null;
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

    /**
     * Force that moves the free hand toward its target for this step.
     *
     * <p>The target is the shoulder plus an offset. When the drive starts the
     * offset is wherever the hand already is, so it never jumps; after that
     * each pointer movement shifts it, clamped to the arm reach. Because the
     * pointer is absolute, a lost or duplicated packet only delays the target
     * instead of producing a speed spike.
     *
     * <p>The target becomes a capped desired velocity, and the force corrects
     * a fixed share of the velocity error per step scaled by the hand mass.
     * The arm weight is compensated, so a held hand does not sag.
     *
     * <p>Movement that would take the target past the reach is kept in
     * {@link #getReachOverflow()}: while the other hand grips, the character
     * uses it to pull the body up after the hand.
     *
     * @param pointerDelta pointer movement since the previous step, in meters
     * @param gravity      world gravity, in m/s²
     * @param step         physics step, in seconds
     */
    public Vector2 drive(Vector2 pointerDelta, Vector2 gravity, float step) {
        controlForce.setZero();
        reachOverflow.setZero();
        if (state == ArmState.GRABBING) {
            driven = false;
            return controlForce;
        }

        Vector2 shoulder = upperArm.getWorldPoint(shoulderAnchor);
        if (!driven) {
            driven = true;
            targetOffset.set(hand.getPosition()).sub(shoulder);
        }

        requestedOffset.set(targetOffset).mulAdd(pointerDelta, HAND_POINTER_SCALE);
        targetOffset.set(requestedOffset).limit(reach);
        reachOverflow.set(requestedOffset).sub(targetOffset);
        target.set(shoulder).add(targetOffset);

        desiredVelocity.set(target).sub(hand.getPosition())
            .scl(POSITION_GAIN)
            .limit(MAX_HAND_SPEED);

        controlForce.set(desiredVelocity).sub(hand.getLinearVelocity())
            .scl(hand.getMass() * VELOCITY_RESPONSE / step)
            .mulAdd(gravity, -armMass)
            .limit(armMass * gravity.len() * MAX_CONTROL_WEIGHT_FACTOR);

        return controlForce;
    }

    /** Lets the arm hang; the next drive starts from wherever the hand is. */
    public void relax() {
        driven = false;
        reachOverflow.setZero();
    }

    /**
     * Lets go when the averaged grip force passes {@code maxForce}, like a hand
     * slipping. The average lets a hand catch a fast fall (a short peak) while
     * still releasing a sustained, explosive pull. The joints would otherwise transmit any force, and an extreme
     * one (a hard fall, a body shoved into the rock) tears the ragdoll apart.
     *
     * @return true if the hand let go
     */
    public boolean releaseIfOverloaded(World world, float step, float maxForce) {
        if (state != ArmState.GRABBING)
            return false;

        float inverseStep = 1f / step;
        float gripForce = gripJoint.getReactionForce(inverseStep).len();
        float ropeForce = ropeJoint == null ? 0f : ropeJoint.getReactionForce(inverseStep).len();
        float force = Math.max(gripForce, ropeForce);
        gripStrain += (force - gripStrain) * Math.min(1f, step / GRIP_STRAIN_TIME);
        if (gripStrain <= maxForce)
            return false;

        release(world);
        return true;
    }

    /** Pointer movement of the last drive that went past the arm reach, in meters. */
    public Vector2 getReachOverflow() {
        return reachOverflow;
    }

    public Body getForearm() {
        return forearm;
    }

    public Body getHand() {
        return hand;
    }

    /** True while the hand is held to the surface it grabbed. */
    public boolean isGrabbing() {
        return state == ArmState.GRABBING;
    }

    /** Frees the sprites of this arm; its bodies are destroyed by its character. */
    public void dispose() {
        spriteUpperArm.getTexture().dispose();
        spriteForearm.getTexture().dispose();
        spriteHand.getTexture().dispose();
    }
}

