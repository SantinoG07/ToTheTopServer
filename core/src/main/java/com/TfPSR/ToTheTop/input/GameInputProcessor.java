package com.TfPSR.ToTheTop.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.TfPSR.ToTheTop.core.GameScreen;

public class GameInputProcessor extends InputAdapter {
    private boolean leftMousePressed;
    private boolean rightMousePressed;
    private final Vector2 mouseDelta = new Vector2();
    private final GameScreen gameScreen;

    public GameInputProcessor(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {

        if (button == Input.Buttons.LEFT) {
            leftMousePressed = true;
            return true;
        }

        if (button == Input.Buttons.RIGHT) {
            rightMousePressed = true;
            return true;
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {

        if (button == Input.Buttons.LEFT) {
            leftMousePressed = false;
            return true;
        }

        if (button == Input.Buttons.RIGHT) {
            rightMousePressed = false;
            return true;
        }

        return false;
    }

    public Vector2 getMouseDelta() {
        mouseDelta.set(Gdx.input.getDeltaX(), Gdx.input.getDeltaY());
        return mouseDelta;
    }

    public boolean isLeftMousePressed() {
        return leftMousePressed;
    }

    public boolean isRightMousePressed() {
        return rightMousePressed;
    }

    @Override
    public boolean keyDown(int keycode){
        if(keycode == Input.Keys.ESCAPE){
            gameScreen.pauseGame();
            return true;
        }

        if(gameScreen.isPaused())
            return true;
        return true;
    }
}
