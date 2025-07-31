package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.utils.InjectorLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SoundConfig {

        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
        private static final File CONFIG_FILE = new File("config/ezvcsurvival/sounds.json");

        private static final Map<String, SoundGroupData> soundGroupDataMap = new HashMap<>();
        private static final Map<String, MobReaction> mobReactionsMap = new HashMap<>();

        public static double THUNDER_RANGE_MULTIPLIER = 0.8;

        public static class MobReaction {
            public double speed = 1.0;
            public double range = 16.0;
            public List<String> groups = new ArrayList<>();
        }

        public static class SoundGroupEntry {
            public List<String> sounds = new ArrayList<>();
            @SerializedName("speed_multiplier")
            public double speedMultiplier = 1.0;
            @SerializedName("range_multiplier")
            public double rangeMultiplier = 1.0;

            public SoundGroupData toData(String name) {
                return new SoundGroupData(name, sounds, speedMultiplier, rangeMultiplier);
            }
        }

        public static class ConfigData {
            @SerializedName("sound_groups")
            public Map<String, SoundGroupEntry> soundGroups = new HashMap<>();
            @SerializedName("mob_sound_reactions")
            public Map<String, MobReaction> mobReactions = new HashMap<>();
            @SerializedName("thunder_range_multiplier")
            public double thunderRangeMultiplier = 0.8;
        }

        public static void load() {
            if (!CONFIG_FILE.exists()) {
                InjectorLogger.logInfo(SoundConfig.class, "Creating default sound config file...");
                saveDefaults();
            }

            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ConfigData config = GSON.fromJson(reader, ConfigData.class);

                soundGroupDataMap.clear();
                for (Map.Entry<String, SoundGroupEntry> entry : config.soundGroups.entrySet()) {
                    soundGroupDataMap.put(entry.getKey(), entry.getValue().toData(entry.getKey()));
                }

                mobReactionsMap.clear();
                mobReactionsMap.putAll(config.mobReactions);

                THUNDER_RANGE_MULTIPLIER = config.thunderRangeMultiplier;

                InjectorLogger.logInfo(SoundConfig.class,"Loaded sound config successfully.");

            } catch (Exception ignored) {
            }
        }

        private static void saveDefaults() {
            ConfigData defaults = new ConfigData();

            defaults.soundGroups.put("wood_sounds", createGroup(
                    List.of("block.wood.break", "block.wood.hit", "block.wood.place")
            ));
            defaults.soundGroups.put("animal_hurts", createGroup(
                    List.of("entity.cow.hurt", "entity.pig.hurt")
            ));

            defaults.mobReactions.put("minecraft:zombie", createReaction(1.5, 20.0, List.of("wood_sounds")));
            defaults.mobReactions.put("minecraft:cow", createReaction(1.8, 16.0, List.of("animal_hurts")));

            defaults.thunderRangeMultiplier = 0.8;

            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(defaults, writer);
            } catch (Exception ignored) {

            }
        }

        private static SoundGroupEntry createGroup(List<String> sounds) {
            SoundGroupEntry entry = new SoundGroupEntry();
            entry.sounds = sounds;
            entry.speedMultiplier = 1.0;
            entry.rangeMultiplier = 1.0;
            return entry;
        }

        private static MobReaction createReaction(double speed, double range, List<String> groups) {
            MobReaction reaction = new MobReaction();
            reaction.speed = speed;
            reaction.range = range;
            reaction.groups = groups;
            return reaction;
        }

        @SuppressWarnings("StatementWithEmptyBody")
        public static List<SoundGroupData> getSoundGroupsForMob(String mobId) {
            MobReaction reaction = mobReactionsMap.get(mobId);
            if (reaction == null || reaction.groups == null) return List.of();

            List<SoundGroupData> result = new ArrayList<>();
            for (String group : reaction.groups) {
                SoundGroupData data = soundGroupDataMap.get(group);
                if (data != null) {
                    result.add(data);
                } else {

                }
            }
            return result;
        }

        public static MobReaction getMobSoundReaction(String mobId) {
            return mobReactionsMap.get(mobId);
        }
}