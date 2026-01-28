package com.armilp.ezvcsurvival.mixin;

import com.armilp.ezvcsurvival.config.*;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.goals.FollowVoiceGoal;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import com.armilp.ezvcsurvival.goals.RunawayVoiceGoal;
import com.armilp.ezvcsurvival.utils.MobGoalUpdater;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {

    @Unique
    private boolean ezvc$hasGoalsInjected = false;

    @Unique
    private final List<Goal> ezvc$injectedGoals = new ArrayList<>();

    // Helper method to get the goal selector using the accessor
    @Unique
    private GoalSelector ezvc$getGoalSelector(MobEntity mob) {
        return ((MobEntityAccessor) mob).vs$getGoalSelector();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ezvc$onMobEntityInit(EntityType<? extends MobEntity> entityType, World world, CallbackInfo ci) {
        if (world.isClient) return;

        MobEntity mob = (MobEntity) (Object) this;

        if (VoiceConfig.DEBUG.get()) {
            Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
            System.out.println("[EZVCSurvival] Mob created: " + (id != null ? id.toString() : "unknown"));
        }

        // Register this mob for future updates
        MobGoalUpdater.registerMob(mob);

        // Schedule goal injection for next tick to ensure entity is fully initialized
        world.getServer().execute(() -> {
            if (!mob.isRemoved()) {
                ezvc$injectGoals(mob, true);
            }
        });
    }

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void ezvc$onInitGoals(CallbackInfo ci) {
        MobEntity mob = (MobEntity) (Object) this;
        World world = mob.getWorld();

        if (world.isClient) return;

        // Only inject if not already injected (prevents double injection)
        if (!ezvc$hasGoalsInjected) {
            ezvc$injectGoals(mob, false);
        }
    }

    @Inject(method = "removeFromDimension", at = @At("HEAD"))
    private void ezvc$onRemove(CallbackInfo ci) {
        MobEntity mob = (MobEntity) (Object) this;

        // Clean up when mob is removed
        MobGoalUpdater.unregisterMob(mob);
        ezvc$cleanupGoals(mob);
        ezvc$hasGoalsInjected = false;
        ezvc$injectedGoals.clear();
    }

    @Unique
    private void ezvc$injectGoals(MobEntity mob, boolean isDelayed) {
        if (mob == null || mob.isRemoved() || mob.getWorld().isClient) return;

        // Skip if already injected
        if (ezvc$hasGoalsInjected) {
            if (VoiceConfig.DEBUG.get() && isDelayed) {
                System.out.println("[EZVCSurvival] Goals already injected, skipping delayed injection");
            }
            return;
        }

        Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
        if (id == null) {
            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival] Cannot get entity identifier");
            }
            return;
        }

        String mobId = id.toString();
        boolean isAnimal = mob instanceof AnimalEntity;

        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] Injecting goals for: " + mobId +
                    " (isAnimal: " + isAnimal + ", delayed: " + isDelayed + ")");
        }

        // Clear any existing EZVC goals first (just in case)
        ezvc$cleanupGoals(mob);

        boolean injectedAnyGoal = false;

        // Inject Entity Voice Goals
        if (EntityVoiceConfig.isEnabled()) {
            if (!isAnimal) {
                if (ezvc$injectFollowVoiceGoal(mob, mobId)) {
                    injectedAnyGoal = true;
                }
            } else {
                if (ezvc$injectRunawayVoiceGoal((AnimalEntity) mob, mobId)) {
                    injectedAnyGoal = true;
                }
            }
        }

        // Inject General Sound Goals
        if (GeneralSoundsConfig.isEnabled()) {
            if (ezvc$injectGeneralSoundGoal(mob, mobId)) {
                injectedAnyGoal = true;
            }
        }

        if (injectedAnyGoal) {
            ezvc$hasGoalsInjected = true;

            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival] Successfully injected goals for: " + mobId);
            }
        } else {
            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival] No goals injected for: " + mobId +
                        " (config disabled or not applicable)");
            }
        }
    }

    @Unique
    private boolean ezvc$injectFollowVoiceGoal(MobEntity mob, String mobId) {
        try {
            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getMonster(mobId);

            if (cfg == null) {
                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival]   No FollowVoiceGoal config for: " + mobId);
                }
                return false;
            }

            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival]   FollowVoiceGoal config - enabled: " + cfg.enabled +
                        ", speed: " + cfg.speed + ", range: " + cfg.range);
            }

            if (cfg.enabled && cfg.speed > 0 && cfg.range > 0) {
                FollowVoiceGoal goal = new FollowVoiceGoal(mob, cfg.speed, (int) cfg.range, cfg.threshold, 10000);
                GoalSelector goalSelector = ezvc$getGoalSelector(mob);
                goalSelector.add(0, goal);
                ezvc$injectedGoals.add(goal);

                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival]   Injected FollowVoiceGoal with priority 0");
                }
                return true;
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival]   ERROR injecting FollowVoiceGoal for " + mobId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }

    @Unique
    private boolean ezvc$injectRunawayVoiceGoal(AnimalEntity animal, String mobId) {
        try {
            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getAnimal(mobId);

            if (cfg == null) {
                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival]   No RunawayVoiceGoal config for: " + mobId);
                }
                return false;
            }

            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival]   RunawayVoiceGoal config - enabled: " + cfg.enabled +
                        ", speed: " + cfg.speed + ", range: " + cfg.range);
            }

            if (cfg.enabled && cfg.speed > 0 && cfg.range > 0) {
                RunawayVoiceGoal goal = new RunawayVoiceGoal(animal, cfg.speed, (int) cfg.range, cfg.threshold);
                GoalSelector goalSelector = ezvc$getGoalSelector(animal);
                goalSelector.add(4, goal);
                ezvc$injectedGoals.add(goal);

                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival]   Injected RunawayVoiceGoal with priority 4");
                }
                return true;
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival]   ERROR injecting RunawayVoiceGoal for " + mobId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }

    @Unique
    private boolean ezvc$injectGeneralSoundGoal(MobEntity mob, String mobId) {
        try {
            GeneralSoundsConfig.Reaction r = GeneralSoundsConfig.getMobReactions().get(mobId);

            if (r == null) {
                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival]   No GeneralSoundGoal config for: " + mobId);
                }
                return false;
            }

            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival]   GeneralSoundGoal config - enabled: " + r.enabled +
                        ", speed: " + r.speed + ", range: " + r.range);
            }

            if (r.enabled && r.speed > 0 && r.range > 0) {
                List<SoundGroupData> groups = SoundConfig.getEnabledSoundGroups();

                if (groups != null && !groups.isEmpty()) {
                    ReactToGeneralSoundGoal goal = new ReactToGeneralSoundGoal(mob, r.speed, (int) r.range, groups);
                    GoalSelector goalSelector = ezvc$getGoalSelector(mob);
                    goalSelector.add(2, goal);
                    ezvc$injectedGoals.add(goal);

                    if (VoiceConfig.DEBUG.get()) {
                        System.out.println("[EZVCSurvival]   Injected ReactToGeneralSoundGoal with priority 2");
                    }
                    return true;
                } else {
                    if (VoiceConfig.DEBUG.get()) {
                        System.out.println("[EZVCSurvival]   No enabled sound groups available");
                    }
                }
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival]   ERROR injecting ReactToGeneralSoundGoal for " + mobId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }

    @Unique
    private void ezvc$cleanupGoals(MobEntity mob) {
        if (mob == null || mob.isRemoved()) return;

        // Remove goals from goal selector
        for (Goal goal : ezvc$injectedGoals) {
            try {
                GoalSelector goalSelector = ezvc$getGoalSelector(mob);
                goalSelector.remove(goal);
            } catch (Exception e) {
                if (VoiceConfig.DEBUG.get()) {
                    System.err.println("[EZVCSurvival] Error removing goal: " + e.getMessage());
                }
            }
        }

        ezvc$injectedGoals.clear();

        if (VoiceConfig.DEBUG.get() && ezvc$hasGoalsInjected) {
            System.out.println("[EZVCSurvival] Cleaned up goals for mob");
        }
    }

    @Unique
    public void ezvc$refreshGoals() {
        MobEntity mob = (MobEntity) (Object) this;

        if (mob == null || mob.isRemoved() || mob.getWorld().isClient) return;

        if (VoiceConfig.DEBUG.get()) {
            Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
            System.out.println("[EZVCSurvival] Refreshing goals for: " +
                    (id != null ? id.toString() : "unknown"));
        }

        // Reset injection flag to allow re-injection
        ezvc$hasGoalsInjected = false;

        // Clean up old goals
        ezvc$cleanupGoals(mob);

        // Re-inject with current config
        ezvc$injectGoals(mob, false);
    }
}