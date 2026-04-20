package com.armilp.ezvcsurvival.sculk;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;


import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.gameevent.GameEvent;



import java.util.function.ToIntFunction;

import static net.minecraft.core.registries.Registries.GAME_EVENT;
import static net.minecraft.world.level.gameevent.vibrations.VibrationSystem.VIBRATION_FREQUENCY_FOR_EVENT;

public class ModGameEvent {

    public static final Identifier VOICE_TALK_ID =
            EZVCSurvival.id("voice_talk");

    public static final ResourceKey<GameEvent> VOICE_TALK_KEY =
            ResourceKey.create(GAME_EVENT, VOICE_TALK_ID);

    public static GameEvent VOICE_TALK = Registry.register(
            BuiltInRegistries.GAME_EVENT,
            VOICE_TALK_ID,
            new GameEvent(16)
    );


    public static void setupFrequency() {
        try {
            ToIntFunction<ResourceKey<GameEvent>> frequencyMap =
                    VIBRATION_FREQUENCY_FOR_EVENT;

            if (frequencyMap instanceof Object2IntOpenHashMap<ResourceKey<GameEvent>> map) {

                int frequency = VoiceConfig.SCULK_SENSOR_FREQUENCY.get();
                map.put(VOICE_TALK_KEY, frequency);

                int storedFrequency = map.getInt(VOICE_TALK_KEY);
                EZVCSurvival.LOGGER.info(
                        "Registered VOICE_TALK with frequency: " + storedFrequency
                );

            } else {
                EZVCSurvival.LOGGER.error(
                        "VIBRATION_FREQUENCY_FOR_EVENT is not mutable!"
                );
            }

        } catch (Exception e) {
            EZVCSurvival.LOGGER.error(
                    "Failed to register VOICE_TALK frequency", e
            );
        }
    }

    public static void register() {
        setupFrequency();
    }
}