package com.armilp.ezvcsurvival.utils;

import com.armilp.ezvcsurvival.config.*;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.goals.FollowVoiceGoal;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import com.armilp.ezvcsurvival.goals.RunawayVoiceGoal;
import com.armilp.ezvcsurvival.mixin.MobEntityAccessor;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.*;

public class MobGoalUpdater {

    private static final Map<UUID, WeakReference<MobEntity>> ALL_MOBS = Collections.synchronizedMap(new WeakHashMap<>());

    public static void registerMob(MobEntity mob) {
        if (mob != null && !mob.getWorld().isClient) {
            ALL_MOBS.put(mob.getUuid(), new WeakReference<>(mob));

            if (VoiceConfig.DEBUG.get()) {
                Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
                System.out.println("[EZVCSurvival] Registered mob: " +
                        (id != null ? id.toString() : "unknown") + " (UUID: " + mob.getUuid() + ")");
            }
        }
    }

    public static void unregisterMob(MobEntity mob) {
        if (mob != null) {
            ALL_MOBS.remove(mob.getUuid());

            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival] Unregistered mob: " + mob.getUuid());
            }
        }
    }

    public static int refreshAllExistingMobs(MinecraftServer server) {
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] Refreshing goals for ALL existing mobs...");
        }

        int updatedCount = 0;
        int errorCount = 0;
        int totalMobsInWorld = 0;

        // Clean up stale references first
        cleanupStaleReferences();

        // Method 1: Update mobs from our registry
        updatedCount += updateRegisteredMobs();

        // Method 2: Also scan all worlds for any mobs we might have missed
        for (ServerWorld world : server.getWorlds()) {
            int worldUpdated = updateMobsInWorld(world);
            updatedCount += worldUpdated;
            totalMobsInWorld += countMobsInWorld(world);
        }

        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] Updated " + updatedCount + " mobs (total in world: " + totalMobsInWorld + ")");
            System.out.println("[EZVCSurvival] Registry size: " + ALL_MOBS.size());
        }

        return updatedCount;
    }

    public static int refreshMobsByEntityId(MinecraftServer server, String entityId) {
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] Refreshing goals for entity: " + entityId);
        }

        int updatedCount = 0;

        // Update from registry
        updatedCount += updateRegisteredMobsByEntityId(entityId);

        // Also scan worlds
        for (ServerWorld world : server.getWorlds()) {
            updatedCount += updateMobsInWorldByEntityId(world, entityId);
        }

        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] Updated " + updatedCount + " " + entityId);
        }

        return updatedCount;
    }

    private static void cleanupStaleReferences() {
        ALL_MOBS.entrySet().removeIf(entry -> {
            WeakReference<MobEntity> ref = entry.getValue();
            if (ref == null || ref.get() == null) {
                return true;
            }
            MobEntity mob = ref.get();
            return mob.isRemoved() || mob.getWorld().isClient;
        });
    }

    private static int updateRegisteredMobs() {
        int updated = 0;
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, WeakReference<MobEntity>> entry : ALL_MOBS.entrySet()) {
            WeakReference<MobEntity> ref = entry.getValue();
            MobEntity mob = ref.get();

            if (mob != null && !mob.isRemoved() && !mob.getWorld().isClient) {
                try {
                    refreshMobGoals(mob);
                    updated++;

                    if (VoiceConfig.DEBUG.get() && updated % 10 == 0) {
                        Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
                        System.out.println("[EZVCSurvival] Updated registered mob: " +
                                (id != null ? id.toString() : "unknown"));
                    }
                } catch (Exception e) {
                    toRemove.add(entry.getKey());

                    if (VoiceConfig.DEBUG.get()) {
                        System.err.println("[EZVCSurvival] Error updating registered mob: " + e.getMessage());
                    }
                }
            } else {
                toRemove.add(entry.getKey());
            }
        }

        // Remove problematic entries
        for (UUID id : toRemove) {
            ALL_MOBS.remove(id);
        }

        return updated;
    }

    private static int updateRegisteredMobsByEntityId(String entityId) {
        int updated = 0;
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, WeakReference<MobEntity>> entry : ALL_MOBS.entrySet()) {
            WeakReference<MobEntity> ref = entry.getValue();
            MobEntity mob = ref.get();

            if (mob != null && !mob.isRemoved() && !mob.getWorld().isClient) {
                Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
                if (id != null && id.toString().equals(entityId)) {
                    try {
                        refreshMobGoals(mob);
                        updated++;
                    } catch (Exception e) {
                        toRemove.add(entry.getKey());

                        if (VoiceConfig.DEBUG.get()) {
                            System.err.println("[EZVCSurvival] Error updating " + entityId + ": " + e.getMessage());
                        }
                    }
                }
            } else {
                toRemove.add(entry.getKey());
            }
        }

        for (UUID id : toRemove) {
            ALL_MOBS.remove(id);
        }

        return updated;
    }

    private static int updateMobsInWorld(ServerWorld world) {
        int updated = 0;

        // Get all mobs in the world using the correct method for 1.21.x
        try {
            // Method 1: Use iterateEntities() - works reliably
            for (net.minecraft.entity.Entity entity : world.iterateEntities()) {
                if (entity instanceof MobEntity mob) {
                    if (!mob.isRemoved()) {
                        try {
                            refreshMobGoals(mob);
                            updated++;
                            registerMob(mob); // Register any mobs we find
                        } catch (Exception e) {
                            if (VoiceConfig.DEBUG.get()) {
                                System.err.println("[EZVCSurvival] Error updating world mob: " + e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error iterating world entities: " + e.getMessage());
            }
        }

        return updated;
    }

    private static int updateMobsInWorldByEntityId(ServerWorld world, String entityId) {
        int updated = 0;

        try {
            for (net.minecraft.entity.Entity entity : world.iterateEntities()) {
                if (entity instanceof MobEntity mob) {
                    Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
                    if (id != null && id.toString().equals(entityId)) {
                        if (!mob.isRemoved()) {
                            try {
                                refreshMobGoals(mob);
                                updated++;
                                registerMob(mob);
                            } catch (Exception e) {
                                if (VoiceConfig.DEBUG.get()) {
                                    System.err.println("[EZVCSurvival] Error updating " + entityId + ": " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error iterating world for " + entityId + ": " + e.getMessage());
            }
        }

        return updated;
    }

    private static int countMobsInWorld(ServerWorld world) {
        int count = 0;

        try {
            for (net.minecraft.entity.Entity entity : world.iterateEntities()) {
                if (entity instanceof MobEntity) {
                    count++;
                }
            }
        } catch (Exception e) {
            // Ignore counting errors
        }

        return count;
    }

    public static void refreshMobGoals(MobEntity mob) {
        if (mob == null || mob.isRemoved() || mob.getWorld().isClient) return;

        try {
            // Remove existing EZVC goals
            removeExistingGoals(mob);

            Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
            if (id == null) return;

            String mobId = id.toString();
            boolean isAnimal = mob instanceof AnimalEntity;

            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival] Refreshing goals for: " + mobId);
            }

            // Inject Entity Voice Goals
            if (EntityVoiceConfig.isEnabled()) {
                if (!isAnimal) {
                    injectFollowVoiceGoal(mob, mobId);
                } else {
                    injectRunawayVoiceGoal((AnimalEntity) mob, mobId);
                }
            }

            // Inject General Sound Goals
            if (GeneralSoundsConfig.isEnabled()) {
                injectGeneralSoundGoal(mob, mobId);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh mob goals: " + e.getMessage(), e);
        }
    }

    public static void removeExistingGoals(MobEntity mob) {
        if (mob == null) return;

        // Create a list of goals to remove
        List<Goal> goalsToRemove = getGoals((MobEntityAccessor) mob);

        // Remove them
        for (net.minecraft.entity.ai.goal.Goal goal : goalsToRemove) {
            MobEntityAccessor accessor = (MobEntityAccessor) mob;
            accessor.vs$getGoalSelector().remove(goal);
        }

        if (VoiceConfig.DEBUG.get() && !goalsToRemove.isEmpty()) {
            System.out.println("[EZVCSurvival] Removed " + goalsToRemove.size() + " old goals");
        }
    }

    private static @NotNull List<Goal> getGoals(MobEntityAccessor mob) {
        List<Goal> goalsToRemove = new ArrayList<>();

        // Find EZVC goals
        for (var prioritizedGoal : mob.vs$getGoalSelector().getGoals()) {
            Goal goal = prioritizedGoal.getGoal();
            if (goal instanceof FollowVoiceGoal ||
                    goal instanceof RunawayVoiceGoal ||
                    goal instanceof ReactToGeneralSoundGoal) {
                goalsToRemove.add(goal);
            }
        }
        return goalsToRemove;
    }

    private static void injectFollowVoiceGoal(MobEntity mob, String mobId) {
        try {
            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getMonster(mobId);
            if (cfg != null && cfg.enabled && cfg.speed > 0 && cfg.range > 0) {
                FollowVoiceGoal goal = new FollowVoiceGoal(mob, cfg.speed, (int) cfg.range, cfg.threshold, 10000);
                MobEntityAccessor accessor = (MobEntityAccessor) mob;
                accessor.vs$getGoalSelector().add(0, goal);

                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival]   Injected FollowVoiceGoal (speed: " + cfg.speed + ", range: " + cfg.range + ")");
                }
            } else if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival]   No valid FollowVoiceGoal config for: " + mobId);
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Failed to inject FollowVoiceGoal: " + e.getMessage());
            }
            throw e;
        }
    }

    private static void injectRunawayVoiceGoal(AnimalEntity animal, String mobId) {
        try {
            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getAnimal(mobId);
            if (cfg != null && cfg.enabled && cfg.speed > 0 && cfg.range > 0) {
                RunawayVoiceGoal goal = new RunawayVoiceGoal(animal, cfg.speed, (int) cfg.range, cfg.threshold);
                MobEntityAccessor accessor = (MobEntityAccessor) animal;
                accessor.vs$getGoalSelector().add(4, goal);

                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival]   Injected RunawayVoiceGoal (speed: " + cfg.speed + ", range: " + cfg.range + ")");
                }
            } else if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival]   No valid RunawayVoiceGoal config for: " + mobId);
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Failed to inject RunawayVoiceGoal: " + e.getMessage());
            }
            throw e;
        }
    }

    private static void injectGeneralSoundGoal(MobEntity mob, String mobId) {
        try {
            GeneralSoundsConfig.Reaction r = GeneralSoundsConfig.getMobReactions().get(mobId);
            if (r != null && r.enabled && r.speed > 0 && r.range > 0) {
                List<SoundGroupData> groups = SoundConfig.getEnabledSoundGroups();
                if (groups != null && !groups.isEmpty()) {
                    ReactToGeneralSoundGoal goal = new ReactToGeneralSoundGoal(mob, r.speed, (int) r.range, groups);
                    MobEntityAccessor accessor = (MobEntityAccessor) mob;
                    accessor.vs$getGoalSelector().add(2, goal);

                    if (VoiceConfig.DEBUG.get()) {
                        System.out.println("[EZVCSurvival]   Injected ReactToGeneralSoundGoal (speed: " + r.speed + ", range: " + r.range + ")");
                    }
                } else if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival]   No enabled sound groups for: " + mobId);
                }
            } else if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival]   No valid GeneralSoundGoal config for: " + mobId);
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Failed to inject ReactToGeneralSoundGoal: " + e.getMessage());
            }
            throw e;
        }
    }

    // Debug method to show registry status
    public static void debugRegistry() {
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] === Mob Registry Debug ===");
            System.out.println("[EZVCSurvival] Total registered mobs: " + ALL_MOBS.size());

            Map<String, Integer> counts = new HashMap<>();
            for (WeakReference<MobEntity> ref : ALL_MOBS.values()) {
                MobEntity mob = ref.get();
                if (mob != null) {
                    Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
                    String mobId = id != null ? id.toString() : "unknown";
                    counts.put(mobId, counts.getOrDefault(mobId, 0) + 1);
                }
            }

            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                System.out.println("[EZVCSurvival]   " + entry.getKey() + ": " + entry.getValue());
            }
            System.out.println("[EZVCSurvival] ===========================");
        }
    }
}