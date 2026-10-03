package com.armilp.ezvcsurvival;

import com.armilp.ezvcsurvival.commands.EZVCCommands;
import com.armilp.ezvcsurvival.compat.guns.GunFireListener;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.MobGoalInjector;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.sculk.ModGameEvent;
import com.armilp.ezvcsurvival.voicechat.VoiceModCheck;
import com.armilp.ezvcsurvival.voicechat.plasmo.PlasmoVoiceCompat;
import com.mojang.logging.LogUtils;
import com.tacz.guns.api.event.common.GunFireEvent;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import static com.armilp.ezvcsurvival.config.SoundConfig.*;

public class EZVCSurvival implements ModInitializer {
    public static final String MOD_ID = "ezvcsurvival";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {

        EzvcPlatform platform = EzvcPlatform.getInstance();
        VoiceModCheck.verify();

        if (FabricLoader.getInstance().isModLoaded("plasmovoice")) {
            PlasmoVoiceCompat.register();
        }

        ConfigRegistry.INSTANCE.register(MOD_ID, net.neoforged.fml.config.ModConfig.Type.COMMON, VoiceConfig.CONFIG, "ezvcsurvival/voices.toml");
        ConfigRegistry.INSTANCE.register(MOD_ID, net.neoforged.fml.config.ModConfig.Type.COMMON, SPEC, "ezvcsurvival/sounds.toml");

        onModConfigLoading();
        onModConfigReloading();

        EntityVoiceConfig.init();
        GeneralSoundsConfig.init();
        loadConfigs();
        ModGameEvent.register();

        GunFireEvent.CALLBACK.register(GunFireListener::onGunFire);
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
