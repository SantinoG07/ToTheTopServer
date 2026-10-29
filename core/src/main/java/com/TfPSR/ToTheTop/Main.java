package com.TfPSR.ToTheTop;

import com.TfPSR.ToTheTop.asset.AssetService;
import com.TfPSR.ToTheTop.menus.LoadMenu;
import com.TfPSR.ToTheTop.network.ThreadServer;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.HashMap;
import java.util.Map;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game { //We use Game, because it has better methods of pause, play, and resume
    private static final float MAX_WIDTH = 16f;
    private static final float MAX_HEIGHT = 9f;

    private final Map<Class<? extends Screen>, Screen> screenCache = new HashMap<>();
    private Batch batch;
    private OrthographicCamera camera;
    private Viewport viewport;
    private AssetService assetService;


    private ThreadServer ts;

    @Override
    public void create() {
        this.batch = new SpriteBatch();
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(MAX_WIDTH, MAX_HEIGHT, camera);
        this.assetService = new AssetService(new InternalFileHandleResolver());

        addScreen(new LoadMenu(this, assetService));
        setScreen(LoadMenu.class);
    }

    /** Starts the server from the menu button. Returns false if the port is already taken. */
    public boolean startServer() {
        if (this.ts != null) {
            return true;
        }
        try {
            this.ts = new ThreadServer();
        } catch (IllegalStateException e) {
            this.ts = null;
            Gdx.app.error("Main", "No se pudo abrir el puerto del servidor", e);
            return false;
        }
        this.ts.start();
        return true;
    }

    /** True once the server accepted enough ready players and the match began. */
    public boolean isMatchStarted() {
        return this.ts != null && this.ts.hasStarted();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        viewport.update(width, height, true);

    }

    public void addScreen(Screen screen) {
        screenCache.put(screen.getClass(), screen);
    }

    public void setScreen(Class<? extends Screen> screenClass) {
        Screen screen = screenCache.get(screenClass);
        if (screen == null) {
            throw new GdxRuntimeException("No screen with class " + screenClass + " found in screenCache");
        }
        super.setScreen(screen);
    }

    @Override
    public void dispose() {
        if (ts != null)
            ts.shutdown();
        screenCache.values().forEach(Screen::dispose);
        screenCache.clear();
        this.batch.dispose();
    }

    public Viewport getViewport() {
        return this.viewport;
    }

    public OrthographicCamera getCamera() {
        return this.camera;
    }

    public Batch getBatch() {
        return this.batch;
    }
}
