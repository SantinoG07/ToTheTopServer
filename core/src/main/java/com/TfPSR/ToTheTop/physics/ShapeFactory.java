package com.TfPSR.ToTheTop.physics;


import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class ShapeFactory {
    private ShapeFactory() {}

    public static Body createRectangle(final Vector2 position, final Vector2 size, float angle, final BodyDef.BodyType type, final World world, float density, float friction, float restitution, boolean isSensor, short groupIndex) {

        //define body
        final BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(position);
        bodyDef.type = type;
        bodyDef.angle = (float) Math.toRadians(angle);
        final Body body = world.createBody(bodyDef);

        //define fixture
        final PolygonShape shape = new PolygonShape();
        shape.setAsBox((size.x / 2), (size.y / 2));
        final FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = density;
        fixtureDef.friction = friction;
        fixtureDef.restitution = restitution;
        fixtureDef.isSensor = isSensor;
        fixtureDef.filter.groupIndex = groupIndex;

        body.createFixture(fixtureDef);
        shape.dispose();

        return body;
    }

    public static Body createCircle(Vector2 position,float radius,BodyDef.BodyType type, World world, float density, float friction, float restitution, boolean isSensor, short groupIndex) {

        //define body
        final BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(position);
        bodyDef.type = type;
        final Body body = world.createBody(bodyDef);

        //define fixture
        final CircleShape shape = new CircleShape();
        shape.setRadius(radius);
        final FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = density;
        fixtureDef.friction = friction;
        fixtureDef.restitution = restitution;
        fixtureDef.isSensor = isSensor;
        fixtureDef.filter.groupIndex = groupIndex;

        body.createFixture(fixtureDef);
        shape.dispose();

        return body;

    }

    public static Body createClosedChain(Vector2 position, Vector2[] vertices, BodyDef.BodyType type, World world, float density, float friction, float restitution, boolean isSensor, short groupIndex) {

        //define body
        final BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(position);
        bodyDef.type = type;
        final Body body = world.createBody(bodyDef);

        //define fixture
        final ChainShape shape = new ChainShape();
        shape.createLoop(vertices);
        final FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = density;
        fixtureDef.friction = friction;
        fixtureDef.restitution = restitution;
        fixtureDef.isSensor = isSensor;
        fixtureDef.filter.groupIndex = groupIndex;

        body.createFixture(fixtureDef);
        shape.dispose();

        return body;

    }
}
