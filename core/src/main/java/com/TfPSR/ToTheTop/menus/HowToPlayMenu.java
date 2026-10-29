package com.TfPSR.ToTheTop.menus;

import com.TfPSR.ToTheTop.asset.AssetService;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class HowToPlayMenu {
    private final Stage stage;
    private final Skin skin;
    private final Table table;
    private final TextButton backButton;
    private InputProcessor previousInputProcessor;

    public HowToPlayMenu(AssetService assetService){
        this.stage = new Stage(new ScreenViewport());
        this.skin = assetService.getSkin();
        this.table = new Table();

        stage.addActor(createBackgroundImage());

        Label.LabelStyle labelStyle = skin.get("defaultLabelStyle", Label.LabelStyle.class);
        Label.LabelStyle titleStyle = new Label.LabelStyle(labelStyle.font, new Color(1f, 0.78f, 0.38f, 1f));
        Label.LabelStyle headerStyle = new Label.LabelStyle(labelStyle.font, new Color(1f, 0.6f, 0.2f, 1f));
        Label.LabelStyle keyStyle = new Label.LabelStyle(labelStyle.font, new Color(1f, 0.85f, 0.45f, 1f));

        Label titleLabel = new Label("HOW TO PLAY", titleStyle);
        titleLabel.setFontScale(2f);

        Label storyHeader = new Label("THE STORY", headerStyle);
        storyHeader.setFontScale(1.3f);

        Label storyLabel = new Label(
                "Long ago, at the foot of a sleeping volcano, a wise elder sensed that the mountain's fury " +
                "would soon wake and swallow the village below. Before his final breath, he entrusted his last " +
                "heirs with an ancient amulet, asking them to climb to the summit and cast it into the volcano's " +
                "core. Climb together, hold on tight, and reach the top before the eruption consumes everything.",
                labelStyle);
        storyLabel.setWrap(true);

        Label controlsHeader = new Label("CONTROLS", headerStyle);
        controlsHeader.setFontScale(1.3f);

        Table controlsTable = new Table();
        controlsTable.top();
        addControlRow(controlsTable, "Move left hand", "HOLD LEFT CLICK + DRAG", labelStyle, keyStyle);
        addControlRow(controlsTable, "Grab with left hand", "RELEASE LEFT CLICK", labelStyle, keyStyle);
        addControlRow(controlsTable, "Move right hand", "HOLD RIGHT CLICK + DRAG", labelStyle, keyStyle);
        addControlRow(controlsTable, "Grab with right hand", "RELEASE RIGHT CLICK", labelStyle, keyStyle);
        addControlRow(controlsTable, "Swing left", "A", labelStyle, keyStyle);
        addControlRow(controlsTable, "Swing right", "D", labelStyle, keyStyle);
        addControlRow(controlsTable, "Jump / torso impulse", "SPACE", labelStyle, keyStyle);
        addControlRow(controlsTable, "Pause", "ESC", labelStyle, keyStyle);

        this.backButton = new TextButton("Back", skin);

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                close();
            }
        });

        NinePatchDrawable panelBackground = new NinePatchDrawable(
                new NinePatch(skin.getRegion("darkGray"), 8, 8, 8, 8));
        panelBackground = panelBackground.tint(new Color(0.08f, 0.07f, 0.1f, 0.92f));
        panelBackground.setMinWidth(0);
        panelBackground.setMinHeight(0);

        Table panel = new Table();
        panel.setBackground(panelBackground);
        panel.pad(35);

        panel.add(titleLabel).pad(5);
        panel.row();
        panel.add(storyHeader).left().padTop(20).padBottom(6);
        panel.row();
        panel.add(storyLabel).width(700).expand().left();
        panel.row();
        panel.add(controlsHeader).left().padTop(20).padBottom(6);
        panel.row();
        panel.add(controlsTable).width(720);
        panel.row();
        panel.add(backButton).width(220).height(55).padTop(25);

        table.setFillParent(true);
        table.add(panel);
        stage.addActor(table);
        stage.getRoot().setVisible(false);
    }

    private Image createBackgroundImage() {
        int gradWidth = 2, gradHeight = 256;
        Pixmap pixmap = new Pixmap(gradWidth, gradHeight, Pixmap.Format.RGB888);
        for (int i = 0; i < gradHeight; i++) {
            float t = i / (float) (gradHeight - 1);
            Color color = new Color(
                    0.16f + 0.10f * t,
                    0.14f + 0.08f * t,
                    0.18f + 0.10f * t,
                    1f);
            pixmap.setColor(color);
            pixmap.drawLine(0, i, gradWidth - 1, i);
        }
        Texture backgroundTexture = new Texture(pixmap);
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();

        Image backgroundImage = new Image(new TextureRegion(backgroundTexture));
        backgroundImage.setFillParent(true);
        return backgroundImage;
    }

    private void addControlRow(Table controlsTable, String description, String key,
                               Label.LabelStyle descriptionStyle, Label.LabelStyle keyStyle) {
        controlsTable.add(new Label(description, descriptionStyle)).left().expandX().padTop(6);
        controlsTable.add(new Label(key, keyStyle)).right().padTop(6).padLeft(30);
        controlsTable.row();
    }

    public void open(InputProcessor previousInputProcessor){
        this.previousInputProcessor = previousInputProcessor;

        Gdx.input.setCursorCatched(false);
        Gdx.input.setInputProcessor(stage);
        stage.getRoot().setVisible(true);
    }

    public void close(){
        stage.getRoot().setVisible(false);

        Gdx.input.setInputProcessor(previousInputProcessor);
    }

    public void drawHowToPlayMenu(float delta) {
        if(!stage.getRoot().isVisible())
            return;

        stage.act(delta);
        stage.draw();
    }

    public Stage getStage() {
        return stage;
    }
}