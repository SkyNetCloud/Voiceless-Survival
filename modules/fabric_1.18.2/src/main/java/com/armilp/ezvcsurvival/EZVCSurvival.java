package com.armilp.ezvcsurvival;


import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.ReactToSoundGoal;
import com.armilp.ezvcsurvival.goals.injector.FollowVoiceGoalInjector;
import com.armilp.ezvcsurvival.goals.injector.ReactToSoundGoalInjector;
import com.armilp.ezvcsurvival.goals.injector.RunAwayVoiceGoalInjector;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.minecraftforge.api.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

public class EZVCSurvival implements ModInitializer {
    public static final String MOD_ID = "ezvcsurvival";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        ModLoadingContext.registerConfig(MOD_ID, ModConfig.Type.COMMON, VoiceConfig.CONFIG, "ezvcsurvival/voices.toml");
        ModLoadingContext.registerConfig(MOD_ID, ModConfig.Type.COMMON, SoundConfig.SPEC, "ezvcsurvival/sounds.toml");
        SoundConfig.loadConfigs();
        FollowVoiceGoalInjector.init();
        RunAwayVoiceGoalInjector.init();
        ReactToSoundGoalInjector.init();
        ReactToSoundGoal.init();
    }


}
