package com.TfPSR.ToTheTop.menus;

import com.TfPSR.ToTheTop.asset.SoundAsset;
import com.TfPSR.ToTheTop.core.Constants;
import com.TfPSR.ToTheTop.core.GameScreen;
import com.TfPSR.ToTheTop.Main;
import com.TfPSR.ToTheTop.asset.AssetService;
import com.TfPSR.ToTheTop.asset.MusicAsset;
import com.TfPSR.ToTheTop.asset.TextureAsset;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

public class MainMenu extends ScreenAdapter {
    private final Main game;
    private final AssetService assetService;
    private final Texture mainMenuImage;
    private final Viewport viewport;
    private final Music mainMenuMusic;
    private final OrthographicCamera camera;
    private final Batch batch;
    private final Stage stage;
    private final TextButton singlePlayerButton;
    private final TextButton multipPlayerButton;
    private final TextButton settingsButton;
    private final TextButton howToPlayButton;
    private final TextButton exitButton;
    private final Skin skin;
    private final Table tablePlay;
    private final Table settingsTable;
    private final Table exitTable;
    private final SettingsMenu settingsMenu;
    private final HowToPlayMenu howToPlayMenu;

    public MainMenu(Main game, AssetService assetService, SettingsMenu settingsMenu){
        this.game = game;
        this.viewport = game.getViewport();
        this.assetService = assetService;
        this.mainMenuMusic = assetService.get(MusicAsset.MAIN_MENU_MUSIC);
        this.mainMenuMusic.setLooping(true);
        this.mainMenuImage = assetService.get(TextureAsset.BACKGROUND_IMAGE);
        this.batch = game.getBatch();
        this.camera = game.getCamera();
        this.stage = new Stage(new ExtendViewport(Constants.UI_VIEWPORT_WIDTH, Constants.UI_VIEWPORT_HEIGHT));
        this.skin = assetService.getSkin();
        this.tablePlay = new Table();
        this.settingsTable = new Table();
        this.exitTable = new Table();
        this.settingsMenu = settingsMenu;
        this.howToPlayMenu = new HowToPlayMenu(assetService);

        this.singlePlayerButton = new TextButton("Singleplayer",skin);
        this.multipPlayerButton = new TextButton("Multiplayer", skin);
        this.settingsButton = new TextButton("Settings", skin);
        this.howToPlayButton = new TextButton("How to Play", skin);
        this.exitButton = new TextButton("Exit", skin);

        singlePlayerButton.getClickListener().setButton(Input.Buttons.LEFT);
        multipPlayerButton.getClickListener().setButton(Input.Buttons.LEFT);
        settingsButton.getClickListener().setButton(Input.Buttons.LEFT);
        howToPlayButton.getClickListener().setButton(Input.Buttons.LEFT);
        exitButton.getClickListener().setButton(Input.Buttons.LEFT);

        assetService.getUiSoundEvent().addHoverSound(singlePlayerButton, assetService);
        assetService.getUiSoundEvent().addHoverSound(multipPlayerButton, assetService);
        assetService.getUiSoundEvent().addHoverSound(settingsButton, assetService);
        assetService.getUiSoundEvent().addHoverSound(howToPlayButton, assetService);
        assetService.getUiSoundEvent().addHoverSound(exitButton, assetService);

        singlePlayerButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                assetService.getAudioManager().playSound(assetService.get(SoundAsset.CLICK_BUTTON));
                game.addScreen(new GameScreen(game, assetService, settingsMenu));
                game.setScreen(GameScreen.class);
            }
        });


        multipPlayerButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                assetService.getAudioManager().playSound(assetService.get(SoundAsset.CLICK_BUTTON));
//              game.addScreen(new GameScreen(game, assetService));
//              game.setScreen(GameScreen.class);
              System.out.println("Modalidad no implementada");
            }
        });

        settingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                assetService.getAudioManager().playSound(assetService.get(SoundAsset.CLICK_BUTTON));
                settingsMenu.open(stage);
            }
        });

        howToPlayButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                assetService.getAudioManager().playSound(assetService.get(SoundAsset.CLICK_BUTTON));
                howToPlayMenu.open(stage);
            }
        });

        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                assetService.getAudioManager().playSound(assetService.get(SoundAsset.CLICK_BUTTON));
                Gdx.app.exit();
            }
        });

        tablePlay.setFillParent(true);
        stage.addActor(tablePlay);

        tablePlay.add(singlePlayerButton).width(280).height(70).padTop(850);

        tablePlay.add(multipPlayerButton).width(280).height(70).padLeft(100).padTop(850);

        tablePlay.row();
        tablePlay.add(howToPlayButton).width(240).height(60).padTop(25).colspan(2);

        settingsTable.setFillParent(true);
        stage.addActor(settingsTable);
        settingsTable.top().left();
        settingsTable.add(settingsButton).width(250).height(60).padLeft(20).padTop(10);

        exitTable.setFillParent(true);
        stage.addActor(exitTable);
        exitTable.top().right();
        exitTable.add(exitButton).width(250).height(60).padRight(20).padTop(10);

    }

    @Override
    public void show() {
        Gdx.input.setCursorCatched(false);
        assetService.getAudioManager().playMusic(mainMenuMusic);
        Gdx.input.setInputProcessor(stage);
        camera.position.set(viewport.getWorldWidth() / 2f, viewport.getWorldHeight() / 2f, 0);
        camera.zoom = 1f;
        camera.update();
    }

    @Override
    public void render(float delta){
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(mainMenuImage, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.end();

        stage.act(delta);
        stage.draw();

        settingsMenu.drawSettingsMenu(delta);
        howToPlayMenu.drawHowToPlayMenu(delta);
    }

    @Override
    public void resize(int width, int height){
        stage.getViewport().update(width, height, true);
        settingsMenu.resize(width, height);
    }

    @Override
    public void hide(){
        mainMenuMusic.stop();
    }
}
