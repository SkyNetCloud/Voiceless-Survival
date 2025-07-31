package com.armilp.ezvcsurvival.goals.injector;

import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.FollowVoiceGoal;
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
        Map<String, VoiceConfig.VoiceAttributes> configs = VoiceConfig.getMobVoiceConfigs();
        Identifier mobId = ENTITY_TYPE.getId(mob.getType());

        if (configs.containsKey(mobId.toString())) {
            VoiceConfig.VoiceAttributes config = configs.get(mobId.toString());
            double speed = config.speed;
            double range = config.range;
            double threshold = config.threshold;

            if (world.getPlayers(player -> player.interactionManager.getGameMode().isSurvivalLike()).isEmpty()) {
                try {
                    Field goalSelectorField = MobEntity.class.getDeclaredField("goalSelector");
                    goalSelectorField.setAccessible(true);
                    GoalSelector goalSelector = (GoalSelector) goalSelectorField.get(mob);
                    goalSelector.add(1, new FollowVoiceGoal(mob, speed, (int) range, threshold, 10000));
                } catch (Exception e) {
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                }
            }
        }
    }
}