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
import net.minecraft.nbt.NbtCompound;
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

                NbtCompound nbt = new NbtCompound();
                nbt.putBoolean(TAG, true);
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
        NbtCompound data =  new NbtCompound();
        data.remove(TAG);
        removeOldGoals(mob);
        try {
            injectGoals(mob);
            data.putBoolean(TAG, true);
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error refreshing goals: " + e.getMessage());
            }
        }
    }

    private static void injectGoals(MobEntity mob) {
        Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
        if (id == null) return;

        String mobId = id.toString();
        InjectedGoals injected = new InjectedGoals();


        if (EntityVoiceConfig.isEnabled() && !(mob instanceof AnimalEntity)) {
            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getMonster(mobId);

            if (cfg != null && cfg.enabled && cfg.speed > 0 && cfg.range > 0) {
                FollowVoiceGoal goal =
                        new FollowVoiceGoal(mob, cfg.speed, (int) cfg.range, cfg.threshold, 10000);

                acc(mob).vs$getGoalSelector().add(0, goal);
                injected.followGoal = goal;
            }
        }


        if (EntityVoiceConfig.isEnabled() && mob instanceof AnimalEntity animal) {
            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getAnimal(mobId);

            if (cfg != null && cfg.enabled && cfg.speed > 0 && cfg.range > 0) {
                RunawayVoiceGoal goal =
                        new RunawayVoiceGoal(animal, cfg.speed, cfg.range, cfg.threshold);

                acc(animal).vs$getGoalSelector().add(4, goal);
                injected.runawayGoal = goal;
            }
        }

        if (GeneralSoundsConfig.isEnabled()) {
            GeneralSoundsConfig.Reaction r =
                    GeneralSoundsConfig.getMobReactions().get(mobId);

            if (r != null && r.enabled && r.speed > 0 && r.range > 0) {
                List<SoundGroupData> groups = SoundConfig.getEnabledSoundGroups();

                if (!groups.isEmpty()) {
                    ReactToGeneralSoundGoal goal =
                            new ReactToGeneralSoundGoal(mob, r.speed, r.range, groups);

                    acc(mob).vs$getGoalSelector().add(2, goal);
                    injected.generalSoundGoal = goal;
                }
            }
        }

        INJECTED_GOALS.put(mob.getUuid(), injected);
    }


    private static void removeOldGoals(MobEntity mob) {
        acc(mob).vs$getGoalSelector().getGoals().removeIf(w ->
                w.getGoal().getClass().getName().startsWith("com.armilp.ezvcsurvival.goals"));
    }

    private static class InjectedGoals {
        Goal followGoal;
        Goal runawayGoal;
        Goal generalSoundGoal;

        boolean hasAnyGoal() {
            return followGoal != null || runawayGoal != null || generalSoundGoal != null;
        }
    }

    public static MobEntityAccessor acc(MobEntity mob) {
        return (MobEntityAccessor) mob;
    }
}