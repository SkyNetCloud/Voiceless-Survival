package com.armilp.ezvcsurvival.goals.injector;

import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.goals.ReactToSoundGoal;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static net.minecraft.util.registry.Registry.ENTITY_TYPE;

public class ReactToSoundGoalInjector {

    public static void init() {
        registerEntityLoadListener();
    }

    private static void registerEntityLoadListener() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof MobEntity mob) {
                addReactToSoundGoal(mob, world);
            }
        });
    }

    private static void addReactToSoundGoal(MobEntity mob, ServerWorld world) {

        Identifier modIdRL = ENTITY_TYPE.getId(mob.getType());
        String modId = modIdRL.toString();



        Map<String, Object> config = SoundConfig.getMobSoundReaction(modId);
        if (config != null){
            double speed = config.get("speed") instanceof Number
                    ? ((Number) config.get("speed")).doubleValue()
                    : 1.0;
            double rangeDouble = config.get("range") instanceof Number
                    ? ((Number) config.get("range")).doubleValue()
                    : 16.0;
            int range = (int) rangeDouble;
            List<?> groups = (List<?>) config.get("groups");
            if (groups != null && !groups.isEmpty()){
                var soundGroups = SoundConfig.getSoundGroupsForMob(modId);
                if (!soundGroups.isEmpty()){
                    try{
                        Field goalSelectorField = MobEntity.class.getDeclaredField("goalSelector");
                        goalSelectorField.setAccessible(true);
                        GoalSelector goalSelector = (GoalSelector) goalSelectorField.get(mob);
                        goalSelector.add(3, new ReactToSoundGoal(mob, speed, range, soundGroups));
                    } catch (Exception ignore){

                    }
                }
            }
        }

    }
}
