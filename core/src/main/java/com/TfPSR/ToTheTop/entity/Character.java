package com.TfPSR.ToTheTop.entity;

import com.TfPSR.ToTheTop.physics.JointFactory;
import com.TfPSR.ToTheTop.physics.ShapeFactory;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
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

    // Share of the weight per body; each arm adds 0.15 (see Arm), total 1.0.
    // The torso is lighter and the limbs heavier than a real body to keep
    // jointed bodies within ~1:10 of each other, which Box2D needs to stay stable.
    private static final float HEAD_MASS_RATIO = 0.08f;
    private static final float TORSO_MASS_RATIO = 0.34f;

    private static final float LEG_MASS_RATIO = 0.14f;

    /**
     * Wiki 3.7.5: A/D swing the hanging body like a child on a swing. The push
     * is a small share of the weight and grows while the key is held, so
     * alternating keys in rhythm with the pendulum builds momentum gently.
     */
    private static final float SWING_WEIGHT_FACTOR = 0.15f;

    /** Seconds a held swing key takes to reach the full push. */
    private static final float SWING_RAMP_TIME = 0.5f;

    /** Above this sideways speed (m/s) the push stops, so the swing cannot grow without end. */
    private static final float MAX_SWING_SPEED = 4f;

    /** Angle above the horizontal of an impulse aimed with A or D, in degrees. */
    private static final float IMPULSE_DIAGONAL_ANGLE = 45f;

    /** Angle above the horizontal of an impulse aimed with W plus A or D, in degrees. */
    private static final float IMPULSE_STEEP_ANGLE = 65f;

    /**
     * Seconds the impulse pushes the torso for. A short push instead of an
     * instant kick lets the torso drag the limbs along naturally without
     * yanking the joints.
     */
    private static final float IMPULSE_PUSH_TIME = 0.12f;

    /** Seconds between two impulses, so the character cannot hop its way along. */
    private static final float IMPULSE_COOLDOWN = 2f;

    /**
     * Take-off speed of the whole body in the impulse of wiki 3.7.4, in m/s.
     * 5 m/s rises about 1.3 m straight up and reaches about 2.5 m on a 45 deg jump.
     */
    private static final float IMPULSE_SPEED = 5f;

    /**
     * Climbing is a pull-up: while one hand grips and the other is dragged,
     * the pointer movement the free arm cannot follow (past its reach) becomes
     * the speed of the body. With no such movement the body holds its height.
     */
    private static final float MAX_LIFT_SPEED = 2.5f;

    /** Share of the body velocity error corrected per step (whole ragdoll, keep low). */
    private static final float LIFT_RESPONSE = 0.2f;

    /** Smoothing of the lift speed between steps, so sparse pointer packets do not jerk the body. */
    private static final float LIFT_SMOOTHING = 0.3f;

    /** Ceiling of the lifting force, as a multiple of the character weight. */
    private static final float LIFT_MAX_WEIGHT_FACTOR = 1.5f;

    /**
     * A grip whose averaged force passes this multiple of the character weight
     * slips. Hanging costs 1x and a pull-up under 3x; catching a fall at
     * ~30 m/s still stays below, so only a sustained, explosive pull reaches it.
     */
    private static final float GRIP_BREAK_WEIGHT_FACTOR = 12f;

    private final World world;

    /** Total mass of the ten bodies, in kilograms. */
    private final float bodyMass;


    Vector2 position;

    private final Body head;
    private final Body torso;
    private final Arm leftArm;
    private final Arm rightArm;
    private final Body leftLeg;
    private final Body rightLeg;

    /**
     * Negative collision group of this character: its own bodies never collide
     * with each other, while bodies of other characters (other groups) do.
     */
    private final short groupIndex;

    /** Leg contacts with a surface; above zero the character is standing on one. */
    private int supportContacts;

    /** The ten simulated bodies, in the order the state packet sends them. */
    private final Body[] parts;

    /** Smoothed lift speed requested by the free hand, in m/s. */
    private final Vector2 liftVelocity = new Vector2();
    private final Vector2 liftForce = new Vector2();
    private final Vector2 liftTarget = new Vector2();
    private final Vector2 swingForce = new Vector2();
    /** Seconds the current swing key has been held, capped at the ramp time. */
    private float swingHeldTime;
    /** Direction of the last swing tick: -1 left, 1 right, 0 none. */
    private float swingDirection;
    /** Seconds left before the next impulse is allowed. */
    private float impulseCooldown;
    /** Force of the impulse in progress and the seconds it still pushes for. */
    private final Vector2 impulseForce = new Vector2();
    private float impulsePushTime;
    private final Vector2 reactionForce = new Vector2();

    public Character(Vector2 position, Vector2 size, float weight, World world, short groupIndex) {
        this.groupIndex = groupIndex;
        this.position = position;

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
        float legMass = weight * LEG_MASS_RATIO;

        float headArea = headSize.x * headSize.y;
        float torsoArea = torsoSize.x * torsoSize.y;
        float legArea = legsSize.x * legsSize.y;

        float headDensity = headMass / headArea;
        float torsoDensity = torsoMass / torsoArea;
        float legsDensity = legMass / legArea;

        this.world = world;

        this.head = ShapeFactory.createRectangle(headPosition, headSize, 0, BodyDef.BodyType.DynamicBody, world, headDensity, 1f, 0, false, groupIndex);
        this.torso = ShapeFactory.createRectangle(torsoPosition, torsoSize, 0, BodyDef.BodyType.DynamicBody, world, torsoDensity, 1f, 0, false, groupIndex);
        this.leftArm = new Arm(leftArmPosition, armsSize, 0, BodyDef.BodyType.DynamicBody, world, weight, 1f, 0, groupIndex, Sides.LEFT);
        this.rightArm = new Arm(rightArmPosition, armsSize, 0, BodyDef.BodyType.DynamicBody, world, weight, 1f, 0, groupIndex, Sides.RIGHT);
        this.leftLeg = ShapeFactory.createRectangle(leftLegPosition, legsSize, 0, BodyDef.BodyType.DynamicBody, world, legsDensity, 1f, 0, false, groupIndex);
        this.rightLeg = ShapeFactory.createRectangle(rightLegPosition, legsSize, 0, BodyDef.BodyType.DynamicBody, world, legsDensity, 1f, 0, false, groupIndex);


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
        leftArm.attachTo(torso, leftShoulderAnchor);
        rightArm.attachTo(torso, rightShoulderAnchor);
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

        // The contact listener identifies every body by its user data: which
        // character owns it, whether it is a hand (to grab) or a leg (to stand).
        for (Body part : parts)
            part.setUserData(new BodyPart(this, null, false));
        leftArm.getHand().setUserData(new BodyPart(this, leftArm, false));
        rightArm.getHand().setUserData(new BodyPart(this, rightArm, false));
        leftLeg.setUserData(new BodyPart(this, null, true));
        rightLeg.setUserData(new BodyPart(this, null, true));

        float mass = 0f;
        for (Body part : parts)
            mass += part.getMass();
        this.bodyMass = mass;
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

    /**
     * Lets go of every grip this character holds on {@code other}. Must run
     * before {@code other} is destroyed: Box2D destroys the grip joints with its
     * bodies, and the arms would keep pointing at freed native joints.
     */
    public void releaseGripsOn(Character other) {
        leftArm.releaseIfHolding(other, world);
        rightArm.releaseIfHolding(other, world);
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
     * the character rests on a surface, never while hanging or falling, and
     * once every {@link #IMPULSE_COOLDOWN} seconds.
     *
     * <p>It is aimed with the held keys: none or W jump straight up, A or D
     * jump diagonally to that side, and W with A or D makes that diagonal
     * steeper. Opposite keys cancel out. S is not used.
     *
     * @return true when the impulse was applied
     */
    public boolean impulse(boolean left, boolean right, boolean up) {
        if (!isResting() || isGripping() || impulseCooldown > 0f)
            return false;

        impulseCooldown = IMPULSE_COOLDOWN;
        float side = (right ? 1f : 0f) - (left ? 1f : 0f);
        float angle = side == 0f ? 90f : (up ? IMPULSE_STEEP_ANGLE : IMPULSE_DIAGONAL_ANGLE);
        // Only the torso is pushed, with the momentum the whole body needs to
        // take off at IMPULSE_SPEED; the limbs follow through their joints.
        float force = bodyMass * IMPULSE_SPEED / IMPULSE_PUSH_TIME;
        impulseForce.set(side * MathUtils.cosDeg(angle), MathUtils.sinDeg(angle)).scl(force);
        impulsePushTime = IMPULSE_PUSH_TIME;
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
        float gripLimit = bodyMass * world.getGravity().len() * GRIP_BREAK_WEIGHT_FACTOR;
        leftArm.releaseIfOverloaded(world, step, gripLimit);
        rightArm.releaseIfOverloaded(world, step, gripLimit);

        driveArm(leftArm, leftMousePressed, pointerDelta, step);
        driveArm(rightArm, rightMousePressed, pointerDelta, step);

        // The climbing stroke needs one hand on the rock and the other driven.
        // Without a grip there is no lift, so the character cannot swim in the air.
        if (isGripping() && (leftMousePressed || rightMousePressed)) {
            applyLift(step);
        } else {
            liftVelocity.setZero();
        }

        impulseCooldown = Math.max(0f, impulseCooldown - step);
        applyImpulsePush(step);
        applySwing(swingLeft, swingRight, step);
    }

    /** Keeps pushing the torso while the impulse started by {@link #impulse} lasts. */
    private void applyImpulsePush(float step) {
        if (impulsePushTime <= 0f)
            return;

        // The last tick may be shorter than a full step: scale it so the total
        // momentum is exactly the one requested.
        float share = Math.min(step, impulsePushTime) / step;
        impulsePushTime -= step;
        torso.applyForceToCenter(impulseForce.x * share, impulseForce.y * share, true);
    }

    /**
     * Pushes the hanging torso sideways while A or D is held (both cancel out).
     * Only with a grip, ramped in over {@link #SWING_RAMP_TIME} and stopped once
     * the torso already moves fast enough that way.
     */
    private void applySwing(boolean swingLeft, boolean swingRight, float step) {
        float direction = (swingRight ? 1f : 0f) - (swingLeft ? 1f : 0f);
        if (direction != swingDirection)
            swingHeldTime = 0f;
        swingDirection = direction;

        if (direction == 0f || !isGripping()) {
            swingHeldTime = 0f;
            return;
        }

        swingHeldTime = Math.min(SWING_RAMP_TIME, swingHeldTime + step);
        if (torso.getLinearVelocity().x * direction >= MAX_SWING_SPEED)
            return;

        float push = bodyMass * world.getGravity().len() * SWING_WEIGHT_FACTOR
                * (swingHeldTime / SWING_RAMP_TIME);
        applyClimbingForce(swingForce.set(direction * push, 0f));
    }

    /**
     * Pull-up of the whole body. The overflow of the driven hand sets the
     * desired speed; vertically the body otherwise holds its height (its weight
     * is compensated), horizontally it stays free to swing unless the hand
     * pulls sideways. The force is scaled by the body mass and capped, and the
     * shoulder rope of the gripping arm keeps the limbs together.
     */
    private void applyLift(float step) {
        Vector2 overflow = leftArm.getReachOverflow();
        float overflowX = overflow.x + rightArm.getReachOverflow().x;
        float overflowY = overflow.y + rightArm.getReachOverflow().y;

        liftVelocity.lerp(liftTarget.set(overflowX / step, overflowY / step), LIFT_SMOOTHING);
        liftVelocity.limit(MAX_LIFT_SPEED);

        Vector2 velocity = torso.getLinearVelocity();
        float gain = bodyMass * LIFT_RESPONSE / step;

        float forceX = Math.abs(overflowX) > 0f ? (liftVelocity.x - velocity.x) * gain : 0f;
        float forceY = (liftVelocity.y - velocity.y) * gain - bodyMass * world.getGravity().y;

        liftForce.set(forceX, forceY).limit(bodyMass * world.getGravity().len() * LIFT_MAX_WEIGHT_FACTOR);
        applyClimbingForce(liftForce);
    }

    /**
     * Pushes the torso and the gripped bodies back with the same force, split
     * between the gripping hands (action and reaction). Against a rock the
     * reaction does nothing; against another player it pulls them instead of
     * dragging both through the air, so two players can never fly together.
     */
    private void applyClimbingForce(Vector2 force) {
        torso.applyForceToCenter(force, true);

        int grips = (leftArm.isGrabbing() ? 1 : 0) + (rightArm.isGrabbing() ? 1 : 0);
        if (grips == 0)
            return;

        reactionForce.set(force).scl(-1f / grips);
        leftArm.pushGrip(reactionForce);
        rightArm.pushGrip(reactionForce);
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
