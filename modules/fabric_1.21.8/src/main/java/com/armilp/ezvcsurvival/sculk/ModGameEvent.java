package com.armilp.ezvcsurvival.sculk;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.Vibrations;

import java.util.function.ToIntFunction;

public class ModGameEvent {

    public static final GameEvent VOICE_TALK = Registry.register(
            Registries.GAME_EVENT,
            Identifier.of(EZVCSurvival.MOD_ID, "voice_talk"),
            new GameEvent(16)
    );

    public static void setupFrequency() {
        try {
            ToIntFunction<RegistryKey<GameEvent>> frequencyMap = Vibrations.FREQUENCIES;

            if (frequencyMap instanceof Object2IntOpenHashMap<RegistryKey<GameEvent>> map) {
                RegistryKey<GameEvent> voiceTalkKey = Registries.GAME_EVENT
                        .getKey(VOICE_TALK)
                        .orElse(null);

                if (voiceTalkKey != null) {
                    int frequency = VoiceConfig.SCULK_SENSOR_FREQUENCY.get();
                    map.put(voiceTalkKey, frequency);

                    int storedFrequency = map.getInt(voiceTalkKey);
                    EZVCSurvival.LOGGER.info("Registered VOICE_TALK with frequency: " + storedFrequency);
                } else {
                    EZVCSurvival.LOGGER.error("Failed to get RegistryKey for VOICE_TALK");
                }
            } else {
                EZVCSurvival.LOGGER.error("VIBRATION_FREQUENCY_FOR_EVENT is not an Object2IntOpenHashMap!");
            }
        } catch (Exception e) {
            EZVCSurvival.LOGGER.error("Failed to register VOICE_TALK frequency", e);
        }
    }

    public static void register() {
        setupFrequency();
    }
}