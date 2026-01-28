package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SoundConfig {

    // Replace ForgeConfigSpec.DoubleValue with a simple wrapper
    public static class DoubleValue {
        private double value;
        private final double defaultValue;
        private final double min;
        private final double max;

        public DoubleValue(double defaultValue, double min, double max) {
            this.defaultValue = defaultValue;
            this.value = defaultValue;
            this.min = min;
            this.max = max;
        }

        public double get() {
            return value;
        }

        public void set(double value) {
            this.value = Math.max(min, Math.min(max, value));
        }
    }

    // Simple Builder class to mimic ForgeConfigSpec.Builder
    public static class Builder {
        public Builder push(String path) {
            return this;
        }

        public Builder pop() {
            return this;
        }

        public DoubleValue defineInRange(String path, double defaultValue, double min, double max) {
            return new DoubleValue(defaultValue, min, max);
        }

        public Object build() {
            return new Object(); // Dummy object
        }
    }

    public static final ForgeConfigSpec.DoubleValue THUNDER_RANGE_MULTIPLIER;

    private static final List<SoundGroupData> priorityGroups = new ArrayList<>();
    private static final List<SoundGroupData> customSoundGroups = new ArrayList<>();

    public static final ForgeConfigSpec SPEC;


    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("weather");
        THUNDER_RANGE_MULTIPLIER = builder.defineInRange("thunder_range_multiplier", 0.8, 0.0, 1.0);
        builder.pop();

        SPEC = builder.build();
    }

    public static void registerConfigListeners() {
        // Initial load (equivalent to ModConfigEvent.Loading)
        onModConfigLoading();

        // Register reload listener (equivalent to ModConfigEvent.Reloading)
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.of(EZVCSurvival.MOD_ID, "sound_config");
                    }

                    @Override
                    public void reload(ResourceManager manager) {
                        onModConfigReloading();
                    }
                }
        );
    }

    private static void onModConfigLoading() {
        EZVCSurvival.LOGGER.info("Loading EZVCSurvival configuration...");
        loadConfigs();
    }

    private static void onModConfigReloading() {
        EZVCSurvival.LOGGER.info("Reloading EZVCSurvival configuration...");
        loadConfigs();
    }

    public static void loadConfigs() {
        try {
            GeneralSoundsConfig.init();
            mergeGeneralSoundsFromJson();
            refreshPriorityGroups();
        } catch (Exception e) {
            EZVCSurvival.LOGGER.warn("Error loading JSON sound configs: {}", e.getMessage());
        }
    }

    private static void refreshPriorityGroups() {
        priorityGroups.clear();
        GeneralSoundsConfig.processPrioritySounds(priorityGroups);
    }

    private static void mergeGeneralSoundsFromJson() {
        customSoundGroups.removeIf(g -> g.groupName().startsWith("auto_sound_"));

        Map<String, GeneralSoundsConfig.SoundEntry> sounds = GeneralSoundsConfig.getSounds();
        if (sounds != null) {
            for (Map.Entry<String, GeneralSoundsConfig.SoundEntry> e : sounds.entrySet()) {
                GeneralSoundsConfig.SoundEntry se = e.getValue();
                customSoundGroups.add(new SoundGroupData(
                        "auto_sound_" + e.getKey().replace(':', '_').replace('.', '_'),
                        List.of(e.getKey()),
                        se.speed_multiplier,
                        se.range_multiplier
                ));
            }
        }
    }

    public static List<SoundGroupData> getEnabledSoundGroups() {
        return Collections.unmodifiableList(customSoundGroups);
    }
}