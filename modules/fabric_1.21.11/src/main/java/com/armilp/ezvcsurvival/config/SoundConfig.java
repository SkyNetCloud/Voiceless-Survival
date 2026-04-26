package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.compat.guns.PointBlankSoundsConfig;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.*;

public class SoundConfig {

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

    public static void loadConfigs() {
        try {
            GeneralSoundsConfig.init();

            mergeGeneralSoundsFromJson();
            refreshPriorityGroups();

            EZVCSurvival.LOGGER.info("[EZVC] Loaded {} sound groups", customSoundGroups.size());

        } catch (Exception e) {
            EZVCSurvival.LOGGER.warn("Error loading JSON sound configs: {}", e.getMessage());
        }
    }

    private static void refreshPriorityGroups() {
        priorityGroups.clear();
        GeneralSoundsConfig.processPrioritySounds(priorityGroups);
    }

    private static void mergeGeneralSoundsFromJson() {
        customSoundGroups.clear();

        Map<String, GeneralSoundsConfig.SoundEntry> sounds = GeneralSoundsConfig.getSounds();
        if (sounds == null) return;

        customSoundGroups.addAll(PointBlankSoundsConfig.getGroups(sounds));

        for (Map.Entry<String, GeneralSoundsConfig.SoundEntry> e : sounds.entrySet()) {
            GeneralSoundsConfig.SoundEntry se = e.getValue();
            if (se == null || !se.enabled) continue;
            customSoundGroups.add(new SoundGroupData("auto_sound_" + e.getKey().replace(':', '_').replace('.', '_'), List.of(e.getKey()), se.speed_multiplier, se.range_multiplier));
        }
    }

    public static List<SoundGroupData> getEnabledSoundGroups() {
        List<SoundGroupData> all = new ArrayList<>(customSoundGroups);
        all.addAll(priorityGroups);
        return all;
    }

    public static List<SoundGroupData> getPriorityGroups() {
        return Collections.unmodifiableList(priorityGroups);
    }
}