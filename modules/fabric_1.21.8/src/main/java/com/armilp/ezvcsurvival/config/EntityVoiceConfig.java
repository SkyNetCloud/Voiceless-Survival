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
import java.util.Objects;
import java.util.Set;

public final class EntityVoiceConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ROOT_TYPE = new TypeToken<RootConfig>() {
    }.getType();

    private static Map<String, EntityConfig> MONSTER_CONFIGS = new HashMap<>();
    private static Map<String, EntityConfig> ANIMAL_CONFIGS = new HashMap<>();
    public static RootConfig ROOT = new RootConfig();
    private static boolean isInitialized = false;

    private EntityVoiceConfig() {
    }

    public static void init() {
        if (!isInitialized) {
            loadOrCreate();
            // Run migration after loading
            migrateOldKeys();
            isInitialized = true;
        } else {
            reloadFromDisk();
        }
    }

    private static void reloadFromDisk() {
        Path path = getConfigPath();
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                RootConfig loaded = GSON.fromJson(reader, ROOT_TYPE);
                if (loaded != null) {
                    ROOT = loaded;

                    // Clean up the keys when loading
                    MONSTER_CONFIGS = cleanConfigMap(loaded.monsters);
                    ANIMAL_CONFIGS = cleanConfigMap(loaded.animals);

                    // Update ROOT with cleaned maps
                    ROOT.monsters = MONSTER_CONFIGS;
                    ROOT.animals = ANIMAL_CONFIGS;
                }
            } catch (Exception e) {
                EZVCSurvival.LOGGER.warn("Error reloading entities_voices.json: {}", e.getMessage());
            }
        }
    }

    public static Set<String> getAllEntityIds() {
        HashSet<String> all = new HashSet<>();
        all.addAll(MONSTER_CONFIGS.keySet());
        all.addAll(ANIMAL_CONFIGS.keySet());
        return all;
    }

    public static EntityConfig getMonster(String entityId) {
        String cleanId = cleanEntityId(entityId);
        return MONSTER_CONFIGS.get(cleanId);
    }

    public static EntityConfig getAnimal(String entityId) {
        String cleanId = cleanEntityId(entityId);
        return ANIMAL_CONFIGS.get(cleanId);
    }

    public static EntityConfig get(String entityId) {
        String cleanId = cleanEntityId(entityId);
        EntityConfig ec = MONSTER_CONFIGS.get(cleanId);
        if (ec == null) ec = ANIMAL_CONFIGS.get(cleanId);
        return ec;
    }

    public static void set(String entityId, EntityConfig value) {
        String cleanId = cleanEntityId(entityId);
        if (MONSTER_CONFIGS.containsKey(cleanId)) {
            MONSTER_CONFIGS.put(cleanId, value);
        } else if (ANIMAL_CONFIGS.containsKey(cleanId)) {
            ANIMAL_CONFIGS.put(cleanId, value);
        } else {
            // Default to monsters if not found
            MONSTER_CONFIGS.put(cleanId, value);
        }
    }

    // Helper method to clean entity IDs
    private static String cleanEntityId(String entityId) {
        if (entityId == null) return null;

        // Remove Optional[ResourceKey[...]] wrapper
        if (entityId.startsWith("Optional[ResourceKey[") && entityId.contains(" / ")) {
            // Extract the part after " / "
            int start = entityId.indexOf(" / ") + 3;
            int end = entityId.indexOf("]]", start);
            if (end != -1) {
                return entityId.substring(start, end);
            }
        }
        return entityId;
    }

    // Helper method to clean a config map
    private static Map<String, EntityConfig> cleanConfigMap(Map<String, EntityConfig> map) {
        if (map == null) return new HashMap<>();

        Map<String, EntityConfig> cleaned = new HashMap<>();
        for (Map.Entry<String, EntityConfig> entry : map.entrySet()) {
            String cleanKey = cleanEntityId(entry.getKey());
            cleaned.put(cleanKey, entry.getValue());
        }
        return cleaned;
    }

    // Migration method to clean up existing files
    public static void migrateOldKeys() {
        boolean needsMigration = false;

        // Check monsters
        Map<String, EntityConfig> cleanedMonsters = cleanConfigMap(MONSTER_CONFIGS);
        if (!cleanedMonsters.equals(MONSTER_CONFIGS)) {
            needsMigration = true;
            MONSTER_CONFIGS = cleanedMonsters;
        }

        // Check animals
        Map<String, EntityConfig> cleanedAnimals = cleanConfigMap(ANIMAL_CONFIGS);
        if (!cleanedAnimals.equals(ANIMAL_CONFIGS)) {
            needsMigration = true;
            ANIMAL_CONFIGS = cleanedAnimals;
        }

        if (needsMigration) {
            // Update ROOT
            ROOT.monsters = MONSTER_CONFIGS;
            ROOT.animals = ANIMAL_CONFIGS;
            persist();
            EZVCSurvival.LOGGER.info("Migrated old entity voice keys to clean format");
        }
    }

    public static void persist() {
        save(getConfigPath());
    }

    private static Path getConfigPath() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("ezvcsurvival");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {
        }
        return configDir.resolve("entities_voices.json");
    }

    private static void loadOrCreate() {
        Path path = getConfigPath();
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                RootConfig loaded = GSON.fromJson(reader, ROOT_TYPE);
                if (loaded != null) {
                    ROOT = loaded;

                    // Clean up keys when loading
                    MONSTER_CONFIGS = cleanConfigMap(loaded.monsters);
                    ANIMAL_CONFIGS = cleanConfigMap(loaded.animals);

                    // Update ROOT with cleaned maps
                    ROOT.monsters = MONSTER_CONFIGS;
                    ROOT.animals = ANIMAL_CONFIGS;
                } else {
                    ROOT = new RootConfig();
                    MONSTER_CONFIGS = new HashMap<>();
                    ANIMAL_CONFIGS = new HashMap<>();
                }
            } catch (JsonParseException | MalformedJsonException e) {
                EZVCSurvival.LOGGER.warn("Malformed JSON in entities_voices.json, using defaults (no backup): {}", e.getMessage());
                ROOT = new RootConfig();
                MONSTER_CONFIGS = new HashMap<>();
                ANIMAL_CONFIGS = new HashMap<>();
                generateDefaults();
                save(path);
            } catch (IOException e) {
                EZVCSurvival.LOGGER.warn("Error reading entities_voices.json, regenerating: {}", e.getMessage());
                ROOT = new RootConfig();
                MONSTER_CONFIGS = new HashMap<>();
                ANIMAL_CONFIGS = new HashMap<>();
                generateDefaults();
                save(path);
            }
        } else {
            ROOT = new RootConfig();
            generateDefaults();
            save(path);
        }

        boolean addedNew = ensureAllEntitiesPresent();
        if (addedNew) {
            save(path);
        }
    }

    private static boolean ensureAllEntitiesPresent() {
        boolean added = false;
        for (EntityType<?> type : Registries.ENTITY_TYPE) {
            SpawnGroup category = type.getSpawnGroup();
            if (category == SpawnGroup.MISC) continue;
            String id = Objects.requireNonNull(Registries.ENTITY_TYPE.getKey(type)).toString();

            // Use clean ID for checking
            String cleanId = cleanEntityId(id);

            if (isMonsterCategory(category)) {
                if (!MONSTER_CONFIGS.containsKey(cleanId)) {
                    MONSTER_CONFIGS.put(cleanId, EntityConfig.defaultFor(type));
                    added = true;
                }
            } else if (isAnimalLikeCategory(category)) {
                if (!ANIMAL_CONFIGS.containsKey(cleanId)) {
                    ANIMAL_CONFIGS.put(cleanId, EntityConfig.defaultFor(type));
                    added = true;
                }
            }
        }
        return added;
    }

    private static void generateDefaults() {
        MONSTER_CONFIGS.clear();
        ANIMAL_CONFIGS.clear();
        for (EntityType<?> type : Registries.ENTITY_TYPE) {
            SpawnGroup category = type.getSpawnGroup();
            if (category == SpawnGroup.MISC) continue;

            String id = Objects.requireNonNull(Registries.ENTITY_TYPE.getKey(type)).toString();
            // Store with clean ID
            String cleanId = cleanEntityId(id);

            if (isMonsterCategory(category)) {
                MONSTER_CONFIGS.put(cleanId, EntityConfig.defaultFor(type));
            } else if (isAnimalLikeCategory(category)) {
                ANIMAL_CONFIGS.put(cleanId, EntityConfig.defaultFor(type));
            }
        }

        // Use clean IDs for putIfPresent calls too
        putIfPresent(MONSTER_CONFIGS, "minecraft:zombie", new EntityConfig(true, 1.0, 60.0, -20.0));
        putIfPresent(MONSTER_CONFIGS, "minecraft:skeleton", new EntityConfig(true, 1.0, 40.0, -15.0));
        putIfPresent(MONSTER_CONFIGS, "quiet_place:death_angel", new EntityConfig(true, 1.0, 50.0, -10.0));
        putIfPresent(ANIMAL_CONFIGS, "minecraft:cow", new EntityConfig(true, 1.0, 25.0, -18.0));
        putIfPresent(ANIMAL_CONFIGS, "minecraft:pig", new EntityConfig(true, 1.0, 15.0, -18.0));
    }

    private static boolean isMonsterCategory(SpawnGroup category) {
        return category == SpawnGroup.MONSTER;
    }

    private static boolean isAnimalLikeCategory(SpawnGroup category) {
        return category == SpawnGroup.CREATURE
                || category == SpawnGroup.AMBIENT
                || category == SpawnGroup.WATER_CREATURE
                || category == SpawnGroup.UNDERGROUND_WATER_CREATURE
                || category.name().equalsIgnoreCase("AXOLOTLS");
    }

    private static void putIfPresent(Map<String, EntityConfig> map, String id, EntityConfig config) {
        String cleanId = cleanEntityId(id);
        if (map.containsKey(cleanId)) {
            map.put(cleanId, config);
        }
    }

    private static void save(Path path) {
        if (MONSTER_CONFIGS == null || ANIMAL_CONFIGS == null) {
            EZVCSurvival.LOGGER.warn("Cannot save null configuration maps");
            return;
        }

        try {
            Files.createDirectories(path.getParent());

            if (ROOT == null) {
                ROOT = new RootConfig();
            }

            ROOT.monsters = MONSTER_CONFIGS;
            ROOT.animals = ANIMAL_CONFIGS;

            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.WRITE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                GSON.toJson(ROOT, ROOT_TYPE, writer);
                writer.flush();
            }

            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival] Successfully saved entities_voices.json with enabled=" + ROOT.enabled);
            }

        } catch (IOException e) {
            EZVCSurvival.LOGGER.error("Failed to save entities_voices.json: {}", e.getMessage());
        }
    }

    public static boolean isEnabled() {
        return ROOT == null || ROOT.enabled;
    }

    public static void setEnabled(boolean enabled) {
        if (ROOT == null) ROOT = new RootConfig();
        ROOT.enabled = enabled;
        persist();

        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] EntityVoiceConfig setEnabled called: " + enabled);
        }
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
            double baseRange = 50.0;
            double baseThreshold = -20.0;

            if (type != null && type.getSpawnGroup() == SpawnGroup.MONSTER) {
                baseRange = 60.0;
            }
            return new EntityConfig(false, baseSpeed, baseRange, baseThreshold);
        }
    }

    public static final class RootConfig {
        public boolean enabled = true;
        public Map<String, EntityConfig> monsters;
        public Map<String, EntityConfig> animals;
    }

    public static EntityConfig getOrCreate(String entityId) {
        String cleanId = cleanEntityId(entityId);
        EntityConfig config = get(cleanId);

        if (config != null) {
            return config;
        }

        config = new EntityConfig(
                true,
                1.0,
                60.0,
                -20.0
        );

        set(cleanId, config);
        persist();

        System.out.println("[EZVCSurvival] Created default voice config for: " + cleanId);

        return config;
    }

}