package com.armilp.ezvcsurvival.mixins;

import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.FollowVoiceGoal;
import com.armilp.ezvcsurvival.goals.ReactToSoundGoal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(MobEntity.class)
public class MobEntityMixinGoals {

    @Final
    @Shadow
    protected GoalSelector goalSelector;

    @Inject(method = "initGoals", at = @At("RETURN"))
    private void addAllGoalsMixin(CallbackInfo ci) {
        MobEntity mob = (MobEntity) (Object) this;
        World world = mob.getWorld();
        if (!(world instanceof ServerWorld serverWorld)) return;

        addFollowVoiceGoal(mob, serverWorld);
        addReactToSoundGoal(mob, serverWorld);
    }


    @Unique
    private void addFollowVoiceGoal(MobEntity mob, ServerWorld world) {
        Map<String, Map<String, Double>> configs = VoiceConfig.getMobVoiceConfigs();
        Identifier mobId = Registries.ENTITY_TYPE.getId(mob.getType());
        String modId = mobId.toString();

        if (!configs.containsKey(modId)) return;

        Map<String, Double> config = configs.get(modId);
        double speed = config.getOrDefault("speed", 1.0);
        double range = config.getOrDefault("range", 16.0);
        double threshold = config.getOrDefault("threshold", -40.0);

        if (world.getPlayers(player -> player.interactionManager.getGameMode().isSurvivalLike()).isEmpty()) {
            goalSelector.add(1, new FollowVoiceGoal(mob, speed, (int) range, threshold, 10000));
        }
    }

    @Unique
    private void addReactToSoundGoal(MobEntity mob, ServerWorld world) {
        Identifier mobId = Registries.ENTITY_TYPE.getId(mob.getType());
        String modId = mobId.toString();

        Map<String, Object> config = SoundConfig.getMobSoundReaction(modId);
        if (config == null) return;

        double speed = config.get("speed") instanceof Number
                ? ((Number) config.get("speed")).doubleValue()
                : 1.0;

        int range = config.get("range") instanceof Number
                ? ((Number) ((Number) config.get("range")).doubleValue()).intValue()
                : 16;

        List<?> groups = (List<?>) config.get("groups");
        if (groups == null || groups.isEmpty()) return;

        var soundGroups = SoundConfig.getSoundGroupsForMob(modId);
        if (soundGroups.isEmpty()) return;
        if (world.getPlayers(player -> player.interactionManager.getGameMode().isSurvivalLike()).isEmpty()) {
            goalSelector.add(3, new ReactToSoundGoal(mob, speed, range, soundGroups));
        }

    }


}
