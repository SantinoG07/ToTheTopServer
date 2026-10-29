package com.TfPSR.ToTheTop.menus;

import com.TfPSR.ToTheTop.Main;
import com.TfPSR.ToTheTop.asset.AssetService;
import com.TfPSR.ToTheTop.core.GameScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class PauseMenu {
    private final Stage stage;
    private final Skin skin;
    private final AssetService assetService;
    private final TextButton continueButton;
    private final TextButton settingsButton;
    private final TextButton mainMenuButton;
    private final Table table;
    private final SettingsMenu settingsMenu;

    public PauseMenu(AssetService assetService, Main game, GameScreen gameScreen, SettingsMenu settingsMenu){
        this.assetService = assetService;
        this.stage = new Stage(new ScreenViewport());
        this.skin = assetService.getSkin();
        this.table = new Table();
        this.settingsMenu = settingsMenu;

        this.continueButton = new TextButton("Continue", skin);
        this.mainMenuButton = new TextButton("Main Menu", skin);
        this.settingsButton = new TextButton("Settings", skin);

        continueButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                gameScreen.resumeGame();
            }
        });

        mainMenuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                stage.getRoot().setVisible(false);
                game.setScreen(MainMenu.class);
            }
        });

        settingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settingsMenu.open(gameScreen.getInputMultiplexer());
            }
        });


        table.setFillParent(true);
        stage.addActor(table);

        table.add(continueButton).width(250).height(60).padTop(600);
        table.add(mainMenuButton).width(250).height(60).padTop(600);
        table.add(settingsButton).width(250).height(60).padTop(600);
        stage.getRoot().setVisible(false);
    }

    public void open(){
        Gdx.input.setCursorCatched(false);
        Gdx.input.setInputProcessor(stage);
        stage.getRoot().setVisible(true);
    }

    public void close(){
        Gdx.input.setCursorCatched(true);
        stage.getRoot().setVisible(false);
    }

    public Stage getStage(){
        return stage;
    }




}
