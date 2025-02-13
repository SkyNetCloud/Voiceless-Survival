package com.armilp.ezvcsurvival.data;

import net.minecraft.util.math.Vec3d;
import com.tacz.guns.api.item.GunTabType;

public record GunshotData(Vec3d position, long timestamp, GunTabType gunType) {
}