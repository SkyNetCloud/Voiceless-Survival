package com.armilp.ezvcsurvival.data;

import net.minecraft.util.math.Vec3d;

public class GunshotData {
    public final Vec3d position;
    public final long timestamp;

    public GunshotData(Vec3d position, long timestamp, GunTabType gunType) {
        this.position = position;
        this.timestamp = timestamp;
    }
}