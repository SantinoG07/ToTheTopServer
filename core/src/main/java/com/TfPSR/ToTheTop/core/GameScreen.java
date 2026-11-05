package com.TfPSR.ToTheTop.core;

import com.TfPSR.ToTheTop.*;
import com.TfPSR.ToTheTop.asset.AssetService;
import com.TfPSR.ToTheTop.asset.SoundAsset;
import com.TfPSR.ToTheTop.entity.Character;
import com.TfPSR.ToTheTop.map.GameMap;
import com.TfPSR.ToTheTop.map.Rocks;
import com.TfPSR.ToTheTop.network.InputPacket;
import com.TfPSR.ToTheTop.network.InputState;
import com.TfPSR.ToTheTop.network.PartState;
import com.TfPSR.ToTheTop.network.PlayerState;
import com.TfPSR.ToTheTop.network.StatePacket;
import com.TfPSR.ToTheTop.network.ThreadServer;
import com.TfPSR.ToTheTop.physics.GameContactListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class GameScreen extends ScreenAdapter {
    private final Vector2 GRAVEDAD = new Vector2(0, -9.8f);

    private static final int MAX_PLAYERS = 4;
    private static final Vector2 CHARACTER_SIZE = new Vector2(0.6f, 1.80f);
    private static final float CHARACTER_WEIGHT = 80f;
    private static final float SPAWN_SPACING = 3f;
    private static final float CAMERA_ZOOM = 1.5f;

    /** The simulation and the state broadcast both run on this fixed tick. */
    private static final float FIXED_STEP = 1f / 60f;
    private static final int MAX_STEPS_PER_FRAME = 5;

    private final Main game;
    private final OrthographicCamera camera;
    private final Batch batch;

    private final World world;
    private final Box2DDebugRenderer debugRenderer;
    private final Character[] players;

    private final GameMap map;
    private final OrthogonalTiledMapRenderer mapRenderer;

    private final AssetService assetService;
    private final Sound dash;

    private final Rocks rocks;

    private final ThreadServer server;

    /** Previous pointer and buttons of every slot, to derive velocity and edges. */
    private final float[] previousPointerX;
    private final float[] previousPointerY;
    private final boolean[] hasPointer;
    private final boolean[] previousLeftButton;
    private final boolean[] previousRightButton;

    /** Event bits of the previous tick of every slot, to react on the rising edge. */
    private final int[] previousEvents;

    /** Paused players keep their body but stop receiving input until resumed. */
    private final boolean[] paused;

    private float accumulator = 0f;

    public GameScreen(Main game, AssetService assetService) {
        this.game = game;
        this.camera = game.getCamera();
        this.batch = game.getBatch();
        this.assetService = assetService;
        this.dash = assetService.get(SoundAsset.DASH);
        this.server = game.getServer();

        this.debugRenderer = new Box2DDebugRenderer();
        map = new GameMap("maps/map.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map.getTiledMap(), 1f / Constants.PIXELS_PER_METER);

        world = new World(GRAVEDAD, true);
        world.setContactListener(new GameContactListener());

        rocks = new Rocks(map.getSurfaceObjects(), world);

        players = createPlayers();

        previousPointerX = new float[MAX_PLAYERS];
        previousPointerY = new float[MAX_PLAYERS];
        hasPointer = new boolean[MAX_PLAYERS];
        previousLeftButton = new boolean[MAX_PLAYERS];
        previousRightButton = new boolean[MAX_PLAYERS];
        previousEvents = new int[MAX_PLAYERS];
        paused = new boolean[MAX_PLAYERS];
    }

    /**
     * Builds one character per occupied slot. The index of the array is the
     * slot the server assigned, so a player keeps its identity for the whole
     * match even if somebody else leaves.
     */
    private Character[] createPlayers() {
        Vector2 spawn = map.getPlayerSpawn();
        Character[] created = new Character[MAX_PLAYERS];
        if (server == null)
            return created;

        for (int slot = 0; slot < MAX_PLAYERS; slot++) {
            if (!server.isSlotOccupied(slot))
                continue;

            float offsetX = (slot - (MAX_PLAYERS - 1) / 2f) * SPAWN_SPACING;
            Vector2 position = new Vector2(spawn.x + offsetX, spawn.y);
            created[slot] = new Character(position, new Vector2(CHARACTER_SIZE), CHARACTER_WEIGHT, world);
        }

        return created;
    }

    /** How many characters this screen is simulating. */
    public int getPlayerCount() {
        int count = 0;
        for (Character player : players) {
            if (player != null)
                count++;
        }
        return count;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        update(delta);
        draw();
    }

    private void draw() {
        mapRenderer.setView(camera);
        mapRenderer.render();
        batch.begin();
        for (Character player : players)
            player.draw(batch);

        batch.setProjectionMatrix(camera.combined);
        debugRenderer.render(world, camera.combined);
        batch.end();
    }

    /**
     * Fixed tick: the physics and the state broadcast advance in equal steps no
     * matter how many frames the window renders, so the client receives a stable
     * cadence. A long frame is clamped instead of being simulated in full.
     */
    private void update(float delta) {
        accumulator += Math.min(delta, FIXED_STEP * MAX_STEPS_PER_FRAME);

        while (accumulator >= FIXED_STEP) {
            step(FIXED_STEP);
            accumulator -= FIXED_STEP;
        }

        centerCameraOnPlayers();
    }

    private void step(float delta) {
        for (int slot = 0; slot < players.length; slot++) {
            Character player = players[slot];
            if (player == null)
                continue;

            // A slot that is no longer occupied left the match: its body must
            // stop existing, or it would keep colliding with the others.
            if (server != null && !server.isSlotOccupied(slot)) {
                player.dispose();
                players[slot] = null;
                paused[slot] = false;
                previousEvents[slot] = 0;
                continue;
            }

            applyInput(slot, player, delta);
        }

        world.step(delta, 10, 4);

        sendState();
    }

    /**
     * Feeds one character with the input its client sent for this tick. It mirrors
     * what the client used to do with its own mouse: the pointer delta becomes the
     * velocity that moves the hands, both buttons apply force while held, and a
     * release latches the grip exactly like the local mouse release did.
     */
    private void applyInput(int slot, Character player, float delta) {
        InputState input = server == null ? null : server.getClientInput(slot);
        if (input == null) {
            previousEvents[slot] = 0;
            player.update(Vector2.Zero, false, false, false, false);
            return;
        }

        // One-off events arrive repeated for a few ticks, so only the tick where
        // the bit appears is acted on.
        int pressedEvents = input.events() & ~previousEvents[slot];
        previousEvents[slot] = input.events();

        if ((pressedEvents & InputPacket.EVENT_PAUSE) != 0)
            paused[slot] = true;

        if ((pressedEvents & InputPacket.EVENT_RESUME) != 0)
            paused[slot] = false;

        // A paused player keeps its body in the world but stops driving it: the
        // previous pointer and buttons are forgotten so resuming cannot apply a
        // jump from the frames that were skipped.
        if (paused[slot]) {
            hasPointer[slot] = false;
            previousLeftButton[slot] = false;
            previousRightButton[slot] = false;
            player.update(Vector2.Zero, false, false, false, false);
            return;
        }

        // Wiki 3.7.4: the impulse is a small jump from the torso.
        if ((pressedEvents & InputPacket.EVENT_IMPULSE) != 0)
            player.impulse();

        Vector2 pointerVelocity = pointerVelocity(slot, input, delta);

        if (previousLeftButton[slot] && !input.left())
            player.onLeftMouseReleased();

        if (previousRightButton[slot] && !input.right())
            player.onRightMouseReleased();

        previousLeftButton[slot] = input.left();
        previousRightButton[slot] = input.right();

        player.update(pointerVelocity, input.left(), input.right(), input.a(), input.d());
    }

    /** Pointer speed in world meters per second from one input to the next. */
    private Vector2 pointerVelocity(int slot, InputState input, float delta) {
        if (!hasPointer[slot]) {
            hasPointer[slot] = true;
            previousPointerX[slot] = input.mouseX();
            previousPointerY[slot] = input.mouseY();
            return Vector2.Zero;
        }

        float velocityX = (input.mouseX() - previousPointerX[slot]) / delta;
        float velocityY = (input.mouseY() - previousPointerY[slot]) / delta;

        previousPointerX[slot] = input.mouseX();
        previousPointerY[slot] = input.mouseY();

        return new Vector2(velocityX, velocityY);
    }

    /** Broadcasts the bodies of every connected player as one STATE datagram. */
    private void sendState() {
        if (server == null)
            return;

        PlayerState[] snapshot = new PlayerState[getPlayerCount()];
        int index = 0;
        for (int slot = 0; slot < players.length; slot++) {
            if (players[slot] != null)
                snapshot[index++] = toPlayerState(slot, players[slot]);
        }

        server.broadcastState(StatePacket.encode(snapshot, StatePacket.MATCH_RUNNING));
    }

    private PlayerState toPlayerState(int slot, Character player) {
        Body[] bodies = player.getParts();
        PartState[] parts = new PartState[bodies.length];

        for (int i = 0; i < bodies.length; i++) {
            Body body = bodies[i];
            Vector2 position = body.getPosition();
            parts[i] = new PartState(position.x, position.y, (float) Math.toDegrees(body.getAngle()));
        }

        return new PlayerState(slot, parts,
                player.isLeftHandGripping(), player.isRightHandGripping(), false);
    }

    /** Keeps the camera on the midpoint of every player so all of them are visible. */
    private void centerCameraOnPlayers() {
        boolean found = false;
        float minX = 0, maxX = 0, minY = 0, maxY = 0;

        for (Character player : players) {
            if (player == null)
                continue;

            Vector2 position = player.getPosition();
            if (!found) {
                minX = maxX = position.x;
                minY = maxY = position.y;
                found = true;
            } else {
                minX = Math.min(minX, position.x);
                maxX = Math.max(maxX, position.x);
                minY = Math.min(minY, position.y);
                maxY = Math.max(maxY, position.y);
            }
        }

        if (!found)
            camera.position.set(map.getPlayerSpawn().x, map.getPlayerSpawn().y, 0);
        else
            camera.position.set((minX + maxX) / 2f, (minY + maxY) / 2f, 0);

        camera.zoom = CAMERA_ZOOM;
        camera.update();
    }
}
