package com.TfPSR.ToTheTop.entity;

import com.TfPSR.ToTheTop.physics.JointFactory;
import com.TfPSR.ToTheTop.physics.ShapeFactory;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;

public class Character {

    private Sprite spriteHead;
    private Sprite spriteTorso;
    private Sprite spriteLeftLeg;
    private Sprite spriteRightLeg;

    private static final float HEAD_WIDTH_RATIO = 0.28f;
    private static final float HEAD_HEIGHT_RATIO = 0.13f;

    private static final float CHEST_WIDTH_RATIO = 0.62f;
    private static final float CHEST_HEIGHT_RATIO = 0.37f;

    private static final float ARMS_WIDTH_RATIO = 0.18f;
    private static final float ARMS_HEIGHT_RATIO = 0.45f;

    private static final float LEGS_WIDTH_RATIO = 0.25f;
    private static final float LEGS_HEIGHT_RATIO = 0.5f;

    private static final float HEAD_MASS_RATIO = 0.08f;
    private static final float TORSO_MASS_RATIO = 0.50f;

    private static final float ARM_MASS_RATIO = 0.03f;

    private static final float LEG_MASS_RATIO = 0.15f;

    /** Lateral push applied to the torso by A/D while a hand is gripping. */
    private static final float SWING_FORCE = 400f;

    /** Vertical take-off speed of the impulse of wiki 3.7.4, in m/s. */
    private static final float IMPULSE_SPEED = 2.2f;

    /**
     * Force applied to the torso per (m/s) of pointer speed while a hand holds
     * the rock. The arm servo alone only repositions the free hand: its force
     * and the counter-force on the torso cancel out, so without this pull a
     * gripped character could never lift its own weight.
     */
    private static final float CLIMB_GAIN = 120f;

    /** Hard ceiling of the pull, as a multiple of the character weight. */
    private static final float CLIMB_MAX_WEIGHT_FACTOR = 3f;

    private final World world;

    private final Vector2 pointerVelocity = new Vector2();

    /** Weight in kilograms, used to size the climbing pull. */
    private final float weight;

    Vector2 position;

    private final Body head;
    private final Body torso;
    private final Arm leftArm;
    private final Arm rightArm;
    private final Body leftLeg;
    private final Body rightLeg;

    private final short groupIndex = -1;

    /** Leg contacts with a surface; above zero the character is standing on one. */
    private int supportContacts;

    /** The ten simulated bodies, in the order the state packet sends them. */
    private final Body[] parts;

    /** Scratch vector of the climbing pull, reused every tick. */
    private final Vector2 climbPull = new Vector2();

    public Character(Vector2 position, Vector2 size, float weight, World world) {
        this.position = position;
        this.weight = weight;

        Vector2 headSize = new Vector2(size.x * HEAD_WIDTH_RATIO, size.y * HEAD_HEIGHT_RATIO);
        Vector2 torsoSize = new Vector2(size.x * CHEST_WIDTH_RATIO, size.y * CHEST_HEIGHT_RATIO);
        Vector2 armsSize = new Vector2(size.x * ARMS_WIDTH_RATIO, size.y * ARMS_HEIGHT_RATIO);
        Vector2 legsSize = new Vector2(size.x * LEGS_WIDTH_RATIO, size.y * LEGS_HEIGHT_RATIO);

        Vector2 headPosition = new Vector2(position.x, position.y + (size.y / 2) - (headSize.y / 2));
        Vector2 torsoPosition = new Vector2(position.x, position.y + (size.y / 2) - headSize.y - (torsoSize.y / 2));
        Vector2 leftArmPosition = new Vector2(position.x - (torsoSize.x / 2) - (armsSize.x / 2), position.y + (size.y / 2) - headSize.y - (armsSize.y / 2));
        Vector2 rightArmPosition = new Vector2(position.x + (torsoSize.x / 2) + (armsSize.x / 2), position.y + (size.y / 2) - headSize.y - (armsSize.y / 2));
        Vector2 leftLegPosition = new Vector2(position.x - (torsoSize.x / 2) + (legsSize.x / 2), position.y - (size.y / 2) + (legsSize.y / 2));
        Vector2 rightLegPosition = new Vector2(position.x + (torsoSize.x / 2) - (legsSize.x / 2), position.y - (size.y / 2) + (legsSize.y / 2));

        float headMass = weight * HEAD_MASS_RATIO;
        float torsoMass = weight * TORSO_MASS_RATIO;
        float armsMass = weight * ARM_MASS_RATIO;
        float legMass = weight * LEG_MASS_RATIO;

        float headArea = headSize.x * headSize.y;
        float torsoArea = torsoSize.x * torsoSize.y;
        float armArea = armsSize.x * armsSize.y;
        float legArea = legsSize.x * legsSize.y;

        float headDensity = headMass / headArea;
        float torsoDensity = torsoMass / torsoArea;
        float armsDensity = armsMass / armArea;
        float legsDensity = legMass / legArea;

        this.world = world;

        this.head = ShapeFactory.createRectangle(headPosition, headSize, 0, BodyDef.BodyType.DynamicBody, world, headDensity, 1f, 0, false, groupIndex);
        this.torso = ShapeFactory.createRectangle(torsoPosition, torsoSize, 0, BodyDef.BodyType.DynamicBody, world, torsoDensity, 1f, 0, false, groupIndex);
        this.leftArm = new Arm(leftArmPosition, armsSize, 0, BodyDef.BodyType.DynamicBody, world, weight, 1f, 0, groupIndex, Sides.LEFT);
        this.rightArm = new Arm(rightArmPosition, armsSize, 0, BodyDef.BodyType.DynamicBody, world, weight, 1f, 0, groupIndex, Sides.RIGHT);
        this.leftLeg = ShapeFactory.createRectangle(leftLegPosition, legsSize, 0, BodyDef.BodyType.DynamicBody, world, legsDensity, 1f, 0, false, groupIndex);
        this.rightLeg = ShapeFactory.createRectangle(rightLegPosition, legsSize, 0, BodyDef.BodyType.DynamicBody, world, legsDensity, 1f, 0, false, groupIndex);

        // The contact listener identifies the legs of this character by their
        // user data, to know whether it is resting on a surface.
        leftLeg.setUserData(this);
        rightLeg.setUserData(this);

        Vector2 torsoNeckAnchor = new Vector2(0, (torsoSize.y / 2));
        Vector2 headNeckAnchor = new Vector2(0, - (headSize.y / 2));
        Vector2 leftShoulderAnchor = new Vector2( - (torsoSize.x / 1.5f), (torsoSize.y / 2) - (armsSize.x / 2));
        Vector2 rightShoulderAnchor = new Vector2((torsoSize.x / 1.5f), (torsoSize.y / 2) - (armsSize.x / 2));
        Vector2 leftTorsoHipAnchor = new Vector2( - (torsoSize.x / 2) + (legsSize.x / 2), - (torsoSize.y / 2));
        Vector2 leftLegHipAnchor = new Vector2(0, (legsSize.y / 2));
        Vector2 rightTorsoHipAnchor = new Vector2((torsoSize.x / 2) - (legsSize.x / 2), - (torsoSize.y / 2));
        Vector2 rightLegHipAnchor = new Vector2(0, (legsSize.y / 2));

        RevoluteJoint neckJoint = JointFactory.createRevoluteJoint(torso, head, false, torsoNeckAnchor, headNeckAnchor, world, -45, 45);
        RevoluteJoint leftShoulderJoint = JointFactory.createRevoluteJoint(torso, leftArm.getUpperArm(), false, leftShoulderAnchor, leftArm.getShoulderAnchor(), world);
        RevoluteJoint rightShoulderJoint = JointFactory.createRevoluteJoint(torso, rightArm.getUpperArm(), false, rightShoulderAnchor, rightArm.getShoulderAnchor(), world);
        RevoluteJoint leftLegJoint = JointFactory.createRevoluteJoint(torso, leftLeg, false, leftTorsoHipAnchor, leftLegHipAnchor, world, -90, 90);
        RevoluteJoint rightLegJoint = JointFactory.createRevoluteJoint(torso, rightLeg, false, rightTorsoHipAnchor, rightLegHipAnchor, world, -90, 90);
        this.spriteHead = createBodyPartSprite(headSize.x, headSize.y, "sprites/head.png");
        this.spriteTorso= createBodyPartSprite(torsoSize.x, torsoSize.y, "sprites/torso.png");
        this.spriteLeftLeg= createBodyPartSprite(legsSize.x, legsSize.y, "sprites/leg.png");
        this.spriteRightLeg= createBodyPartSprite(legsSize.x, legsSize.y, "sprites/leg.png");

        // Order fixed by the state packet contract: head, torso, then each arm
        // top to bottom, then the legs. The client depends on it.
        this.parts = new Body[]{
            head,
            torso,
            leftArm.getUpperArm(), leftArm.getForearm(), leftArm.getHand(),
            rightArm.getUpperArm(), rightArm.getForearm(), rightArm.getHand(),
            leftLeg,
            rightLeg
        };
    }

    /** The ten simulated bodies, in the order the state packet sends them. */
    public Body[] getParts() {
        return parts;
    }

    public boolean isLeftHandGripping() {
        return leftArm.isGrabbing();
    }

    public boolean isRightHandGripping() {
        return rightArm.isGrabbing();
    }

    /** True while either hand is latched to a surface. */
    public boolean isGripping() {
        return leftArm.isGrabbing() || rightArm.isGrabbing();
    }

    /** Called by the contact listener when a leg starts touching a surface. */
    public void addSupport() {
        supportContacts++;
    }

    /** Called by the contact listener when a leg stops touching a surface. */
    public void removeSupport() {
        if (supportContacts > 0)
            supportContacts--;
    }

    /**
     * True while a leg rests on a surface. Hanging from a hand is not resting,
     * and neither is falling, so the impulse is refused in both cases.
     */
    public boolean isResting() {
        return supportContacts > 0;
    }

    /**
     * Wiki 3.7.4: a small jump, driven from the torso. It is only allowed while
     * the character rests on a surface, never while hanging or falling.
     *
     * @return true when the impulse was applied
     */
    public boolean impulse() {
        if (!isResting() || isGripping())
            return false;

        Vector2 center = torso.getWorldCenter();
        torso.applyLinearImpulse(0f, IMPULSE_SPEED * torso.getMass(),
                center.x, center.y, true);
        return true;
    }


    private Sprite createBodyPartSprite(float width, float height, String spritePath) {
        Texture texture = new Texture(Gdx.files.internal(spritePath));
        Sprite sprite = new Sprite(texture);
        sprite.setSize(width, height);
        sprite.setOrigin(width/2f, height/2f);

        return sprite;
    }
    private void syncSpriteToBody(Sprite sprite, Body body) {
        Vector2 pos = body.getPosition(); // centro del body en Box2D
        sprite.setPosition(pos.x - sprite.getWidth() / 2f, pos.y - sprite.getHeight() / 2f);
        sprite.setRotation((float) Math.toDegrees(body.getAngle()));
    }

    public void draw(Batch batch) {
        syncSpriteToBody(spriteHead, head);
        syncSpriteToBody(spriteLeftLeg, leftLeg);
        syncSpriteToBody(spriteRightLeg, rightLeg);
        syncSpriteToBody(spriteTorso, torso);

        leftArm.draw(batch);
        rightArm.draw(batch);
        spriteHead.draw(batch);
        spriteLeftLeg.draw(batch);
        spriteRightLeg.draw(batch);
        spriteTorso.draw(batch);
    }

    public void onLeftMouseReleased() {
        leftArm.grab(world);
    }

    public void onRightMouseReleased() {
        rightArm.grab(world);
    }

    public void releaseLeftHand() {
        leftArm.release(world);
    }

    public void releaseRightHand() {
        rightArm.release(world);
    }


    /**
     * @param pointerDelta pointer movement since the previous step, in meters
     * @param step         physics step, in seconds
     */
    public void update(Vector2 pointerDelta, float step, boolean leftMousePressed, boolean rightMousePressed,
                       boolean swingLeft, boolean swingRight) {
        driveArm(leftArm, leftMousePressed, pointerDelta, step);
        driveArm(rightArm, rightMousePressed, pointerDelta, step);

        Vector2 mouseVelocity = pointerVelocity.set(pointerDelta).scl(1f / step);

        // While a hand holds the rock, dragging the pointer pulls the body in
        // that direction: this is the actual climbing stroke. Without a grip the
        // pull is skipped, so the character cannot swim through the air.
        if (isGripping() && (leftMousePressed || rightMousePressed)) {
            applyClimbPull(mouseVelocity);
        }

        // Wiki 3.7.5: A/D push the torso sideways, and only while a hand holds
        // a surface. Both keys cancel out.
        if (swingLeft || swingRight) {
            if (isGripping()) {
                float direction = (swingRight ? 1f : 0f) - (swingLeft ? 1f : 0f);
                torso.applyForceToCenter(direction * SWING_FORCE, 0, true);
            }
        }
    }

    /**
     * Pulls the torso toward the pointer movement, anchored to the gripping
     * hand. Capped to a few times the character weight so a flick of the mouse
     * cannot launch the body.
     *
     * <p>The pull is remembered between input packets: at low frame rates the
     * pointer only changes on some ticks, and without this the character would
     * only be pulled on those few ticks and could never climb.
     */
    private void applyClimbPull(Vector2 pointerVelocity) {
        if (pointerVelocity.len2() > 0.0001f) {
            climbPull.set(pointerVelocity).scl(CLIMB_GAIN);

            float max = weight * 9.8f * CLIMB_MAX_WEIGHT_FACTOR;
            if (climbPull.len2() > max * max)
                climbPull.setLength(max);
        } else {
            // No fresh pointer data: keep the stroke alive for a few ticks.
            climbPull.scl(0.85f);
        }

        torso.applyForceToCenter(climbPull, true);
    }


    /**
     * Moves a hand while its button is held. The same force pushes the torso
     * back, so the arm alone cannot lift or launch the body.
     */
    private void driveArm(Arm arm, boolean pressed, Vector2 pointerDelta, float step) {
        if (!pressed) {
            arm.relax();
            return;
        }

        Vector2 force = arm.drive(pointerDelta, world.getGravity(), step);

        arm.getHand().applyForceToCenter(force, true);

        torso.applyForceToCenter(
            force.x * -1f,
            force.y * -1f,
            true
        );
    }

    public Vector2 getPosition() {
        return head.getPosition();
    }

    /**
     * Removes every body of this character from the world and frees its sprites.
     * Used when its slot leaves the match; the joints of a destroyed body are
     * destroyed with it, including the grip joint against a surface.
     */
    public void dispose() {
        for (Body body : parts)
            world.destroyBody(body);

        spriteHead.getTexture().dispose();
        spriteTorso.getTexture().dispose();
        spriteLeftLeg.getTexture().dispose();
        spriteRightLeg.getTexture().dispose();
        leftArm.dispose();
        rightArm.dispose();
    }
}
