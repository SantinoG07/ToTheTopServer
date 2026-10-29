package com.TfPSR.ToTheTop.menus;

import com.TfPSR.ToTheTop.asset.AssetService;
import com.TfPSR.ToTheTop.asset.SoundAsset;
import com.TfPSR.ToTheTop.core.Constants;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

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
    private final Image backgroundOverlay;

    public SettingsMenu(AssetService assetService){
        this.stage = new Stage(new ExtendViewport(Constants.UI_VIEWPORT_WIDTH, Constants.UI_VIEWPORT_HEIGHT));
        this.assetService = assetService;
        this.skin = assetService.getSkin();
        this.table = new Table();

        this.backgroundOverlay = new Image(skin.getDrawable("white"));
        backgroundOverlay.setColor(0f, 0f, 0f, 0.65f);
        backgroundOverlay.setFillParent(true);

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

        soundSlider.addListener(new InputListener(){
           @Override
           public boolean touchDown(InputEvent e, float x, float y, int pointer, int button){
                return true;
           }

           @Override
            public void touchUp(InputEvent e, float x, float y, int pointer, int button){
               assetService.getAudioManager().playSound(assetService.get(SoundAsset.SLIDER));

           }
        });

        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                assetService.getAudioManager().setMusicVolume(musicSlider.getValue());
            }
        });

        this.continueButton =  new TextButton("Continue", skin);

        continueButton.getClickListener().setButton(Input.Buttons.LEFT);

        assetService.getUiSoundEvent().addHoverSound(continueButton, assetService);

        continueButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                assetService.getAudioManager().playSound(assetService.get(SoundAsset.CLICK_BUTTON));
                close();
            }
        });

        table.setFillParent(true);
        stage.addActor(backgroundOverlay);
        stage.addActor(table);

        table.add(musicLabel).padRight(25);
        table.add(musicSlider).width(300).height(50).pad(20);
        table.row();
        table.add(soundLabel);
        table.add(soundSlider).width(300).height(50).pad(20);
        table.row();
        table.add(continueButton).width(280).height(70).pad(25);
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

    public void resize(int width, int height){
        stage.getViewport().update(width, height, true);
    }
}
