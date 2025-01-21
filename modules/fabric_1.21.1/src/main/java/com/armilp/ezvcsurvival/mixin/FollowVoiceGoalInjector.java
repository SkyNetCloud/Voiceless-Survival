package com.armilp.ezvcsurvival.mixin;

import com.armilp.ezvcsurvival.FollowVoiceGoal;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

import static net.minecraft.registry.Registries.ENTITY_TYPE;

@Mixin(MobEntity.class)
public class FollowVoiceGoalInjector {

    @Final
    @Shadow
    protected GoalSelector goalSelector;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initGoals(CallbackInfo ci) {
        MobEntity mob = (MobEntity) (Object) this;
        Map<String, Map<String, Double>> configs = VoiceConfig.getMobVoiceConfigs();
        Identifier mobId = ENTITY_TYPE.getId(mob.getType());

        if (configs.containsKey(mobId.toString())) {
            Map<String, Double> config = configs.get(mobId.toString());
            double speed = config.getOrDefault("speed", 1.0);
            double range = config.getOrDefault("range", 16.0);
            double threshold = config.getOrDefault("threshold", -40.0);

            this.goalSelector.add(1, new FollowVoiceGoal(mob, speed, (int) range, threshold));
        }
    }
}