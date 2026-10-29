package com.TfPSR.ToTheTop.menus;

import com.TfPSR.ToTheTop.Main;
import com.TfPSR.ToTheTop.asset.AssetService;
import com.TfPSR.ToTheTop.asset.MusicAsset;
import com.TfPSR.ToTheTop.asset.TextureAsset;
import com.TfPSR.ToTheTop.core.GameScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Server menu: a single power button. As soon as the match starts, the server
 * switches to the screen the players see.
 */
public class MainMenu extends ScreenAdapter {
    private final Main game;
    private final AssetService assetService;
    private final Texture mainMenuImage;
    private final Viewport viewport;
    private final Music mainMenuMusic;
    private final OrthographicCamera camera;
    private final Batch batch;
    private final Stage stage;
    private final Skin skin;
    private final Table tablePlay;
    private final SettingsMenu settingsMenu;
    private final TextButton powerButton;

    public MainMenu(Main game, AssetService assetService, SettingsMenu settingsMenu) {
        this.game = game;
        this.viewport = game.getViewport();
        this.assetService = assetService;
        this.mainMenuMusic = assetService.get(MusicAsset.MAIN_MENU_MUSIC);
        this.mainMenuMusic.setLooping(true);
        this.mainMenuImage = assetService.get(TextureAsset.BACKGROUND_IMAGE);
        this.batch = game.getBatch();
        this.camera = game.getCamera();
        this.stage = new Stage(new ScreenViewport());
        this.skin = assetService.getSkin();
        this.tablePlay = new Table();
        this.settingsMenu = settingsMenu;

        this.powerButton = new TextButton("Encender", this.skin);
        this.powerButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (MainMenu.this.game.startServer()) {
                    MainMenu.this.powerButton.setDisabled(true);
                    MainMenu.this.powerButton.setTouchable(Touchable.disabled);
                }
            }
        });

        this.tablePlay.setFillParent(true);
        this.stage.addActor(this.tablePlay);
        this.tablePlay.add(this.powerButton).width(250).height(60);
    }

    @Override
    public void show() {
        Gdx.input.setCursorCatched(false);
        assetService.getAudioManager().playMusic(this.mainMenuMusic);
        Gdx.input.setInputProcessor(this.stage);
        this.camera.position.set(this.viewport.getWorldWidth() / 2f, this.viewport.getWorldHeight() / 2f, 0);
        this.camera.zoom = 1f;
        this.camera.update();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        this.camera.update();
        this.batch.setProjectionMatrix(this.camera.combined);

        this.batch.begin();
        this.batch.draw(this.mainMenuImage, 0, 0, this.viewport.getWorldWidth(), this.viewport.getWorldHeight());
        this.batch.end();

        this.stage.act(delta);
        this.stage.draw();

        if (this.game.isMatchStarted()) {
            this.game.addScreen(new GameScreen(this.game, this.assetService, this.settingsMenu));
            this.game.setScreen(GameScreen.class);
        }
    }

    @Override
    public void resize(int width, int height) {
        this.stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        this.mainMenuMusic.stop();
    }
}
