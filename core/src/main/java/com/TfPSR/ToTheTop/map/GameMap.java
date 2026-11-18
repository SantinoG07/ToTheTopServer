package com.TfPSR.ToTheTop.map;

import com.TfPSR.ToTheTop.core.Constants;
import com.badlogic.gdx.maps.MapGroupLayer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class GameMap {

    private final TiledMap map;
    private final MapLayer playerLayer;
    private final MapLayer surfaceLayer;
    private final Rectangle summitArea;

    /** Name of the rectangle, in the Player layer, that bounds the level sideways. */
    private static final String PLAY_AREA_OBJECT = "PlayArea";

    /** Name of the rectangle, in the Player layer, that marks the summit. */
    private static final String SUMMIT_OBJECT = "Summit";
    /** Map property, set in Tiled, with the seconds a match lasts. */
    private static final String TIME_LIMIT_PROPERTY = "timeLimit";
    /** Seconds a match lasts when the map does not set {@value #TIME_LIMIT_PROPERTY}. */
    private static final float DEFAULT_TIME_LIMIT = 300f;

    /** Extent of the fallback summit area, in meters: effectively unbounded. */
    private static final float FAR = 100000f;

    public GameMap(String mapPath) {
        TmxMapLoader loader = new TmxMapLoader();

        map = loader.load(mapPath);
        final MapGroupLayer gameplayLayer = (MapGroupLayer) map.getLayers().get("Gameplay");
        playerLayer = gameplayLayer.getLayers().get("Player");
        surfaceLayer = gameplayLayer.getLayers().get("Surface");
        summitArea = loadSummitArea();
    }

    public MapObjects getSurfaceObjects() {
        return surfaceLayer.getObjects();
    }

    /**
     * Area, in meters, a head has to enter to win the match. It is the
     * {@value #SUMMIT_OBJECT} rectangle drawn in Tiled; it may stick out above
     * the map, since a head standing on the top rock is higher than the map.
     */
    public Rectangle getSummitArea() {
        return summitArea;
    }

    /**
     * Side limits of the level, in meters: the {@value #PLAY_AREA_OBJECT}
     * rectangle drawn in Tiled, or {@code null} when the map has none. Only its
     * left and right edges are used.
     */
    public Rectangle getPlayArea() {
        MapObject area = playerLayer.getObjects().get(PLAY_AREA_OBJECT);
        if (!(area instanceof RectangleMapObject rectangle))
            return null;
        Rectangle pixels = rectangle.getRectangle();
        return new Rectangle(pixels.x / Constants.PIXELS_PER_METER, pixels.y / Constants.PIXELS_PER_METER,
                pixels.width / Constants.PIXELS_PER_METER, pixels.height / Constants.PIXELS_PER_METER);
    }

    /**
     * Seconds the match lasts before it is lost: the {@value #TIME_LIMIT_PROPERTY}
     * property of the map (Map > Map Properties in Tiled), or a default.
     */
    public float getTimeLimit() {
        Object value = map.getProperties().get(TIME_LIMIT_PROPERTY);
        if (value instanceof Number number && number.floatValue() > 0f)
            return number.floatValue();
        return DEFAULT_TIME_LIMIT;
    }

    /**
     * The Tiled summit rectangle in meters. A map without one falls back to
     * everything above the highest rock, the rule used before the object existed.
     */
    private Rectangle loadSummitArea() {
        MapObject summit = playerLayer.getObjects().get(SUMMIT_OBJECT);
        if (summit instanceof RectangleMapObject rectangle) {
            Rectangle pixels = rectangle.getRectangle();
            return new Rectangle(pixels.x / Constants.PIXELS_PER_METER, pixels.y / Constants.PIXELS_PER_METER,
                    pixels.width / Constants.PIXELS_PER_METER, pixels.height / Constants.PIXELS_PER_METER);
        }

        return new Rectangle(-FAR, highestRockY(), 2 * FAR, FAR);
    }

    /**
     * Height in meters of the highest point of the climbing surfaces. Tiled
     * loads polygon vertices already in world coordinates, so the maximum y is
     * the topmost rock of the route.
     */
    private float highestRockY() {
        float summit = 0f;

        for (MapObject rock : surfaceLayer.getObjects()) {
            if (!(rock instanceof PolygonMapObject polygon))
                continue;

            float[] vertices = polygon.getPolygon().getTransformedVertices();
            for (int i = 1; i < vertices.length; i += 2)
                summit = Math.max(summit, vertices[i] / Constants.PIXELS_PER_METER);
        }

        return summit;
    }

    public Vector2 getPlayerSpawn() {
        float x = playerLayer.getObjects().get("PlayerSpawn").getProperties().get("x", Float.class);
        float y = playerLayer.getObjects().get("PlayerSpawn").getProperties().get("y", Float.class);
        return pixelsToMeters(x, y);
    }

    private Vector2 pixelsToMeters(float x, float y) {
        return new Vector2(x / Constants.PIXELS_PER_METER, y / Constants.PIXELS_PER_METER);
    }

    public TiledMap getTiledMap() {
        return map;
    }
}
