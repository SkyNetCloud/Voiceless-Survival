package com.armilp.ezvcsurvival;


import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.ReactToSoundGoal;
import com.armilp.ezvcsurvival.goals.injector.FollowVoiceGoalInjector;
import com.armilp.ezvcsurvival.goals.injector.ReactToSoundGoalInjector;
import com.armilp.ezvcsurvival.goals.injector.RunAwayVoiceGoalInjector;
import com.mojang.logging.LogUtils;


import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.impl.ForgeConfigAPIPortFabric;
import fuzs.forgeconfigapiport.fabric.impl.client.ForgeConfigAPIPortFabricClient;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;

import static net.neoforged.fml.config.ModConfig.Type.COMMON;

public class EZVCSurvival implements ModInitializer {
    public static final String MOD_ID = "ezvcsurvival";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {

        ConfigRegistry.INSTANCE.register(MOD_ID, COMMON, VoiceConfig.CONFIG, "ezvcsurvival/voices.toml");
        ConfigRegistry.INSTANCE.register(MOD_ID, COMMON, SoundConfig.SPEC, "ezvcsurvival/sounds.toml");

        SoundConfig.loadConfigs();
        FollowVoiceGoalInjector.init();
        RunAwayVoiceGoalInjector.init();
        ReactToSoundGoalInjector.init();
        ReactToSoundGoal.init();
    }


}
