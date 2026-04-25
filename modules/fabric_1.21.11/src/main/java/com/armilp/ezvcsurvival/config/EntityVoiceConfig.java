package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.MalformedJsonException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class EntityVoiceConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ROOT_TYPE = new TypeToken<RootConfig>() {}.getType();

    private static Map<String, EntityConfig> MONSTER_CONFIGS = new HashMap<>();
    private static Map<String, EntityConfig> ANIMAL_CONFIGS = new HashMap<>();
    public static RootConfig ROOT = new RootConfig();

    private EntityVoiceConfig() {}

    public static void init() {
        loadOrCreate();
        migrateOldKeys();
    }

    public static Set<String> getAllEntityIds() {
        HashSet<String> all = new HashSet<>();
        all.addAll(MONSTER_CONFIGS.keySet());
        all.addAll(ANIMAL_CONFIGS.keySet());
        return all;
    }

    public static EntityConfig getMonster(String entityId) {
        return MONSTER_CONFIGS.get(entityId);
    }

    public static EntityConfig getAnimal(String entityId) {
        return ANIMAL_CONFIGS.get(entityId);
    }


    public static EntityConfig get(String entityId) {
        EntityConfig ec = MONSTER_CONFIGS.get(entityId);
        if (ec == null) ec = ANIMAL_CONFIGS.get(entityId);
        return ec;
    }

    public static void set(String entityId, EntityConfig value) {
        if (MONSTER_CONFIGS == null) MONSTER_CONFIGS = new HashMap<>();
        if (ANIMAL_CONFIGS == null) ANIMAL_CONFIGS = new HashMap<>();


        if (MONSTER_CONFIGS.containsKey(entityId)) {
            MONSTER_CONFIGS.put(entityId, value);
        } else if (ANIMAL_CONFIGS.containsKey(entityId)) {
            ANIMAL_CONFIGS.put(entityId, value);
        } else {
            MONSTER_CONFIGS.put(entityId, value);
        }
    }



    private static Map<String, EntityConfig> cleanConfigMap(Map<String, EntityConfig> map) {
        if (map == null) return new HashMap<>();
        Map<String, EntityConfig> cleaned = new HashMap<>();
        for (Map.Entry<String, EntityConfig> entry : map.entrySet()) {
            cleaned.put(entry.getKey(), entry.getValue());
        }
        return cleaned;
    }

    public static void migrateOldKeys() {
        Map<String, EntityConfig> cleanedMonsters = cleanConfigMap(MONSTER_CONFIGS);
        Map<String, EntityConfig> cleanedAnimals = cleanConfigMap(ANIMAL_CONFIGS);

        MONSTER_CONFIGS = cleanedMonsters;
        ANIMAL_CONFIGS = cleanedAnimals;

        ROOT.monsters = MONSTER_CONFIGS;
        ROOT.animals = ANIMAL_CONFIGS;
        persist();
    }

    public static void persist() {
        save(getConfigPath());
    }

    private static Path getConfigPath() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("ezvcsurvival");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {}
        return configDir.resolve("entities_voices.json");
    }

    private static void loadOrCreate() {
        Path path = getConfigPath();

        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                RootConfig loaded = GSON.fromJson(reader, ROOT_TYPE);

                if (loaded != null) {
                    ROOT = loaded;
                    MONSTER_CONFIGS = cleanConfigMap(loaded.monsters);
                    ANIMAL_CONFIGS = cleanConfigMap(loaded.animals);
                    ROOT.monsters = MONSTER_CONFIGS;
                    ROOT.animals = ANIMAL_CONFIGS;
                } else {
                    resetDefaults();
                }

            } catch (JsonParseException | MalformedJsonException e) {
                resetDefaults();
                save(path);
            } catch (IOException e) {
                resetDefaults();
                save(path);
            }
        } else {
            resetDefaults();
            save(path);
        }

        if (ensureAllEntitiesPresent()) {
            save(path);
        }
    }

    private static void resetDefaults() {
        ROOT = new RootConfig();
        MONSTER_CONFIGS = new HashMap<>();
        ANIMAL_CONFIGS = new HashMap<>();
        generateDefaults();
    }

    private static boolean ensureAllEntitiesPresent() {
        boolean added = false;

        for (EntityType<?> type : Registries.ENTITY_TYPE) {
            SpawnGroup category = type.getSpawnGroup();
            if (category == SpawnGroup.MISC) continue;

            String id = Registries.ENTITY_TYPE.getId(type).toString();

            if (category == SpawnGroup.MONSTER) {
                if (!MONSTER_CONFIGS.containsKey(id)) {
                    MONSTER_CONFIGS.put(id, EntityConfig.defaultFor(type));
                    added = true;
                }
            } else {
                if (!ANIMAL_CONFIGS.containsKey(id)) {
                    ANIMAL_CONFIGS.put(id, EntityConfig.defaultFor(type));
                    added = true;
                }
            }
        }

        return added;
    }

    private static void generateDefaults() {
        for (EntityType<?> type : Registries.ENTITY_TYPE) {
            SpawnGroup category = type.getSpawnGroup();
            if (category == SpawnGroup.MISC) continue;

            String id = Registries.ENTITY_TYPE.getId(type).toString();

            if (category == SpawnGroup.MONSTER) {
                MONSTER_CONFIGS.put(id, EntityConfig.defaultFor(type));
            } else {
                ANIMAL_CONFIGS.put(id, EntityConfig.defaultFor(type));
            }
        }
    }

    private static void save(Path path) {
        try {
            Files.createDirectories(path.getParent());

            ROOT.monsters = MONSTER_CONFIGS;
            ROOT.animals = ANIMAL_CONFIGS;

            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(ROOT, ROOT_TYPE, writer);
            }

        } catch (IOException e) {
            EZVCSurvival.LOGGER.error("Failed to save entities_voices.json: {}", e.getMessage());
        }
    }

    public static boolean isEnabled() {
        return ROOT == null || ROOT.enabled;
    }

    public static final class EntityConfig {
        public boolean enabled;
        public double speed;
        public double range;
        public double threshold;

        public EntityConfig(boolean enabled, double speed, double range, double threshold) {
            this.enabled = enabled;
            this.speed = speed;
            this.range = range;
            this.threshold = threshold;
        }

        public static EntityConfig defaultFor(EntityType<?> type) {
            double baseSpeed = 1.0;
            double baseRange = type != null && type.getSpawnGroup() == SpawnGroup.MONSTER ? 60.0 : 50.0;
            return new EntityConfig(false, baseSpeed, baseRange, -20.0);
        }
    }

    public static final class RootConfig {
        public boolean enabled = true;
        public Map<String, EntityConfig> monsters;
        public Map<String, EntityConfig> animals;
    }
}