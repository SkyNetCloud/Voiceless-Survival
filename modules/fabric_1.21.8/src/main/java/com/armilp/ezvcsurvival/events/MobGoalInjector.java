//package com.armilp.ezvcsurvival.events;
//
//import com.armilp.ezvcsurvival.config.*;
//import com.armilp.ezvcsurvival.data.SoundGroupData;
//import com.armilp.ezvcsurvival.goals.FollowVoiceGoal;
//import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
//import com.armilp.ezvcsurvival.goals.RunawayVoiceGoal;
//import com.armilp.ezvcsurvival.mixin.MobEntityAccessor;
//import net.minecraft.entity.EntityType;
//import net.minecraft.entity.ai.goal.Goal;
//import net.minecraft.entity.ai.goal.GoalSelector;
//import net.minecraft.entity.ai.goal.PrioritizedGoal;
//import net.minecraft.entity.mob.MobEntity;
//import net.minecraft.entity.passive.AnimalEntity;
//import net.minecraft.registry.Registries;
//import net.minecraft.server.command.ServerCommandSource;
//import net.minecraft.text.Text;
//import net.minecraft.util.Identifier;
//import net.minecraft.world.World;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
//public class MobGoalInjector {
//    private static final Map<UUID, MobEntity> TRACKED_MOBS = new ConcurrentHashMap<>();
//    private static final Map<UUID, InjectedGoals> INJECTED_GOALS = new ConcurrentHashMap<>();
//    private static final List<String> RECENT_DEBUG_LOGS = new ArrayList<>();
//    private static final int MAX_DEBUG_LOGS = 50;
//
//    public static void onEntityJoin(World level, MobEntity mob) {
//        debugLog("=== Entity Join ===");
//        debugLog("Entity: " + mob.getType().getTranslationKey());
//
//        if (level.isClient) {
//            debugLog("Skipping: client side");
//            return;
//        }
//
//        if (mob == null || mob.isRemoved()) {
//            debugLog("Skipping: null or removed");
//            return;
//        }
//
//        Identifier entityId = Registries.ENTITY_TYPE.getId(mob.getType());
//        debugLog("Entity ID: " + (entityId != null ? entityId.toString() : "unknown"));
//        debugLog("UUID: " + mob.getUuid());
//        debugLog("Is Animal: " + (mob instanceof AnimalEntity));
//
//        try {
//            // Clean up any existing goals first
//            cleanupOldGoals(mob);
//
//            // Inject new goals
//            boolean injected = injectGoals(mob);
//
//            if (injected) {
//                TRACKED_MOBS.put(mob.getUuid(), mob);
//                debugLog("Successfully injected goals");
//            } else {
//                debugLog("No goals were injected (config disabled or not applicable)");
//            }
//
//        } catch (Exception e) {
//            debugLog("ERROR in onEntityJoin: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    public static void onEntityRemove(MobEntity mob) {
//        if (mob == null) return;
//
//        UUID mobId = mob.getUuid();
//        debugLog("Entity removed: " + mob.getType().getTranslationKey() + " (UUID: " + mobId + ")");
//
//        cleanupOldGoals(mob);
//        TRACKED_MOBS.remove(mobId);
//        INJECTED_GOALS.remove(mobId);
//    }
//
//    private static boolean injectGoals(MobEntity mob) {
//        if (mob == null || mob.isRemoved() || mob.getWorld().isClient) {
//            return false;
//        }
//
//        Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
//        if (id == null) {
//            debugLog("Cannot get entity identifier");
//            return false;
//        }
//
//        String mobId = id.toString();
//        debugLog("Attempting to inject goals for: " + mobId);
//
//        InjectedGoals injectedGoals = new InjectedGoals();
//        boolean isAnimal = mob instanceof AnimalEntity;
//
//        debugLog("EntityVoiceConfig enabled: " + EntityVoiceConfig.isEnabled());
//        debugLog("GeneralSoundsConfig enabled: " + GeneralSoundsConfig.isEnabled());
//
//        // Inject Entity Voice Goals
//        if (EntityVoiceConfig.isEnabled()) {
//            if (!isAnimal) {
//                debugLog("Injecting FollowVoiceGoal for monster");
//                injectedGoals.followGoal = injectFollowVoiceGoal(mob, mobId);
//            } else {
//                debugLog("Injecting RunawayVoiceGoal for animal");
//                injectedGoals.runawayGoal = injectRunawayVoiceGoal((AnimalEntity) mob, mobId);
//            }
//        }
//
//        // Inject General Sound Goals
//        if (GeneralSoundsConfig.isEnabled()) {
//            debugLog("Injecting GeneralSoundGoal");
//            injectedGoals.generalSoundGoal = injectGeneralSoundGoal(mob, mobId);
//        }
//
//        // Store the injected goals
//        if (injectedGoals.hasAnyGoal()) {
//            INJECTED_GOALS.put(mob.getUuid(), injectedGoals);
//            debugLog("Stored injected goals for: " + mobId);
//
//            // Verify goals were actually added to selector
//            if (verifyGoalsInSelector(mob, injectedGoals)) {
//                debugLog("Goals verified in goal selector");
//            } else {
//                debugLog("WARNING: Goals not found in goal selector!");
//            }
//
//            return true;
//        } else {
//            debugLog("No goals were injected for: " + mobId);
//            return false;
//        }
//    }
//
//    private static Goal injectFollowVoiceGoal(MobEntity mob, String mobId) {
//        debugLog("  -> Checking FollowVoiceGoal config for: " + mobId);
//
//        try {
//            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getMonster(mobId);
//
//            if (cfg == null) {
//                debugLog("    No config found for: " + mobId);
//                return null;
//            }
//
//            debugLog("    Config found - enabled: " + cfg.enabled + ", speed: " + cfg.speed + ", range: " + cfg.range);
//
//            if (cfg.enabled && cfg.speed > 0 && cfg.range > 0) {
//                Goal goal = new FollowVoiceGoal(mob, cfg.speed, (int) cfg.range, cfg.threshold, 10000);
//                MobEntityAccessor mobEntityAccessor = (MobEntityAccessor) mob;
//                GoalSelector goalSelector = mobEntityAccessor.vs$getGoalSelector();
//
//                if (goalSelector != null) {
//                    goalSelector.add(0, goal); // High priority
//                    debugLog("    FollowVoiceGoal injected successfully with priority 0");
//                    return goal;
//                } else {
//                    debugLog("    ERROR: GoalSelector is null!");
//                }
//            } else {
//                debugLog("    Config not valid for injection");
//            }
//        } catch (Exception e) {
//            debugLog("    ERROR injecting FollowVoiceGoal: " + e.getMessage());
//            e.printStackTrace();
//        }
//
//        return null;
//    }
//
//    private static Goal injectRunawayVoiceGoal(AnimalEntity animal, String mobId) {
//        debugLog("  -> Checking RunawayVoiceGoal config for: " + mobId);
//
//        try {
//            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getAnimal(mobId);
//
//            if (cfg == null) {
//                debugLog("    No config found for: " + mobId);
//                return null;
//            }
//
//            debugLog("    Config found - enabled: " + cfg.enabled + ", speed: " + cfg.speed + ", range: " + cfg.range);
//
//            if (cfg.enabled && cfg.speed > 0 && cfg.range > 0) {
//                Goal goal = new RunawayVoiceGoal(animal, cfg.speed, (int) cfg.range, cfg.threshold);
//                MobEntityAccessor mobEntityAccessor = (MobEntityAccessor) animal;
//                GoalSelector goalSelector = mobEntityAccessor.vs$getGoalSelector();
//
//                if (goalSelector != null) {
//                    goalSelector.add(4, goal); // Medium-high priority
//                    debugLog("    RunawayVoiceGoal injected successfully with priority 4");
//                    return goal;
//                } else {
//                    debugLog("    ERROR: GoalSelector is null!");
//                }
//            } else {
//                debugLog("    Config not valid for injection");
//            }
//        } catch (Exception e) {
//            debugLog("    ERROR injecting RunawayVoiceGoal: " + e.getMessage());
//            e.printStackTrace();
//        }
//
//        return null;
//    }
//
//    private static Goal injectGeneralSoundGoal(MobEntity mob, String mobId) {
//        debugLog("  -> Checking GeneralSoundGoal config for: " + mobId);
//
//        try {
//            GeneralSoundsConfig.Reaction r = GeneralSoundsConfig.getMobReactions().get(mobId);
//
//            if (r == null) {
//                debugLog("    No reaction config found for: " + mobId);
//                return null;
//            }
//
//            debugLog("    Reaction config found - enabled: " + r.enabled + ", speed: " + r.speed + ", range: " + r.range);
//
//            if (r.enabled && r.speed > 0 && r.range > 0) {
//                List<SoundGroupData> groups = SoundConfig.getEnabledSoundGroups();
//
//                if (groups != null && !groups.isEmpty()) {
//                    Goal goal = new ReactToGeneralSoundGoal(mob, r.speed, (int) r.range, groups);
//                    MobEntityAccessor mobEntityAccessor = (MobEntityAccessor) mob;
//                    GoalSelector goalSelector = mobEntityAccessor.vs$getGoalSelector();
//
//                    if (goalSelector != null) {
//                        goalSelector.add(2, goal); // High priority
//                        debugLog("    ReactToGeneralSoundGoal injected successfully with priority 2");
//                        return goal;
//                    } else {
//                        debugLog("    ERROR: GoalSelector is null!");
//                    }
//                } else {
//                    debugLog("    No enabled sound groups found");
//                }
//            } else {
//                debugLog("    Reaction config not valid for injection");
//            }
//        } catch (Exception e) {
//            debugLog("    ERROR injecting ReactToGeneralSoundGoal: " + e.getMessage());
//            e.printStackTrace();
//        }
//
//        return null;
//    }
//
//    private static boolean verifyGoalsInSelector(MobEntity mob, InjectedGoals injected) {
//        try {
//            MobEntityAccessor mobEntityAccessor = (MobEntityAccessor) mob;
//            GoalSelector goalSelector = mobEntityAccessor.vs$getGoalSelector();
//
//            if (goalSelector == null) {
//                debugLog("    Cannot verify: GoalSelector is null");
//                return false;
//            }
//
//            Set<PrioritizedGoal> availableGoals = goalSelector.getGoals();
//
//            boolean hasFollowGoal = injected.followGoal == null ||
//                    availableGoals.stream().anyMatch(wg -> wg.getGoal() == injected.followGoal);
//            boolean hasRunawayGoal = injected.runawayGoal == null ||
//                    availableGoals.stream().anyMatch(wg -> wg.getGoal() == injected.runawayGoal);
//            boolean hasGeneralSoundGoal = injected.generalSoundGoal == null ||
//                    availableGoals.stream().anyMatch(wg -> wg.getGoal() == injected.generalSoundGoal);
//
//            debugLog("    Goal verification:");
//            debugLog("      - FollowGoal present: " + hasFollowGoal);
//            debugLog("      - RunawayGoal present: " + hasRunawayGoal);
//            debugLog("      - GeneralSoundGoal present: " + hasGeneralSoundGoal);
//
//            return hasFollowGoal && hasRunawayGoal && hasGeneralSoundGoal;
//        } catch (Exception e) {
//            debugLog("    ERROR verifying goals: " + e.getMessage());
//            return false;
//        }
//    }
//
//    private static void cleanupOldGoals(MobEntity mob) {
//        if (mob == null) return;
//
//        UUID mobId = mob.getUuid();
//        InjectedGoals injected = INJECTED_GOALS.get(mobId);
//
//        if (injected != null) {
//            debugLog("Cleaning up old goals for: " + mob.getType().getTranslationKey());
//
//            try {
//                removeGoal(mob, injected.followGoal);
//                removeGoal(mob, injected.runawayGoal);
//                removeGoal(mob, injected.generalSoundGoal);
//            } catch (Exception e) {
//                debugLog("ERROR removing old goals: " + e.getMessage());
//            }
//
//            INJECTED_GOALS.remove(mobId);
//        }
//    }
//
//    private static void removeGoal(MobEntity mob, Goal goal) {
//        if (goal != null && mob != null) {
//            try {
//                MobEntityAccessor mobEntityAccessor = (MobEntityAccessor) mob;
//                GoalSelector goalSelector = mobEntityAccessor.vs$getGoalSelector();
//
//                if (goalSelector != null) {
//                    goalSelector.remove(goal);
//                    debugLog("    Removed goal: " + goal.getClass().getSimpleName());
//                }
//            } catch (Exception e) {
//                debugLog("    ERROR removing goal: " + e.getMessage());
//            }
//        }
//    }
//
//    public static void refreshAll() {
//        debugLog("=== Refreshing All Goals ===");
//
//        for (MobEntity mob : new ArrayList<>(TRACKED_MOBS.values())) {
//            if (mob == null || mob.isRemoved() || mob.getWorld().isClient) {
//                TRACKED_MOBS.remove(mob.getUuid());
//                continue;
//            }
//
//            debugLog("Refreshing goals for: " + mob.getType().getTranslationKey());
//            cleanupOldGoals(mob);
//            injectGoals(mob);
//        }
//
//        debugLog("Refresh complete. Total tracked mobs: " + TRACKED_MOBS.size());
//    }
//
//    public static void refreshEntityId(String entityId) {
//        debugLog("=== Refreshing Goals for Entity: " + entityId + " ===");
//
//        for (MobEntity mob : new ArrayList<>(TRACKED_MOBS.values())) {
//            if (mob == null || mob.isRemoved()) continue;
//
//            Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
//            if (id != null && id.toString().equals(entityId)) {
//                debugLog("Refreshing: " + entityId);
//                cleanupOldGoals(mob);
//                injectGoals(mob);
//            }
//        }
//    }
//
//    // ===== Debugging Methods =====
//
//    private static void debugLog(String message) {
//        if (VoiceConfig.DEBUG.get()) {
//            System.out.println("[EZVCSurvival] " + message);
//
//            // Store recent logs for debugging
//            synchronized (RECENT_DEBUG_LOGS) {
//                RECENT_DEBUG_LOGS.add(System.currentTimeMillis() + ": " + message);
//                if (RECENT_DEBUG_LOGS.size() > MAX_DEBUG_LOGS) {
//                    RECENT_DEBUG_LOGS.remove(0);
//                }
//            }
//        }
//    }
//
//    public static List<String> getDebugInfo() {
//        List<String> info = new ArrayList<>();
//        info.add("=== EZVC Survival Debug Info ===");
//        info.add("Tracked mobs: " + TRACKED_MOBS.size());
//        info.add("Mobs with injected goals: " + INJECTED_GOALS.size());
//        info.add("EntityVoiceConfig enabled: " + EntityVoiceConfig.isEnabled());
//        info.add("GeneralSoundsConfig enabled: " + GeneralSoundsConfig.isEnabled());
//
//        info.add("\nTracked mobs:");
//        for (Map.Entry<UUID, MobEntity> entry : TRACKED_MOBS.entrySet()) {
//            MobEntity mob = entry.getValue();
//            if (mob != null) {
//                Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
//                InjectedGoals goals = INJECTED_GOALS.get(entry.getKey());
//                info.add("  - " + (id != null ? id.toString() : "unknown") +
//                        " (hasGoals: " + (goals != null && goals.hasAnyGoal()) + ")");
//            }
//        }
//
//        return info;
//    }
//
//    public static List<String> getRecentDebugLogs() {
//        synchronized (RECENT_DEBUG_LOGS) {
//            return new ArrayList<>(RECENT_DEBUG_LOGS);
//        }
//    }
//
//    public static int getTrackedMobCount() {
//        return TRACKED_MOBS.size();
//    }
//
//    public static int getInjectedGoalCount() {
//        return INJECTED_GOALS.size();
//    }
//
//    public static void forceInjectTestGoal(ServerCommandSource source) {
//        debugLog("=== Force Inject Test ===");
//
//        // Find a nearby mob to test with
//        List<MobEntity> nearbyMobs = source.getWorld().getEntitiesByClass(
//                MobEntity.class,
//                source.getEntity().getBoundingBox().expand(20),
//                mob -> true
//        );
//
//        if (nearbyMobs.isEmpty()) {
//            source.sendFeedback(() -> Text.literal("No nearby mobs found for testing"), false);
//            return;
//        }
//
//        MobEntity testMob = nearbyMobs.get(0);
//        debugLog("Testing with: " + testMob.getType().getTranslationKey());
//
//        // Force inject a test goal
//        try {
//            Goal testGoal = new FollowVoiceGoal(testMob, 1.0, 10, 0.5, 10000);
//            MobEntityAccessor mobEntityAccessor = (MobEntityAccessor) testMob;
//            GoalSelector goalSelector = mobEntityAccessor.vs$getGoalSelector();
//
//            if (goalSelector != null) {
//                goalSelector.add(0, testGoal);
//                debugLog("Test goal injected successfully");
//                source.sendFeedback(() -> Text.literal("Test goal injected into " +
//                        testMob.getType().getTranslationKey()), false);
//            } else {
//                source.sendFeedback(() -> Text.literal("ERROR: GoalSelector is null!"), false);
//            }
//        } catch (Exception e) {
//            source.sendFeedback(() -> Text.literal("ERROR: " + e.getMessage()), false);
//            e.printStackTrace();
//        }
//    }
//
//    // ===== Helper Classes =====
//
//    private static class InjectedGoals {
//        Goal followGoal;
//        Goal runawayGoal;
//        Goal generalSoundGoal;
//
//        boolean hasAnyGoal() {
//            return followGoal != null || runawayGoal != null || generalSoundGoal != null;
//        }
//    }
//}