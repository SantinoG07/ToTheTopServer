package com.TfPSR.ToTheTop.core;

import com.TfPSR.ToTheTop.*;
import com.TfPSR.ToTheTop.asset.AssetService;
import com.TfPSR.ToTheTop.asset.SoundAsset;
import com.TfPSR.ToTheTop.entity.Character;
import com.TfPSR.ToTheTop.input.GameInputProcessor;
import com.TfPSR.ToTheTop.map.GameMap;
import com.TfPSR.ToTheTop.map.Rocks;
import com.TfPSR.ToTheTop.menus.SettingsMenu;
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
        this.settingsMenu = settingsMenu;
        this.pauseMenu = new PauseMenu(assetService, game, this, settingsMenu);

        this.debugRenderer = new Box2DDebugRenderer();
        map = new GameMap("maps/map.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map.getTiledMap(), 1f / Constants.PIXELS_PER_METER);

        world = new World(GRAVEDAD, true);
        world.setContactListener(new GameContactListener());

        rocks = new Rocks(map.getSurfaceObjects(), world);

        players = createPlayers(game.getPlayerCount());

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

    private void update(float delta) {
        centerCameraOnPlayers();

        // No local input is applied yet: the server only receives the players'
        // input packets, so the characters are stepped with no force at all.
        for (Character player : players)
            player.update(Vector2.Zero, false, false);

        world.step(delta, 10, 4);
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
