package com.TfPSR.ToTheTop.asset;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;

public class AssetService implements Disposable {
    private final AssetManager assetManager;
    private final Skin skin;
    private final AudioManager audioManager;

    public AssetService(FileHandleResolver fileHandleResolver) {
        this.assetManager = new AssetManager(fileHandleResolver);
        this.skin = new Skin();
        this.audioManager = new AudioManager();

        int size = 20, borderThickness = 2;
        Pixmap pixmapButton = new Pixmap(size, size, Pixmap.Format.RGB888);
        pixmapButton.setColor(Color.DARK_GRAY);
        pixmapButton.fill();
        pixmapButton.setColor(Color.GRAY);
        pixmapButton.fillRectangle(borderThickness, borderThickness, size - (borderThickness *2), size - (borderThickness * 2));
        skin.add("darkGray", new Texture(pixmapButton));
        pixmapButton.dispose();

        NinePatch button = new NinePatch(skin.getRegion("darkGray"), borderThickness, borderThickness, borderThickness, borderThickness);
        NinePatchDrawable buttonDrawable = new NinePatchDrawable(button);
        buttonDrawable.setPadding(6, 12, 6, 12);

        BitmapFont buttonFont = new BitmapFont();
        skin.add("buttonFont", buttonFont);

        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.up = buttonDrawable.tint(Color.WHITE);
        textButtonStyle.over = buttonDrawable.tint( Color.LIGHT_GRAY);
        textButtonStyle.down = buttonDrawable.tint(new Color(0.6f, 0.6f, 0.6f,1));
        textButtonStyle.font = skin.getFont("buttonFont");
        skin.add("default", textButtonStyle);

        int sliderWidth = 20, sliderHeight = 6;
        Pixmap pixmapSliderBackground = new Pixmap(sliderWidth, sliderHeight, Pixmap.Format.RGB888);
        pixmapSliderBackground.setColor(Color.DARK_GRAY);
        pixmapSliderBackground.fill();

        skin.add("sliderBackground", new Texture(pixmapSliderBackground));
        pixmapSliderBackground.dispose();

        int knobSize = 14;
        Pixmap pixmapSliderKnob = new Pixmap(knobSize, knobSize, Pixmap.Format.RGB888);
        pixmapSliderKnob.setColor(Color.DARK_GRAY);
        pixmapSliderKnob.fillCircle(knobSize / 2, knobSize / 2, knobSize / 2 );

        skin.add("sliderKnob", new Texture(pixmapSliderKnob));
        pixmapSliderKnob.dispose();

        TextureRegion backgroundRegion = new TextureRegion(skin.get("sliderBackground", Texture.class));
        TextureRegionDrawable backgroundDrawable = new TextureRegionDrawable(backgroundRegion);

        TextureRegion knobRegion = new TextureRegion(skin.get("sliderKnob", Texture.class));
        TextureRegionDrawable knobDrawable = new TextureRegionDrawable(knobRegion);

        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = backgroundDrawable;
        sliderStyle.knob = knobDrawable;

        skin.add("defaultSliderStyle", sliderStyle);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.get("buttonFont", BitmapFont.class);
        labelStyle.fontColor = Color.WHITE;

        skin.add("defaultLabelStyle", labelStyle);
    }




    public Skin getSkin() {
        return skin;
    }

    public <T> T load(Asset<T> asset){
        this.assetManager.load(asset.getDescriptor());
        this.assetManager.finishLoading() ;
        return this.assetManager.get(asset.getDescriptor());
    }

    public <T> void queue(Asset<T> asset){
        this.assetManager.load(asset.getDescriptor());
    }

    public <T> T get(Asset<T> asset){
        return this.assetManager.get(asset.getDescriptor());
    }

    public boolean update(){
        return this.assetManager.update();
    }

    @Override
    public void dispose() {
        this.assetManager.dispose();
    }


    public AudioManager getAudioManager() {
        return audioManager;
    }
}
