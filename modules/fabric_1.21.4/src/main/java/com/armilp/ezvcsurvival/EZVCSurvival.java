package com.armilp.ezvcsurvival;


import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.mojang.logging.LogUtils;


import fuzs.forgeconfigapiport.fabric.api.forge.v4.ForgeConfigRegistry;
import net.fabricmc.api.ModInitializer;

import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

public class EZVCSurvival implements ModInitializer {
    public static final String MOD_ID = "ezvcsurvival";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        ForgeConfigRegistry.INSTANCE.register(MOD_ID, ModConfig.Type.COMMON, VoiceConfig.CONFIG);
    }
}
