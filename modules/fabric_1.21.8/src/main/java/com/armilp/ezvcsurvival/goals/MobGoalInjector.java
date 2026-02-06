package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.mixins.MobEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.*;

public class MobGoalInjector {

    private static final String TAG = "ezvcsurvival:goals_added";
    private static final Set<MobEntity> TRACKED_MOBS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<UUID, InjectedGoals> INJECTED_GOALS = new WeakHashMap<>();

    public static void onEntityJoin(Entity entity, ServerWorld world) {
        if (!(entity instanceof MobEntity mob)) return;
        if (mob.isRemoved()) return;

        try {
            if (!hasActiveGoals(mob)) {
                injectGoals(mob);

//                CompoundTag data = mob.getPersistentData();
//                data.putBoolean(TAG, true);
                TRACKED_MOBS.add(mob);

                if (VoiceConfig.DEBUG.get()) {
                    Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
                    System.out.println(
                            "[EZVCSurvival] Injected goals into: " +
                                    (id != null ? id : "unknown") +
                                    " (UUID=" + mob.getUuid() + ")"
                    );
                }
            } else {
                TRACKED_MOBS.add(mob);

                if (VoiceConfig.DEBUG.get()) {
                    Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
                    System.out.println(
                            "[EZVCSurvival] Goals already present for: " +
                                    (id != null ? id : "unknown") +
                                    " (UUID=" + mob.getUuid() + ")"
                    );
                }
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error injecting goals on join: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static boolean hasActiveGoals( MobEntity mob) {
        InjectedGoals injected = MobGoalInjector.INJECTED_GOALS.get(mob.getUuid());
        if (injected != null && injected.hasAnyGoal()) {
            return verifyGoalsInSelector(mob, injected);
        }
        return false;
    }

    private static boolean verifyGoalsInSelector(MobEntity mob, InjectedGoals injected) {
        Set<PrioritizedGoal> availableGoals = acc(mob).vs$getGoalSelector().getGoals();

        boolean hasFollowGoal = injected.followGoal == null ||
                availableGoals.stream().anyMatch(wg -> wg.getGoal() == injected.followGoal);
        boolean hasRunawayGoal = injected.runawayGoal == null ||
                availableGoals.stream().anyMatch(wg -> wg.getGoal() == injected.runawayGoal);
        boolean hasGeneralSoundGoal = injected.generalSoundGoal == null ||
                availableGoals.stream().anyMatch(wg -> wg.getGoal() == injected.generalSoundGoal);

        return hasFollowGoal && hasRunawayGoal && hasGeneralSoundGoal;
    }

    public static void refreshAll() {
        Iterator<MobEntity> it = TRACKED_MOBS.iterator();
        while (it.hasNext()) {
            MobEntity mob = it.next();
            if (mob == null || mob.isRemoved()) {
                it.remove();
                continue;
            }
            refreshMob(mob);
        }
    }

    public static void refreshEntityId(String entityId) {
        for (MobEntity mob : new ArrayList<>(TRACKED_MOBS)) {
            if (mob == null || mob.isRemoved()) continue;
            Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
            if (id != null && id.toString().equals(entityId)) {
                refreshMob(mob);
            }
        }
    }

    private static void refreshMob(MobEntity mob) {
        if (mob == null || mob.isRemoved()) return;
        try {
            cleanupOldGoals(mob);
            injectGoals(mob);

            if (VoiceConfig.DEBUG.get()) {
                Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
                System.out.println("[EZVCSurvival] Refreshed goals for: " + (id != null ? id : "unknown") + " (UUID=" + mob.getUuid() + ")");
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error refreshing goals: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void injectGoals(MobEntity mob) {

        if (mob == null || mob.isRemoved()) return;

        Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
        if (id == null) return;
        String mobId = id.toString();

        InjectedGoals injectedGoals = new InjectedGoals();
        boolean isAnimal = mob instanceof AnimalEntity;

        // Voice goals
        if (EntityVoiceConfig.isEnabled()) {
            if (!isAnimal) {
                injectedGoals.followGoal = injectFollowVoiceGoal(mob, mobId);
            } else {
                injectedGoals.runawayGoal = injectRunawayVoiceGoal((AnimalEntity) mob, mobId);
            }
        }

        // General sound reaction
        if (GeneralSoundsConfig.isEnabled()) {
            injectedGoals.generalSoundGoal = injectGeneralSoundGoal(mob, mobId);
        }

        INJECTED_GOALS.put(mob.getUuid(), injectedGoals);
    }

    private static Goal injectFollowVoiceGoal(MobEntity mob, String mobId) {
        try {
            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getMonster(mobId);
            if (cfg != null && cfg.enabled && cfg.speed > 0 && cfg.range > 0) {
                Goal goal = new FollowVoiceGoal(mob, cfg.speed, (int) cfg.range, cfg.threshold, 10000);
                acc(mob).vs$getGoalSelector().add(0, goal);
                return goal;
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Failed to inject FollowVoiceGoal for " + mobId + ": " + e.getMessage());
            }
        }
        return null;
    }

    private static Goal injectRunawayVoiceGoal(AnimalEntity animal, String mobId) {
        try {
            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getAnimal(mobId);
            if (cfg != null && cfg.enabled && cfg.speed > 0 && cfg.range > 0) {
                Goal goal = new RunawayVoiceGoal(animal, cfg.speed, (int) cfg.range, cfg.threshold);
                acc(animal).vs$getGoalSelector().add(4, goal);
                return goal;
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Failed to inject RunawayVoiceGoal for " + mobId + ": " + e.getMessage());
            }
        }
        return null;
    }

    private static Goal injectGeneralSoundGoal(MobEntity mob, String mobId) {
        try {
            GeneralSoundsConfig.Reaction r = GeneralSoundsConfig.getMobReactions().get(mobId);
            if (r != null && r.enabled && r.speed > 0 && r.range > 0) {
                List<SoundGroupData> groups = SoundConfig.getEnabledSoundGroups();
                if (!groups.isEmpty()) {
                    Goal goal = new ReactToGeneralSoundGoal(mob, r.speed, (int) r.range, groups);
                    acc(mob).vs$getGoalSelector().add(2, goal);
                    return goal;
                }
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Failed to inject ReactToGeneralSoundGoal for " + mobId + ": " + e.getMessage());
            }
        }
        return null;
    }

    private static void cleanupOldGoals(MobEntity mob) {
        try {
            InjectedGoals injected = INJECTED_GOALS.get(mob.getUuid());
            if (injected != null) {
                removeGoal(mob, injected.followGoal);
                removeGoal(mob, injected.runawayGoal);
                removeGoal(mob, injected.generalSoundGoal);
                INJECTED_GOALS.remove(mob.getUuid());
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error removing old goals: " + e.getMessage());
            }
        }
    }

    private static void removeGoal(MobEntity mob, Goal goal) {
        if (goal != null) {
            acc(mob).vs$getGoalSelector().remove(goal);
        }
    }

    private static class InjectedGoals {
        Goal followGoal;
        Goal runawayGoal;
        Goal generalSoundGoal;

        boolean hasAnyGoal() {
            return followGoal != null || runawayGoal != null || generalSoundGoal != null;
        }
    }

    private static MobEntityAccessor acc(MobEntity mob) {
        return (MobEntityAccessor) mob;
    }
}