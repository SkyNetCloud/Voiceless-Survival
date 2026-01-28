package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.data.TimedSoundData;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import com.armilp.ezvcsurvival.mixin.MobEntityAccessor;

import de.maxhenkel.voicechat.api.ServerLevel;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SoundEventTracker {
    private static final long SOUND_EXPIRATION_MS = 3000;
    private static final Map<Identifier, TimedSoundData> lastPlayedPositions = new ConcurrentHashMap<>();

    public static void setLastPlayedPosition(Identifier sound, double x, double y, double z, double speedMultiplier, double rangeMultiplier) {
        lastPlayedPositions.put(sound, new TimedSoundData(new Vec3d(x, y, z), System.currentTimeMillis(), speedMultiplier, rangeMultiplier));
    }

    public static void notifyNearbyMobs(ServerWorld level, Identifier sound, double x, double y, double z, double speedMultiplier, double rangeMultiplier) {
        Vec3d soundPos = new Vec3d(x, y, z);


        Box searchBox = Box.from(soundPos).expand(rangeMultiplier * 16.0); // Assuming rangeMultiplier affects hearing distance
        List<MobEntity> nearbyMobs = level.getEntitiesByClass(
                MobEntity.class,
                searchBox,
                mob -> true // Include all mobs in the box
        );


        for (var entity : nearbyMobs) {
            if (!(entity instanceof MobEntity mob)) continue;
            if (mob.getTarget() != null) continue;
            MobEntityAccessor mobEntityAccessor = (MobEntityAccessor) mob;
            for (PrioritizedGoal prioritizedGoal : mobEntityAccessor.vs$getGoalSelector().getGoals()) {
                if (prioritizedGoal.getGoal() instanceof ReactToGeneralSoundGoal) {
                    ((ReactToGeneralSoundGoal) prioritizedGoal.getGoal()).onSoundPlayed(sound, soundPos, speedMultiplier, rangeMultiplier);
                    break;
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
