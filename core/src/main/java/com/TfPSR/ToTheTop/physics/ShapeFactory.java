package com.TfPSR.ToTheTop.physics;


import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.GeometryUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.ShortArray;

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

    /** Triangles smaller than this (m²) are skipped: Box2D rejects degenerate polygons. */
    private static final float MIN_TRIANGLE_AREA = 1e-4f;

    private static final EarClippingTriangulator TRIANGULATOR = new EarClippingTriangulator();

    /**
     * Solid body from any simple polygon, concave or with many vertices. The
     * outline is split into triangles (one convex fixture each), so unlike a
     * hollow chain loop the inside is solid: a limb pushed into it is pushed
     * back out instead of getting trapped.
     *
     * @param vertices outline in body-local meters, x/y interleaved
     */
    public static Body createSolidPolygon(Vector2 position, float[] vertices, BodyDef.BodyType type, World world, float density, float friction, float restitution, short groupIndex) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(position);
        bodyDef.type = type;
        Body body = world.createBody(bodyDef);

        ShortArray triangles = TRIANGULATOR.computeTriangles(vertices);
        float[] triangle = new float[6];
        PolygonShape shape = new PolygonShape();

        for (int i = 0; i < triangles.size; i += 3) {
            for (int corner = 0; corner < 3; corner++) {
                int vertex = triangles.get(i + corner) * 2;
                triangle[corner * 2] = vertices[vertex];
                triangle[corner * 2 + 1] = vertices[vertex + 1];
            }

            if (Math.abs(GeometryUtils.triangleArea(triangle[0], triangle[1], triangle[2], triangle[3], triangle[4], triangle[5])) < MIN_TRIANGLE_AREA)
                continue;

            shape.set(triangle);

            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = shape;
            fixtureDef.density = density;
            fixtureDef.friction = friction;
            fixtureDef.restitution = restitution;
            fixtureDef.filter.groupIndex = groupIndex;
            body.createFixture(fixtureDef);
        }

        shape.dispose();
        return body;
    }
}
