package com.TfPSR.ToTheTop.map;

import com.TfPSR.ToTheTop.core.Constants;
import com.badlogic.gdx.maps.MapGroupLayer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Vector2;

public class GameMap {

    private final TiledMap map;
    private final MapLayer playerLayer;
    private final MapLayer surfaceLayer;

    public GameMap(String mapPath) {
        TmxMapLoader loader = new TmxMapLoader();

        map = loader.load(mapPath);
        final MapGroupLayer gameplayLayer = (MapGroupLayer) map.getLayers().get("Gameplay");
        playerLayer = gameplayLayer.getLayers().get("Player");
        surfaceLayer = gameplayLayer.getLayers().get("Surface");
    }

    public MapObjects getSurfaceObjects() {
        return surfaceLayer.getObjects();
    }

    /**
     * Height in meters of the highest point of the climbing surfaces: the
     * summit the match is won at.
     *
     * <p>Tiled loads polygon vertices already in world coordinates, so the
     * maximum y is the topmost rock of the route. The top edge of the map is
     * higher than every rock and sending a player there would never fire.
     */
    public float getSummitY() {
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
