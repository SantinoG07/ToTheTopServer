package com.TfPSR.ToTheTop.menus;

import com.TfPSR.ToTheTop.asset.AssetService;
import com.TfPSR.ToTheTop.asset.AudioManager;
import com.TfPSR.ToTheTop.asset.SoundAsset;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Button;

public class UiSoundEvent {

    public void addHoverSound(Button button, AssetService assetService){
        button.addListener(new InputListener(){
           @Override
           public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor){
               if(pointer == -1 && (fromActor == null || !fromActor.isDescendantOf(event.getListenerActor())))
                   assetService.getAudioManager().playSound(assetService.get(SoundAsset.HOVER));
           }
        });
    }
}
