package com.TfPSR.ToTheTop.core;

import com.TfPSR.ToTheTop.*;
import com.TfPSR.ToTheTop.asset.AssetService;
import com.TfPSR.ToTheTop.asset.SoundAsset;
import com.TfPSR.ToTheTop.entity.Character;
import com.TfPSR.ToTheTop.input.GameInputProcessor;
import com.TfPSR.ToTheTop.map.GameMap;
import com.TfPSR.ToTheTop.map.Rocks;
import com.TfPSR.ToTheTop.menus.SettingsMenu;
import com.TfPSR.ToTheTop.network.InputState;
import com.TfPSR.ToTheTop.network.PartState;
import com.TfPSR.ToTheTop.network.PlayerState;
import com.TfPSR.ToTheTop.network.StatePacket;
import com.TfPSR.ToTheTop.network.ThreadServer;
import com.TfPSR.ToTheTop.physics.GameContactListener;
import com.TfPSR.ToTheTop.menus.PauseMenu;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
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
    private final InputMultiplexer inputMultiplexer;

    private final Rocks rocks;

    private final GameInputProcessor inputProcessor;
    private final ThreadServer server;

    /** Previous pointer and buttons of every slot, to derive velocity and edges. */
    private final float[] previousPointerX;
    private final float[] previousPointerY;
    private final boolean[] hasPointer;
    private final boolean[] previousLeftButton;
    private final boolean[] previousRightButton;

    private float accumulator = 0f;
    private boolean paused = false;
    private final SettingsMenu settingsMenu;
    private final PauseMenu pauseMenu;

    public GameScreen(Main game, AssetService assetService, SettingsMenu settingsMenu) {
        this.game = game;
        this.camera = game.getCamera();
        this.batch = game.getBatch();
        this.assetService = assetService;
        this.dash = assetService.get(SoundAsset.DASH);
        this.inputMultiplexer = new InputMultiplexer();
        this.server = game.getServer();
        this.settingsMenu = settingsMenu;
        this.pauseMenu = new PauseMenu(assetService, game, this, settingsMenu);

        this.debugRenderer = new Box2DDebugRenderer();
        map = new GameMap("maps/map.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map.getTiledMap(), 1f / Constants.PIXELS_PER_METER);

        world = new World(GRAVEDAD, true);
        world.setContactListener(new GameContactListener());

        rocks = new Rocks(map.getSurfaceObjects(), world);

        players = createPlayers(game.getPlayerCount());

        previousPointerX = new float[players.length];
        previousPointerY = new float[players.length];
        hasPointer = new boolean[players.length];
        previousLeftButton = new boolean[players.length];
        previousRightButton = new boolean[players.length];

        inputProcessor = new GameInputProcessor(this);
        inputMultiplexer.addProcessor(pauseMenu.getStage());
        inputMultiplexer.addProcessor(inputProcessor);
        Gdx.input.setInputProcessor(inputMultiplexer);

        Gdx.input.setCursorCatched(true);
    }

    /**
     * Builds one character per connected player, spread evenly around the map
     * spawn so both are visible on screen. The physics of every character lives
     * in the server because it will be the authority for the networked match.
     */
    private Character[] createPlayers(int playerCount) {
        int count = Math.max(1, playerCount);
        Vector2 spawn = map.getPlayerSpawn();
        Character[] created = new Character[count];

        for (int i = 0; i < count; i++) {
            float offsetX = (i - (count - 1) / 2f) * SPAWN_SPACING;
            Vector2 position = new Vector2(spawn.x + offsetX, spawn.y);
            created[i] = new Character(position, new Vector2(CHARACTER_SIZE), CHARACTER_WEIGHT, world);
        }

        return created;
    }

    /** How many characters this screen is simulating. */
    public int getPlayerCount() {
        return players.length;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if(!paused)
            update(delta);

        draw();
    }

    public InputMultiplexer getInputMultiplexer() {
        return inputMultiplexer;
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

        if (paused) {
            pauseMenu.getStage().act(Gdx.graphics.getDeltaTime());
            pauseMenu.getStage().draw();

            settingsMenu.drawSettingsMenu(Gdx.graphics.getDeltaTime());
        }
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
        for (int slot = 0; slot < players.length; slot++)
            applyInput(slot, players[slot], delta);

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
            player.update(Vector2.Zero, false, false);
            return;
        }

        Vector2 pointerVelocity = pointerVelocity(slot, input, delta);

        if (previousLeftButton[slot] && !input.left())
            player.onLeftMouseReleased();

        if (previousRightButton[slot] && !input.right())
            player.onRightMouseReleased();

        previousLeftButton[slot] = input.left();
        previousRightButton[slot] = input.right();

        player.update(pointerVelocity, input.left(), input.right());
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

    /** Broadcasts the bodies of every player as one STATE datagram. */
    private void sendState() {
        if (server == null)
            return;

        PlayerState[] snapshot = new PlayerState[players.length];
        for (int i = 0; i < players.length; i++)
            snapshot[i] = toPlayerState(i, players[i]);

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
        Vector2 first = players[0].getPosition();
        float minX = first.x;
        float maxX = first.x;
        float minY = first.y;
        float maxY = first.y;

        for (int i = 1; i < players.length; i++) {
            Vector2 position = players[i].getPosition();
            minX = Math.min(minX, position.x);
            maxX = Math.max(maxX, position.x);
            minY = Math.min(minY, position.y);
            maxY = Math.max(maxY, position.y);
        }

        camera.position.set((minX + maxX) / 2f, (minY + maxY) / 2f, 0);
        camera.zoom = CAMERA_ZOOM;
        camera.update();
    }

    // The server window must not drive a character with its own mouse. Grabbing
    // will be applied from the networked input instead.
    public void leftMouseReleased() {
    }

    public void rightMouseReleased() {
    }

    public boolean isPaused(){
        return paused;
    }

    public void pauseGame(){
        paused = true;
        pauseMenu.open();
    }

    public void resumeGame(){
        paused = false;
        Gdx.input.setInputProcessor(inputMultiplexer);
        pauseMenu.close();
    }
}
