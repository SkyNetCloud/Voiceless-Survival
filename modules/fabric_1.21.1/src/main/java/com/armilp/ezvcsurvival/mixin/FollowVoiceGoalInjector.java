package com.armilp.ezvcsurvival.mixin;


import com.armilp.ezvcsurvival.FollowVoiceGoal;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Map;

import static net.minecraft.registry.Registries.ENTITY_TYPE;

@Mixin(MobEntity.class)
public class FollowVoiceGoalInjector {

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void onEntityJoinWorld(CallbackInfo info) {
        MobEntity mob = (MobEntity) (Object) this;
        onEntityJoinWorld(mob);
    }

    @Unique
    private static void onEntityJoinWorld(MobEntity mob) {

        Map<String, Map<String, Double>> configs = VoiceConfig.getMobVoiceConfigs();
        Identifier mobId = ENTITY_TYPE.getId(mob.getType());

        if (configs.containsKey(mobId.toString())) {

            Map<String, Double> config = configs.get(mobId.toString());
            double speed = config.getOrDefault("speed", 1.0);
            double range = config.getOrDefault("range", 16.0);
            double threshold = config.getOrDefault("threshold", -40.0);

            // Add the goal using reflection to access the protected goalSelector field
            try {
                Field goalSelectorField = MobEntity.class.getDeclaredField("goalSelector");
                goalSelectorField.setAccessible(true);
                net.minecraft.entity.ai.goal.GoalSelector goalSelector = (net.minecraft.entity.ai.goal.GoalSelector) goalSelectorField.get(mob);
                goalSelector.add(1, new FollowVoiceGoal(mob, speed, (int) range,threshold));
            } catch (Exception ignored) {
                // Log error if needed
            }
        }
    }
}
