package com.armilp.ezvcsurvival.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class VoiceConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("ezvcsurvival/voices.json");

    private static VoiceConfigData config = new VoiceConfigData();

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save(); // Save default
        } else {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                config = GSON.fromJson(reader, VoiceConfigData.class);
            } catch (IOException e) {
                System.err.println("Failed to load voice config: " + e.getMessage());
            }
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
           System.out.println("Saved voice config to " + CONFIG_PATH);
        }
    }

    public static Map<String, VoiceAttributes> getMobVoiceConfigs() {
        return config.mobVoiceConfigs;
    }

    public static Map<String, VoiceAttributes> getAnimalVoiceConfigs() {
        return config.animalVoiceConfigs;
    }

    public static WhisperConfig getWhisperConfig() {
        return config.whisperConfig;
    }

    public static MiscConfig getMiscConfig() {
        return config.miscConfig;
    }

    public static Map<String, ArmorEffect> getArmorEffects() {
        return config.armorEffects;
    }

    // === CONFIG DATA STRUCTURES === //

    public static class VoiceConfigData {
        public Map<String, VoiceAttributes> mobVoiceConfigs = new HashMap<>();
        public Map<String, VoiceAttributes> animalVoiceConfigs = new HashMap<>();
        public WhisperConfig whisperConfig = new WhisperConfig();
        public MiscConfig miscConfig = new MiscConfig();
        public Map<String, ArmorEffect> armorEffects = new HashMap<>();
    }

    public static class VoiceAttributes {
        public double speed = 1.0;
        public double range = 16.0;
        public double threshold = -40.0;
    }

    public static class WhisperConfig {
        public double rangeMultiplier = 0.5;
        public double speedMultiplier = 0.8;
    }

    public static class MiscConfig {
        public double thunderRangeMultiplier = 0.5;
        public double sneakingRangeMultiplier = 0.5;
    }

    @SuppressWarnings("unused")
    public static class ArmorEffect {
        public double speedMultiplier = 1.0;
        public double rangeMultiplier = 1.0;
    }
}