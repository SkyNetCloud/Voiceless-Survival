package com.armilp.ezvcsurvival.goals.injector;

import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.RunawayVoiceGoal;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.util.Map;

import static net.minecraft.registry.Registries.ENTITY_TYPE;

public class RunAwayVoiceGoalInjector {

        public static void init() {
            registerEntityLoadListener();
        }

        private static void registerEntityLoadListener() {
            ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
                if (entity instanceof AnimalEntity animal) {
                    addRunwayVoiceGoal(animal);
                }
            });
        }

        private static void addRunwayVoiceGoal(AnimalEntity animal) {
            Map<String, Map<String, Double>> configs = VoiceConfig.getAnimalVoiceConfigs();
            Identifier animalId = ENTITY_TYPE.getId(animal.getType());

            if (configs.containsKey(animalId.toString())) {
                Map<String, Double> config = configs.get(animalId.toString());
                double speed = config.getOrDefault("speed", 1.0);
                double range = config.getOrDefault("range", 16.0);
                double threshold = config.getOrDefault("threshold", -40.0);

                try{
                        Field goalSelectorField = MobEntity.class.getDeclaredField("goalSelector");
                        goalSelectorField.setAccessible(true);
                        GoalSelector goalSelector = (GoalSelector) goalSelectorField.get(animal);
                        goalSelector.add(1, new RunawayVoiceGoal(animal, speed, (int) range,threshold));
                        //EZVCSurvival.LOGGER.info("Goal is being Added");
                } catch (Exception ignored){

                }
            }
        }
}
