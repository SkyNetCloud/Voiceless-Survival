package com.armilp.ezvcsurvival;

import com.armilp.ezvcsurvival.config.VoiceConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.util.Map;

import static net.minecraft.registry.Registries.ENTITY_TYPE;

public class FollowVoiceGoalInjector {
    public static void init() {
        registerEntityLoadListener();
    }

    private static void registerEntityLoadListener() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof MobEntity mob) {
                addFollowVoiceGoal(mob, world);
            }
        });
    }

    private static void addFollowVoiceGoal(MobEntity mob, ServerWorld world) {
        Map<String, Map<String, Double>> configs = VoiceConfig.getMobVoiceConfigs();
        Identifier mobId = ENTITY_TYPE.getId(mob.getType());

        if (configs.containsKey(mobId.toString())) {
            Map<String, Double> config = configs.get(mobId.toString());
            double speed = config.getOrDefault("speed", 1.0);
            double range = config.getOrDefault("range", 16.0);
            double threshold = config.getOrDefault("threshold", -40.0);
            if (world.getPlayers(player -> player.interactionManager.getGameMode().isSurvivalLike()).isEmpty()) {
                try{
                    Field goalSelectorField = MobEntity.class.getDeclaredField("goalSelector");
                    goalSelectorField.setAccessible(true);
                    GoalSelector goalSelector = (GoalSelector) goalSelectorField.get(mob);
                    goalSelector.add(1, new FollowVoiceGoal(mob, speed, (int) range,threshold));
                } catch (Exception ignored){

                }
            }
        }
    }
}