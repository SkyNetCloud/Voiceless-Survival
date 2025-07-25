package com.armilp.ezvcsurvival.data;

import net.minecraft.util.math.Vec3d;

public class TimedSoundData {
    public final Vec3d position;
    public final long timestamp;

    public TimedSoundData(Vec3d position, long timestamp) {
        this.position = position;
        this.timestamp = timestamp;
    }
}