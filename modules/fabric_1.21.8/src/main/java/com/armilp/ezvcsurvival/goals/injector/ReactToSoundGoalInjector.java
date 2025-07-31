package com.armilp.ezvcsurvival.goals.injector;

import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.goals.ReactToSoundGoal;
import com.armilp.ezvcsurvival.utils.InjectorLogger;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.util.List;

import static net.minecraft.registry.Registries.ENTITY_TYPE;

public class ReactToSoundGoalInjector {

    public static void init() {
        registerEntityLoadListener();
    }

    private static void registerEntityLoadListener() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof MobEntity mob) {
                addReactToSoundGoal(mob);
            }
        });
    }

    private static void addReactToSoundGoal(MobEntity mob) {
        Identifier mobId = ENTITY_TYPE.getId(mob.getType());
        String mobIdStr = mobId.toString();

        SoundConfig.MobReaction config = SoundConfig.getMobSoundReaction(mobIdStr);
        if (config == null || config.groups == null || config.groups.isEmpty()) return;

        List<SoundGroupData> soundGroups = SoundConfig.getSoundGroupsForMob(mobIdStr);
        if (soundGroups.isEmpty()) return;


        try {
            Field goalSelectorField = MobEntity.class.getDeclaredField("goalSelector");
            goalSelectorField.setAccessible(true);
            GoalSelector goalSelector = (GoalSelector) goalSelectorField.get(mob);

            goalSelector.add(3, new ReactToSoundGoal(mob, config.speed, (int) config.range, soundGroups));

            InjectorLogger.logInfo(ReactToSoundGoalInjector.class,
                    "Added ReactToSoundGoal to " + mobIdStr);
        } catch (Exception e) {
            InjectorLogger.logError(ReactToSoundGoalInjector.class,
                    "Failed to add ReactToSoundGoal for " + mobIdStr, e);
        }
    }
}
