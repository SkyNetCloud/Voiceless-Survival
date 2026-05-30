package com.armilp.ezvcsurvival;

import com.armilp.ezvcsurvival.commands.EZVCCommands;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.events.SoundEventHandler;
import com.armilp.ezvcsurvival.goals.MobGoalInjector;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.sculk.ModGameEvent;
import com.mojang.logging.LogUtils;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

public class EZVCSurvival implements ModInitializer {
    public static final String MOD_ID = "ezvcsurvival";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {

        EzvcPlatform platform = EzvcPlatform.getInstance();

        System.out.println("EZVCSurvival Mod Initialized with ID: " + MOD_ID);

        ConfigRegistry.INSTANCE.register(MOD_ID, net.neoforged.fml.config.ModConfig.Type.COMMON, VoiceConfig.CONFIG, "ezvcsurvival/voices.toml");
        ConfigRegistry.INSTANCE.register(MOD_ID, net.neoforged.fml.config.ModConfig.Type.COMMON, SoundConfig.SPEC, "ezvcsurvival/sounds.toml");

        EntityVoiceConfig.init();
        GeneralSoundsConfig.init();
        SoundConfig.loadConfigs();
        ModGameEvent.register();

        ServerEntityEvents.ENTITY_LOAD.register(MobGoalInjector::onEntityJoin);

        EZVCNetwork.registerPackets();

//        MinecraftClient.getInstance().getSoundManager().registerListener(new SoundInstanceListener() {
//            @Override
//            public void onSoundPlayed(SoundInstance sound, WeightedSoundSet soundSet, float range) {
//                SoundEventHandler.onPlaySound(sound, soundSet, range);
//            }
//
//
//        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> EZVCCommands.commandInit(dispatcher));
        platform.onRegistrationCompleted();
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

}
