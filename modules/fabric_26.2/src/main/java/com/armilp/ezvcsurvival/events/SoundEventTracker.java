package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.data.TimedSoundData;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.armilp.ezvcsurvival.goals.MobGoalInjector.acc;


public class SoundEventTracker {
    private static final long SOUND_EXPIRATION_MS = 4000;
    private static final Map<Identifier, TimedSoundData> lastPlayedPositions = new ConcurrentHashMap<>();

    public static void setLastPlayedPosition(Identifier sound, double x, double y, double z, double speedMultiplier, double rangeMultiplier) {
        lastPlayedPositions.put(sound, new TimedSoundData(new Vec3d(x, y, z), System.currentTimeMillis(), speedMultiplier, rangeMultiplier));
    }

    public static void notifyNearbyMobs(ServerWorld level, Identifier sound, double x, double y, double z, double speedMultiplier, double rangeMultiplier) {
        Vec3d soundPos = new Vec3d(x, y, z);

        for (var entity : level.iterateEntities()) {
            if (!(entity instanceof MobEntity mob)) continue;
            if (mob.getTarget() != null) continue;

            for (PrioritizedGoal wrappedGoal : acc(mob).vs$getGoalSelector().getGoals()) {
                if (wrappedGoal.getGoal() instanceof ReactToGeneralSoundGoal) {
                    ((ReactToGeneralSoundGoal) wrappedGoal.getGoal()).onSoundPlayed(sound, soundPos, speedMultiplier, rangeMultiplier);
                }
            }
        }
    }

    public static Vec3d getLastPlayedPositionForSound(Identifier soundLocation) {
        TimedSoundData data = lastPlayedPositions.get(soundLocation);
        if (data != null && (System.currentTimeMillis() - data.timestamp() <= SOUND_EXPIRATION_MS)) {
            return data.position();
        }
        return null;
    }
}
