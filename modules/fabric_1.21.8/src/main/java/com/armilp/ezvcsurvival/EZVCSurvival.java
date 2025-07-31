package com.armilp.ezvcsurvival;


import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.ReactToSoundGoal;
import com.armilp.ezvcsurvival.goals.injector.FollowVoiceGoalInjector;
import com.armilp.ezvcsurvival.goals.injector.ReactToSoundGoalInjector;
import com.armilp.ezvcsurvival.goals.injector.RunAwayVoiceGoalInjector;
import net.fabricmc.api.ModInitializer;

public class EZVCSurvival implements ModInitializer {
    public static final String MOD_ID = "ezvcsurvival";

    @Override
    public void onInitialize() {

        // Loading Mod Message using MOD_ID
        System.out.println("EZVCSurvival Mod Initialized with ID: " + MOD_ID);


        SoundConfig.load();
        VoiceConfig.load();

        FollowVoiceGoalInjector.init();
        RunAwayVoiceGoalInjector.init();
        ReactToSoundGoalInjector.init();
        ReactToSoundGoal.init();

    }




}
