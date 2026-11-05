package com.TfPSR.ToTheTop.menus;

import com.TfPSR.ToTheTop.Main;
import com.TfPSR.ToTheTop.asset.AssetService;
import com.TfPSR.ToTheTop.asset.MusicAsset;
import com.TfPSR.ToTheTop.asset.SoundAsset;
import com.TfPSR.ToTheTop.asset.TextureAsset;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class LoadMenu extends ScreenAdapter {
    private final AssetService assetService;
    private final Main game;
    private ShapeRenderer shapeRenderer;

    public LoadMenu(Main game, AssetService assetService) {
        this.game = game;
        this.assetService = assetService;
    }

   @Override
   public void show(){
       this.shapeRenderer = new ShapeRenderer();

       for (SoundAsset sound : SoundAsset.values()) {
           assetService.queue(sound);
       }

       for(MusicAsset music : MusicAsset.values()){
           assetService.queue(music);
       }

       for(TextureAsset texture : TextureAsset.values()){
           assetService.queue(texture);
       }
   }

    @Override
    public void render(float delta) {
        if(this.assetService.update()){
            game.addScreen(new MainMenu(game, assetService));
            game.setScreen(MainMenu.class);
        }else{

        }
    }
}

