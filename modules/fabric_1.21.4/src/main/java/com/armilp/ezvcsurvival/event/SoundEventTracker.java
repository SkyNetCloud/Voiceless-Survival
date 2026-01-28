package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.data.TimedSoundData;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SoundEventTracker {
    private static final long SOUND_EXPIRATION_MS = 3000;
    private static final Map<Identifier, TimedSoundData> lastPlayedPositions = new ConcurrentHashMap<>();

    public static void registerSound(Identifier soundLocation, Vec3d position) {
        long now = System.currentTimeMillis();
        lastPlayedPositions.put(soundLocation, new TimedSoundData(position, now));
    }


    public static Vec3d getLastPlayedPositionForSound(Identifier soundLocation) {
        long now = System.currentTimeMillis();
        TimedSoundData data = lastPlayedPositions.get(soundLocation);
        if (data != null && (now - data.timestamp <= SOUND_EXPIRATION_MS)) {
            return data.position;
        }
        return null;
    }
}
