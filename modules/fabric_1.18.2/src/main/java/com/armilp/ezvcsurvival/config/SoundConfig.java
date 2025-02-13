package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.data.GunTypeModifiers;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.*;
import java.util.stream.Collectors;


public class SoundConfig {

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SOUND_GROUPS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOB_SOUND_REACTIONS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GUN_TYPE_MODIFIERS;

    private static final Map<String, SoundGroupData> soundGroupDataMap = new HashMap<>();
    private static final Map<String, Map<String, Object>> mobReactionsMap = new HashMap<>();
    private static final Map<String, GunTypeModifiers> gunModifiersMap = new HashMap<>();

    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Sound groups configuration");
        builder.push("sound_groups");
        SOUND_GROUPS = builder.defineList("groups",
                () -> List.of(
                        "wood_sounds=block.wood.break,block.wood.hit,block.wood.place,1.0,1.0",
                        "animal_hurts=entity.cow.hurt,entity.pig.hurt"
                ),
                obj -> obj instanceof String && ((String) obj).contains("="));
        builder.pop();

        builder.comment("Mob sound reactions configuration");
        builder.push("mob_sound_reactions");
        MOB_SOUND_REACTIONS = builder.defineList("reactions",
                () -> List.of(
                        "minecraft:zombie=speed=1.5,range=20,groups=wood_sounds",
                        "minecraft:cow=speed=1.8,range=16,groups=animal_hurts"
                ),
                obj -> obj instanceof String && ((String) obj).contains("="));
        builder.pop();

        builder.comment("Gun type modifiers configuration");
        builder.push("gun_type_modifiers");
        GUN_TYPE_MODIFIERS = builder.defineList("modifiers",
                () -> List.of(
                        "pistol=1.2,3.8",
                        "sniper=1.5,8.0",
                        "rifle=1.3,6.5",
                        "shotgun=1.4,4.8",
                        "smg=1.2,3.0",
                        "rpg=1.8,10.0",
                        "mg=1.5,4.0"
                ),
                obj -> obj instanceof String && ((String) obj).contains("="));
        builder.pop();

        SPEC = builder.build();
    }

    public static void onModConfigReload() {
        EZVCSurvival.LOGGER.info("Reloading EZVCSurvival config...");
        loadConfigs();
    }

    public static void loadConfigs() {
        loadSoundGroups();
        loadMobSoundReactions();
        loadGunTypeModifiers();
    }

    private static void loadSoundGroups() {
        soundGroupDataMap.clear();
        List<? extends String> groups = SOUND_GROUPS.get();
        if (groups != null) {
            for (String entry : groups) {
                String[] parts = entry.split("=", 2);
                if (parts.length < 2) continue;
                String groupName = parts[0].trim();
                List<String> tokens = Arrays.stream(parts[1].split(","))
                        .map(String::trim)
                        .collect(Collectors.toList());
                soundGroupDataMap.put(groupName, new SoundGroupData(groupName, tokens, 1.0, 1.0));
            }
        }
    }

    private static void loadMobSoundReactions() {
        mobReactionsMap.clear();
        List<? extends String> reactions = MOB_SOUND_REACTIONS.get();
        if (reactions != null) {
            for (String entry : reactions) {
                String[] parts = entry.split("=", 2);
                if (parts.length < 2) continue;
                String mobId = parts[0].trim();
                Map<String, Object> map = new HashMap<>();
                map.put("groups", Arrays.asList(parts[1].split(",")));
                mobReactionsMap.put(mobId, map);
            }
        }
    }

    private static void loadGunTypeModifiers() {
        gunModifiersMap.clear();
        List<? extends String> modifiers = GUN_TYPE_MODIFIERS.get();
        if (modifiers != null) {
            for (String entry : modifiers) {
                String[] parts = entry.split("=", 2);
                if (parts.length < 2) continue;
                String gunType = parts[0].trim();
                String[] values = parts[1].split(",");
                try {
                    double speed = Double.parseDouble(values[0]);
                    double range = Double.parseDouble(values[1]);
                    gunModifiersMap.put(gunType, new GunTypeModifiers(speed, range));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    public static List<SoundGroupData> getSoundGroupsForMob(String mobId) {
        List<SoundGroupData> list = new ArrayList<>();
        Map<String, Object> reaction = mobReactionsMap.get(mobId);
        if (reaction != null && reaction.containsKey("groups")) {
            @SuppressWarnings("unchecked")
            List<String> groups = (List<String>) reaction.get("groups");
            for (String group : groups) {
                SoundGroupData data = soundGroupDataMap.get(group);
                if (data != null) {
                    list.add(data);
                } else {
                    EZVCSurvival.LOGGER.warn("No se encontró el grupo: " + group + " para el mob: " + mobId);
                }
            }
        }
        return list;
    }

    public static double getSpeedMultiplier(String gunType) {
        GunTypeModifiers mod = gunModifiersMap.get(gunType.toLowerCase());
        return mod != null ? mod.speedMultiplier() : 1.0;
    }

    public static double getRangeMultiplier(String gunType) {
        GunTypeModifiers mod = gunModifiersMap.get(gunType.toLowerCase());
        return mod != null ? mod.rangeMultiplier() : 1.0;
    }

    public static Map<String, Object> getMobSoundReaction(String mobId) {
        return mobReactionsMap.get(mobId);
    }
}
