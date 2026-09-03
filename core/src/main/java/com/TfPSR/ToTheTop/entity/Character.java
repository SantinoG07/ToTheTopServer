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

    private final World world;

    Vector2 position;

    private final Body head;
    private final Body torso;
    private final Arm leftArm;
    private final Arm rightArm;
    private final Body leftLeg;
    private final Body rightLeg;

    private final short groupIndex = -1;

    public Character(Vector2 position, Vector2 size, float weight, World world) {
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
        this.leftArm = new Arm(leftArmPosition, armsSize, 0, BodyDef.BodyType.DynamicBody, world, armsDensity, 1f, 0, groupIndex, Sides.LEFT);
        this.rightArm = new Arm(rightArmPosition, armsSize, 0, BodyDef.BodyType.DynamicBody, world, armsDensity, 1f, 0, groupIndex, Sides.RIGHT);
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
        RevoluteJoint leftLegJoint = JointFactory.createRevoluteJoint(torso, leftLeg, false, leftTorsoHipAnchor, leftLegHipAnchor, world, -90, 90);
        RevoluteJoint rightLegJoint = JointFactory.createRevoluteJoint(torso, rightLeg, false, rightTorsoHipAnchor, rightLegHipAnchor, world, -90, 90);
        this.spriteHead = createBodyPartSprite(headSize.x, headSize.y, "sprites/head.png");
        this.spriteTorso= createBodyPartSprite(torsoSize.x, torsoSize.y, "sprites/torso.png");
        this.spriteLeftLeg= createBodyPartSprite(legsSize.x, legsSize.y, "sprites/leg.png");
        this.spriteRightLeg= createBodyPartSprite(legsSize.x, legsSize.y, "sprites/leg.png");
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


    public void update(Vector2 mouseVelocity, boolean leftMousePressed, boolean rightMousePressed) {
        if (leftMousePressed) {
            applyInternalArmForce(leftArm, mouseVelocity);
        }

        if (rightMousePressed) {
            applyInternalArmForce(rightArm, mouseVelocity);
        }
    }

    private void applyInternalArmForce(Arm arm, Vector2 mouseVelocity) {
        Vector2 force = arm.update(mouseVelocity);

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
}
