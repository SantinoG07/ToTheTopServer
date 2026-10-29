package com.TfPSR.ToTheTop.asset;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.audio.Sound;

public enum SoundAsset implements Asset<Sound>{
    DASH("dash.wav");

    private final AssetDescriptor<Sound> descriptor;

    SoundAsset(String soundFile){
        this.descriptor = new AssetDescriptor<>("audio/" + soundFile, Sound.class);
    }

    @Override
    public AssetDescriptor<Sound> getDescriptor() {
        return descriptor;
    }
}
