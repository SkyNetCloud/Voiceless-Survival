package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;


import java.util.List;
import java.util.Objects;

public class ReactToGeneralSoundGoal extends Goal {
    private static final long PRIORITY_SOUND_DURATION_MS = 3500;
    private static final int NAVIGATION_UPDATE_INTERVAL = 15;
    private static final int LOOK_UPDATE_INTERVAL = 30;
    private static final long SOUND_REACTION_TIMEOUT = 4000;

    public static Vec3d lastPrioritySoundPos = null;
    public static long lastPrioritySoundTimestamp = 0;

    private final MobEntity mob;
    private final double speed;
    private final double range;
    private final List<SoundGroupData> soundGroups;
    private final String entityId;
    private final boolean isMonster;

    private Vec3d targetSoundPos = null;
    private double targetSpeedMultiplier = 1.0;
    private double targetRangeMultiplier = 1.0;
    private long targetSetTime = 0;
    private int tickCounter = 0;

    public ReactToGeneralSoundGoal(MobEntity mob, double speed, double range, List<SoundGroupData> soundGroups) {
        this.mob = mob;
        this.speed = speed;
        this.range = range;
        this.soundGroups = soundGroups;
        this.entityId = Objects.requireNonNull(Registries.ENTITY_TYPE.getKey(mob.getType())).toString();
        this.isMonster = mob instanceof Monster;
    }

    public void onSoundPlayed(Identifier soundLoc, Vec3d soundPos, double speedMult, double rangeMult) {
        if (mob.getTarget() != null) return;

        String soundId = soundLoc.toString();
        if (!GeneralSoundsConfig.canEntityReactToSound(entityId, soundId)) return;

        boolean isPriority = false;
        for (int i = 0, size = soundGroups.size(); i < size; i++) {
            SoundGroupData group = soundGroups.get(i);
            if (group.groupName().startsWith("auto_priority_")) {
                if (group.sounds().contains(soundId)) {
                    isPriority = true;
                    speedMult = group.speedMultiplier();
                    rangeMult = group.rangeMultiplier();
                    break;
                }
            }
        }

        if (!isPriority) {
            boolean found = false;
            for (int i = 0, size = soundGroups.size(); i < size; i++) {
                SoundGroupData group = soundGroups.get(i);
                if (group.sounds().contains(soundId)) {
                    speedMult = group.speedMultiplier();
                    rangeMult = group.rangeMultiplier();
                    found = true;
                    break;
                }
            }
            if (!found) return;
        }

        Vec3d mobPos = mob.getPos();
        double effectiveRange = range * rangeMult;
        if (mob.getWorld().isRaining() || mob.getWorld().isThundering()) {
            effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
        }

        double distSq = mobPos.squaredDistanceTo(soundPos);
        if (distSq > effectiveRange * effectiveRange) return;

        this.targetSoundPos = soundPos;
        this.targetSpeedMultiplier = speedMult;
        this.targetRangeMultiplier = rangeMult;
        this.targetSetTime = System.currentTimeMillis();

        if (isPriority && lastPrioritySoundPos == null) {
            setPrioritySound(soundPos);
        }
    }

    @Override
    public boolean canStart() {
        if (mob.getTarget() != null) return false;

        long now = System.currentTimeMillis();

        if (lastPrioritySoundPos != null && (now - lastPrioritySoundTimestamp) > PRIORITY_SOUND_DURATION_MS) {
            lastPrioritySoundPos = null;
        }

        if (lastPrioritySoundPos != null) {
            Vec3d mobPos = mob.getPos();
            double effectiveRange = range * 1.5;
            if (mob.getWorld().isRaining() || mob.getWorld().isThundering()) {
                effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
            }
            if (mobPos.squaredDistanceTo(lastPrioritySoundPos) <= effectiveRange * effectiveRange) {
                targetSoundPos = lastPrioritySoundPos;
                targetSpeedMultiplier = 1.5;
                targetRangeMultiplier = 1.5;
                return true;
            }
        }

        if (targetSoundPos != null && (now - targetSetTime) < SOUND_REACTION_TIMEOUT) {
            return true;
        }

        return false;
    }


    @Override
    public boolean shouldContinue() {
        if (mob.getTarget() != null) return false;

        long now = System.currentTimeMillis();
        if (targetSoundPos == null) return false;

        if (lastPrioritySoundPos != null && (now - lastPrioritySoundTimestamp) <= PRIORITY_SOUND_DURATION_MS) {
            return true;
        }

        if ((now - targetSetTime) > SOUND_REACTION_TIMEOUT) return false;

        Vec3d mobPos = mob.getPos();
        double effectiveRange = range * targetRangeMultiplier;
        if (mob.getWorld().isRaining() || mob.getWorld().isThundering()) {
            effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
        }

        return mobPos.squaredDistanceTo(targetSoundPos) <= effectiveRange * effectiveRange;
    }

    @Override
    public void start() {
        tickCounter = 0;
        if (targetSoundPos != null) {
            updateNavigation();
        }
    }

    @Override
    public void tick() {
        if (targetSoundPos == null) {
            return;
        }

        tickCounter++;

        if (tickCounter % NAVIGATION_UPDATE_INTERVAL != 0) return;

        updateNavigation();

        if (isMonster && targetSoundPos != null && tickCounter % LOOK_UPDATE_INTERVAL == 0) {
            mob.getLookControl().lookAt(targetSoundPos.x, targetSoundPos.y, targetSoundPos.z, 30.0F, 30.0F);
        }
    }

    @Override
    public void stop() {
        targetSoundPos = null;
        tickCounter = 0;
    }

    private void updateNavigation() {
        if (targetSoundPos == null || mob.getTarget() != null) return;

        long now = System.currentTimeMillis();
        if (lastPrioritySoundPos != null && (now - lastPrioritySoundTimestamp) > PRIORITY_SOUND_DURATION_MS) {
            lastPrioritySoundPos = null;
        }

        Vec3d currentPos = mob.getPos();
        boolean isPriority = lastPrioritySoundPos != null && targetSoundPos.equals(lastPrioritySoundPos);

        double effectiveRange = range * targetRangeMultiplier;
        double effectiveSpeed = speed * targetSpeedMultiplier;

        if (isPriority) {
            effectiveRange *= 1.5;
            effectiveSpeed *= 1.3;
        }

        if (mob.getWorld().isRaining() || mob.getWorld().isThundering()) {
            effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
        }

        double distance = currentPos.distanceTo(targetSoundPos);

        if (distance > effectiveRange) return;

        if (isPriority && distance < 2.0) {
            lastPrioritySoundPos = null;
            targetSoundPos = null;
            return;
        }

        if (distance > 50.0) effectiveSpeed *= 0.8;

        Vec3d target = isMonster ?
                grounded(targetSoundPos) :
                grounded(currentPos.add(currentPos.subtract(targetSoundPos).normalize().multiply(effectiveRange)));

        mob.getNavigation().startMovingTo(target.x, target.y, target.z, effectiveSpeed);
    }

    private Vec3d grounded(Vec3d desiredXZ) {
        BlockPos base = BlockPos.ofFloored(desiredXZ.x, 0, desiredXZ.z);
        BlockPos top = mob.getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, base);
        return new Vec3d(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
    }

    public static void setPrioritySound(Vec3d position) {
        lastPrioritySoundPos = position;
        lastPrioritySoundTimestamp = System.currentTimeMillis();
    }
}