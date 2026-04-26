package com.armilp.ezvcsurvival;


import com.armilp.ezvcsurvival.commands.EZVCCommands;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.MobGoalInjector;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.sculk.ModGameEvent;
import com.mojang.logging.LogUtils;

import fuzs.forgeconfigapiport.fabric.api.forge.v4.ForgeConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import org.slf4j.Logger;


public class EZVCSurvival implements ModInitializer {
    public static final String MOD_ID = "ezvcsurvival";
    public static final Logger LOGGER = LogUtils.getLogger();
    //public static final String MOD_VERSION = "2.0.0";
    //public static EZVCNetwork voiceNetwork;

    @Override
    public void onInitialize() {

        EZVCNetwork.registerCommon();

        System.out.println("EZVCSurvival Mod Initialized with ID: " + MOD_ID);

        ForgeConfigRegistry.INSTANCE.register(MOD_ID, net.neoforged.fml.config.ModConfig.Type.COMMON, VoiceConfig.CONFIG, "ezvcsurvival/voices.toml");
        ForgeConfigRegistry.INSTANCE.register(MOD_ID, net.neoforged.fml.config.ModConfig.Type.COMMON, SoundConfig.SPEC, "ezvcsurvival/sounds.toml");

        EntityVoiceConfig.init();
        GeneralSoundsConfig.init();
        SoundConfig.loadConfigs();
        ModGameEvent.register();

        ServerEntityEvents.ENTITY_LOAD.register(MobGoalInjector::onEntityJoin);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> EZVCCommands.commandInit(dispatcher));

    }



}
