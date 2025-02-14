package com.armilp.ezvcsurvival.data;

import com.tacz.guns.api.item.GunTabType;
import net.minecraft.util.math.Vec3d;

public record GunshotData(Vec3d position, long timestamp, GunTabType gunType) {
}