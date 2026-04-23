package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.MalformedJsonException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class GunfireConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ROOT_TYPE = new TypeToken<Root>() {
    }.getType();

    public static Root ROOT = new Root();

    private GunfireConfig() {
    }

    public static void init() {
        loadOrCreate();
    }

    public static Map<String, Reaction> getMobReactions() {
        if (ROOT == null) ROOT = new Root();
        if (ROOT.mobs == null) ROOT.mobs = new HashMap<>();
        return ROOT.mobs;
    }

    public static boolean isEnabled() {
        return ROOT == null || ROOT.enabled;
    }

    private static Path getPath() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("ezvcsurvival");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
        return dir.resolve("gunfire.json");
    }

    private static void loadOrCreate() {
        Path path = getPath();

        if (Files.exists(path)) {
            try {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                try (java.io.StringReader r = new java.io.StringReader(content)) {
                    Root loaded = GSON.fromJson(r, ROOT_TYPE);
                    ROOT = loaded != null ? loaded : defaultRoot();
                }
            } catch (MalformedJsonException e) {
                EZVCSurvival.LOGGER.warn("Malformed JSON in gunfire.json, using defaults: {}", e.getMessage());
                ROOT = defaultRoot();
                save(path);
            } catch (IOException e) {
                EZVCSurvival.LOGGER.warn("Error reading gunfire.json, regenerating: {}", e.getMessage());
                ROOT = defaultRoot();
                save(path);
            }
        } else {
            ROOT = defaultRoot();
            save(path);
        }

        normalizeMobKeys();

        boolean changed = ensureAllPresent();
        if (changed) save(path);
    }


    private static void normalizeMobKeys() {
        if (ROOT.mobs == null) return;

        Map<String, Reaction> cleaned = new HashMap<>();

        for (Map.Entry<String, Reaction> entry : ROOT.mobs.entrySet()) {
            String key = entry.getKey();

            if (key != null && key.contains(" / ")) {
                int start = key.indexOf(" / ") + 3;
                int end = key.indexOf("]]", start);
                if (end != -1) {
                    key = key.substring(start, end);
                }
            }

            cleaned.put(key, entry.getValue());
        }

        ROOT.mobs.clear();
        ROOT.mobs.putAll(cleaned);
    }


    private static boolean ensureAllPresent() {
        boolean changed = false;

        if (ROOT.mobs == null) {
            ROOT.mobs = new HashMap<>();
            changed = true;
        }

        for (EntityType<?> type : Registries.ENTITY_TYPE) {
            SpawnGroup cat = type.getSpawnGroup();
            if (cat == SpawnGroup.MISC) continue;

            String id = Objects.requireNonNull(Registries.ENTITY_TYPE.getKey(type)).toString();

            if (!ROOT.mobs.containsKey(id)) {
                ROOT.mobs.put(id, new Reaction(
                        true, // ✅ ENABLED BY DEFAULT
                        1.0,
                        cat == SpawnGroup.MONSTER ? 50.0 : 40.0
                ));
                changed = true;
            }
        }


        forceEnable(ROOT.mobs, "minecraft:zombie", new Reaction(true, 1.2, 60.0));
        forceEnable(ROOT.mobs, "minecraft:skeleton", new Reaction(true, 1.1, 55.0));
        forceEnable(ROOT.mobs, "minecraft:cow", new Reaction(true, 1.0, 40.0));
        forceEnable(ROOT.mobs, "minecraft:pig", new Reaction(true, 1.0, 40.0));

        activateEntitiesFromMod(ROOT.mobs, "zombie_extreme", new Reaction(true, 1.0, 50.0));
        activateEntitiesFromMod(ROOT.mobs, "apocalypsenow", new Reaction(true, 1.0, 50.0));

        return changed;
    }


    private static void forceEnable(Map<String, Reaction> map, String id, Reaction newReaction) {
        Reaction current = map.get(id);

        if (current == null || !current.enabled) {
            map.put(id, newReaction);
        }
    }

    private static void activateEntitiesFromMod(Map<String, Reaction> map, String modId, Reaction reaction) {
        for (String entityId : map.keySet()) {
            if (entityId.startsWith(modId + ":")) {
                map.put(entityId, reaction);
            }
        }
    }

    private static Root defaultRoot() {
        Root r = new Root();
        r.mobs = new HashMap<>();
        return r;
    }

    private static void save(Path path) {
        if (ROOT == null) {
            EZVCSurvival.LOGGER.warn("Cannot save null ROOT configuration");
            return;
        }

        try {
            Files.createDirectories(path.getParent());

            try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.WRITE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                GSON.toJson(ROOT, ROOT_TYPE, w);
                w.flush();
            }

        } catch (IOException e) {
            EZVCSurvival.LOGGER.error("Failed to save gunfire.json: {}", e.getMessage());
        }
    }

    public static void setMobReaction(String entityId, boolean enabled, double speed, double range) {
        if (ROOT.mobs == null) ROOT.mobs = new HashMap<>();
        ROOT.mobs.put(entityId, new Reaction(enabled, speed, range));
    }

    public static void persist() {
        save(getPath());
    }


    public static final class Root {
        public boolean enabled = true;
        public Map<String, Reaction> mobs;
    }

    public static final class Reaction {
        public boolean enabled;
        public double speed;
        public double range;

        public Reaction(boolean enabled, double speed, double range) {
            this.enabled = enabled;
            this.speed = speed;
            this.range = range;
        }
    }
}