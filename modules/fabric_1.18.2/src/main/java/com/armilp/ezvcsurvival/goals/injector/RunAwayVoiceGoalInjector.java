package com.armilp.ezvcsurvival.goals.injector;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.RunawayVoiceGoal;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.util.Map;

import static net.minecraft.util.registry.Registry.ENTITY_TYPE;

public class RunAwayVoiceGoalInjector {

        public static void init() {
            registerEntityLoadListener();
        }

        private static void registerEntityLoadListener() {
            ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
                if (entity instanceof AnimalEntity animal) {
                    addRunwayVoiceGoal(animal,world);
                }
            });
        }

        private static void addRunwayVoiceGoal(AnimalEntity animal, ServerWorld world) {
            Map<String, Map<String, Double>> configs = VoiceConfig.getAnimalVoiceConfigs();
            Identifier animalId = ENTITY_TYPE.getId(animal.getType());

            if (configs.containsKey(animalId.toString())) {
                Map<String, Double> config = configs.get(animalId.toString());
                double speed = config.getOrDefault("speed", 1.0);
                double range = config.getOrDefault("range", 16.0);
                double threshold = config.getOrDefault("threshold", -40.0);
                if (world.getPlayers(player -> player.interactionManager.getGameMode().isSurvivalLike()).isEmpty()) {
                    try{
                        Field goalSelectorField = MobEntity.class.getDeclaredField("goalSelector");
                        goalSelectorField.setAccessible(true);
                        GoalSelector goalSelector = (GoalSelector) goalSelectorField.get(animal);
                        goalSelector.add(1, new RunawayVoiceGoal(animal, speed, (int) range,threshold));
                        EZVCSurvival.LOGGER.info("Goal is being Added");
                    } catch (Exception ignored){

                    }
                }
            }
        }
}
