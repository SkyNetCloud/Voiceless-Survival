package com.armilp.ezvcsurvival.goals.injector;

import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.RunawayVoiceGoal;
import com.armilp.ezvcsurvival.utils.InjectorLogger;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.world.ServerWorld;
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
                addRunawayVoiceGoal(animal, world);
            }
        });
    }

    private static void addRunawayVoiceGoal(AnimalEntity animal, ServerWorld world) {
        Identifier animalId = ENTITY_TYPE.getId(animal.getType());
        Map<String, VoiceConfig.VoiceAttributes> configs = VoiceConfig.getAnimalVoiceConfigs();

        VoiceConfig.VoiceAttributes config = configs.get(animalId.toString());
        if (config == null) return;

        boolean noSurvivalPlayers = world.getPlayers(p -> p.interactionManager.getGameMode().isSurvivalLike()).isEmpty();
        if (!noSurvivalPlayers) return;

        try {
            Field goalSelectorField = MobEntity.class.getDeclaredField("goalSelector");
            goalSelectorField.setAccessible(true);
            GoalSelector goalSelector = (GoalSelector) goalSelectorField.get(animal);

            goalSelector.add(1, new RunawayVoiceGoal(animal, config.speed, (int) config.range, config.threshold));

            InjectorLogger.logInfo(RunAwayVoiceGoalInjector.class,
                    "Added RunawayVoiceGoal to " + animalId);
        } catch (Exception e) {
            InjectorLogger.logError(RunAwayVoiceGoalInjector.class,
                    "Failed to inject RunawayVoiceGoal for " + animalId, e);
        }
    }
}
