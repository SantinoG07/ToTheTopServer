package com.TfPSR.ToTheTop.menus;

import com.TfPSR.ToTheTop.asset.AssetService;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class SettingsMenu {
    private final Stage stage;
    private final AssetService assetService;
    private final Skin skin;
    private final Table table;
    private final Slider musicSlider;
    private final Slider soundSlider;
    private final Label musicLabel;
    private final Label soundLabel;
    private final TextButton continueButton;
    private  InputProcessor previusInputProcessor;

    public SettingsMenu(AssetService assetService){
        this.stage = new Stage(new ScreenViewport());
        this.assetService = assetService;
        this.skin = assetService.getSkin();
        this.table = new Table();
        this.musicLabel = new Label("Music Volume", skin.get("defaultLabelStyle", Label.LabelStyle.class));
        this.soundLabel = new Label("Sound Volume", skin.get("defaultLabelStyle", Label.LabelStyle.class));

        this.musicSlider = new Slider(0f, 1f, 0.05f, false, skin.get("defaultSliderStyle", Slider.SliderStyle.class));
        this.soundSlider = new Slider(0f, 1f, 0.05f, false, skin.get("defaultSliderStyle", Slider.SliderStyle.class));
        musicSlider.setValue(assetService.getAudioManager().getMusicVolume());
        soundSlider.setValue(assetService.getAudioManager().getSoundVolume());

        soundSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                assetService.getAudioManager().setSoundVolume(soundSlider.getValue());
            }
        });

        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                assetService.getAudioManager().setMusicVolume(musicSlider.getValue());
            }
        });

        this.continueButton =  new TextButton("Continue", skin);

        continueButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                close();
            }
        });

        table.setFillParent(true);
        stage.addActor(table);

        table.add(musicLabel);
        table.add(musicSlider).width(250).height(60).pad(20);
        table.row();
        table.add(soundLabel);
        table.add(soundSlider).width(250).height(60).pad(20);
        table.row();
        table.add(continueButton).width(250).height(60).pad(20);
        stage.getRoot().setVisible(false);
    }

    public void open(InputProcessor previosInputProcessor){
        this.previusInputProcessor = previosInputProcessor;

        Gdx.input.setCursorCatched(false);
        Gdx.input.setInputProcessor(stage);
        stage.getRoot().setVisible(true);
    }

    public void close(){
        stage.getRoot().setVisible(false);

        Gdx.input.setInputProcessor(previusInputProcessor);
    }

    public void drawSettingsMenu(float delta) {
        if(!stage.getRoot().isVisible())
            return;

        stage.act(delta);
        stage.draw();
    }

    public Stage getStage() {
        return stage;
    }
}
