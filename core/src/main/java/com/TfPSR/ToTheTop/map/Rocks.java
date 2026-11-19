package com.TfPSR.ToTheTop.map;

import com.TfPSR.ToTheTop.core.Constants;
import com.TfPSR.ToTheTop.physics.ShapeFactory;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;

import java.util.ArrayList;
import java.util.List;

public class Rocks {
    /** Tiled type of the rocks that crumble when held too long. */
    private static final String BREAKABLE_TYPE = "Breakable";

    ArrayList<Body> rocksBodies = new ArrayList<>();
    /** Breakable rocks in map order; the STATE packet sends them in this order. */
    private final List<BreakableRock> breakables = new ArrayList<>();



    public Rocks(MapObjects rocksObjects, World world) {
        for(MapObject rock : rocksObjects) {

            PolygonMapObject rockPolygon = (PolygonMapObject) rock;

            float[] rockVertices =
                rockPolygon.getPolygon().getTransformedVertices();

            Vector2[] vertices =
                new Vector2[rockVertices.length / 2];

            float minX = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;

            // Convertimos los vértices de Tiled a metros
            for(int i = 0; i < rockVertices.length; i += 2) {

                float x =
                    rockVertices[i] / Constants.PIXELS_PER_METER;

                float y =
                    rockVertices[i + 1] / Constants.PIXELS_PER_METER;

                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);

                vertices[i / 2] = new Vector2(x, y);
            }

            // Centro de la roca en coordenadas mundiales
            Vector2 rockPosition = new Vector2(
                (minX + maxX) / 2f,
                (minY + maxY) / 2f
            );

            // Convertimos los vértices de mundo -> local
            float[] localVertices = new float[vertices.length * 2];
            for(int i = 0; i < vertices.length; i++) {
                localVertices[i * 2] = vertices[i].x - rockPosition.x;
                localVertices[i * 2 + 1] = vertices[i].y - rockPosition.y;
            }

            // Solid (triangulated) instead of a hollow chain loop: Tiled can
            // still draw any concave shape, and a limb that gets pushed into
            // the rock is pushed back out instead of getting trapped inside.
            Body body = ShapeFactory.createSolidPolygon(
                rockPosition,
                localVertices,
                BodyDef.BodyType.StaticBody,
                world,
                0,
                1f,
                0f,
                (short) -2
            );

            if (isBreakable(rock)) {
                BreakableRock breakable = new BreakableRock(body,
                        new Rectangle(minX, minY, maxX - minX, maxY - minY));
                body.setUserData(breakable);
                breakables.add(breakable);
            } else {
                body.setUserData(this);
            }
            rocksBodies.add(body);
        }
    }

    /** Tiled stores the object type as "type" or, in newer versions, as "class". */
    static boolean isBreakable(MapObject rock) {
        Object type = rock.getProperties().get("type");
        if (type == null)
            type = rock.getProperties().get("class");
        return BREAKABLE_TYPE.equals(type);
    }

    public List<BreakableRock> getBreakables() {
        return breakables;
    }

    /** State byte of every breakable rock, in map order, for the STATE packet. */
    public byte[] breakableStates() {
        byte[] states = new byte[breakables.size()];
        for (int i = 0; i < states.length; i++)
            states[i] = (byte) breakables.get(i).wireState();
        return states;
    }

}
