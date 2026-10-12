package com.armilp.ezvcsurvival.voicechat;

import net.fabricmc.loader.api.FabricLoader;

public class VoiceModCheck {

    private VoiceModCheck(){
    }

    public static boolean hasSimpleVoiceChat(){
        return FabricLoader.getInstance().isModLoaded("voicechat");
    }

    public static boolean hasPlasmoVoice(){
        return FabricLoader.getInstance().isModLoaded("plasmovoice");
    }

    public static void verify(){
        if (!hasSimpleVoiceChat() && !hasPlasmoVoice()){
            throw new IllegalStateException("Voiceless Survival requires either Simple Voice Chat or Plasmo Voice to be installed.");
        }
    }
}
