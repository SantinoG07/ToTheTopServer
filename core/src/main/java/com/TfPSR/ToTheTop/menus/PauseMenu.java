package com.TfPSR.ToTheTop.menus;

import com.TfPSR.ToTheTop.Main;
import com.TfPSR.ToTheTop.asset.AssetService;
import com.TfPSR.ToTheTop.asset.SoundAsset;
import com.TfPSR.ToTheTop.core.Constants;
import com.TfPSR.ToTheTop.core.GameScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ExtendViewport;


public class PauseMenu {
    private final Stage stage;
    private final Skin skin;
    private final AssetService assetService;
    private final TextButton continueButton;
    private final TextButton settingsButton;
    private final TextButton mainMenuButton;
    private final Table table;
    private final SettingsMenu settingsMenu;
    private final Image backgroundOverlay;

    public PauseMenu(AssetService assetService, Main game, GameScreen gameScreen, SettingsMenu settingsMenu){
        this.assetService = assetService;
        this.stage = new Stage(new ExtendViewport(Constants.UI_VIEWPORT_WIDTH, Constants.UI_VIEWPORT_HEIGHT));
        this.skin = assetService.getSkin();
        this.table = new Table();
        this.settingsMenu = settingsMenu;

        this.backgroundOverlay = new Image(skin.getDrawable("white"));
        backgroundOverlay.setColor(0f, 0f, 0f, 0.65f);
        backgroundOverlay.setFillParent(true);

        this.continueButton = new TextButton("Continue", skin);
        this.mainMenuButton = new TextButton("Main Menu", skin);
        this.settingsButton = new TextButton("Settings", skin);

        continueButton.getClickListener().setButton(Input.Buttons.LEFT);
        mainMenuButton.getClickListener().setButton(Input.Buttons.LEFT);
        settingsButton.getClickListener().setButton(Input.Buttons.LEFT);

        assetService.getUiSoundEvent().addHoverSound(continueButton, assetService);
        assetService.getUiSoundEvent().addHoverSound(mainMenuButton, assetService);
        assetService.getUiSoundEvent().addHoverSound(settingsButton, assetService);

        continueButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                assetService.getAudioManager().playSound(assetService.get(SoundAsset.CLICK_BUTTON));
                gameScreen.resumeGame();
            }
        });

        mainMenuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                assetService.getAudioManager().playSound(assetService.get(SoundAsset.CLICK_BUTTON));
                stage.getRoot().setVisible(false);
                game.setScreen(MainMenu.class);
            }
        });

        settingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                assetService.getAudioManager().playSound(assetService.get(SoundAsset.CLICK_BUTTON));
                settingsMenu.open(gameScreen.getInputMultiplexer());
            }
        });


        table.setFillParent(true);
        stage.addActor(backgroundOverlay);
        stage.addActor(table);

        table.add(continueButton).width(280).height(70).padTop(600).padRight(25);
        table.add(mainMenuButton).width(280).height(70).padTop(600).padRight(25);
        table.add(settingsButton).width(280).height(70).padTop(600).padRight(25);
        stage.getRoot().setVisible(false);
    }

    public void open(){
        assetService.getAudioManager().playSound(assetService.get(SoundAsset.CLICK_BUTTON));
        Gdx.input.setCursorCatched(false);
        stage.getRoot().setVisible(true);
    }

    public void close(){
        assetService.getAudioManager().playSound(assetService.get(SoundAsset.CLICK_BUTTON));
        Gdx.input.setCursorCatched(true);
        stage.getRoot().setVisible(false);
    }

    public Stage getStage(){
        return stage;
    }

    public void resize(int width, int height){
        stage.getViewport().update(width, height, true);
    }
}
