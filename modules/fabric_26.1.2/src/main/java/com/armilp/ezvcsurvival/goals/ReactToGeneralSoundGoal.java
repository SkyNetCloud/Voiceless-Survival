package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;


import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import static net.minecraft.world.entity.ai.goal.Goal.Flag.LOOK;
import static net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE;

public class ReactToGeneralSoundGoal extends Goal {
    private static final long PRIORITY_SOUND_DURATION_MS = 3500;
    private static final int NAVIGATION_UPDATE_INTERVAL = 15;
    private static final int LOOK_UPDATE_INTERVAL = 30;
    private static final long SOUND_REACTION_TIMEOUT = 4000;

    public static Vec3 lastPrioritySoundPos = null;
    public static long lastPrioritySoundTimestamp = 0;

    private final Mob mob;
    private final double speed;
    private final double range;
    private final List<SoundGroupData> soundGroups;
    private final String entityId;
    private final boolean isMonster;

    private Vec3 targetSoundPos = null;
    private double targetSpeedMultiplier = 1.0;
    private double targetRangeMultiplier = 1.0;
    private long targetSetTime = 0;
    private int tickCounter = 0;

    public ReactToGeneralSoundGoal(Mob mob, double speed, double range, List<SoundGroupData> soundGroups) {
        this.mob = mob;
        this.speed = speed;
        this.range = range;
        this.soundGroups = soundGroups;
        this.entityId = Objects.requireNonNull(Registries.ENTITY_TYPE.identifier()).toString();
        this.isMonster = mob instanceof Monster;
        this.setFlags(EnumSet.of(MOVE, LOOK));

        if (VoiceConfig.DEBUG.get()) {
            EZVCSurvival.LOGGER.debug("[ReactToGeneralSoundGoal] Created for {} with speed: {}, range: {}",
                    mob.getType(), speed, range);
        }
    }

    public void onSoundPlayed(Identifier soundLoc, Vec3 soundPos, double speedMult, double rangeMult) {
        if (mob.getTarget() != null) return;

        String soundId = soundLoc.toString();
        if (!GeneralSoundsConfig.canEntityReactToSound(entityId, soundId)) return;

        boolean isPriority = false;
        for (SoundGroupData group : soundGroups) {
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
            for (SoundGroupData group : soundGroups) {
                if (group.sounds().contains(soundId)) {
                    speedMult = group.speedMultiplier();
                    rangeMult = group.rangeMultiplier();
                    found = true;
                    break;
                }
            }
            if (!found) return;
        }

        Vec3 mobPos = mob.position();
        double effectiveRange = range * rangeMult;
        if (mob.level().isRaining() || mob.level().isThundering()) {
            effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
        }

        double distSq = mobPos.distanceToSqr(soundPos);
        if (distSq > effectiveRange * effectiveRange) {
            if (VoiceConfig.DEBUG.get()) {
                EZVCSurvival.LOGGER.debug("[ReactToGeneralSoundGoal] Sound {} too far ({} > {})",
                        soundId, Math.sqrt(distSq), effectiveRange);
            }
            return;
        }

        this.targetSoundPos = soundPos;
        this.targetSpeedMultiplier = speedMult;
        this.targetRangeMultiplier = rangeMult;
        this.targetSetTime = System.currentTimeMillis();

        if (VoiceConfig.DEBUG.get()) {
            EZVCSurvival.LOGGER.debug("[ReactToGeneralSoundGoal] Reacting to sound {} at {} (distance: {}, priority: {})",
                    soundId, soundPos, Math.sqrt(distSq), isPriority);
        }

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
            Vec3 mobPos = mob.position();
            double effectiveRange = range * 1.5;
            if (mob.level().isRaining() || mob.level().isThundering()) {
                effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
            }
            if (mobPos.distanceToSqr(lastPrioritySoundPos) <= effectiveRange * effectiveRange) {
                targetSoundPos = lastPrioritySoundPos;
                targetSpeedMultiplier = 1.5;
                targetRangeMultiplier = 1.5;

                if (VoiceConfig.DEBUG.get()) {
                    EZVCSurvival.LOGGER.debug("[ReactToGeneralSoundGoal] canStart: Reacting to priority sound at {}", lastPrioritySoundPos);
                }
                return true;
            }
        }

        if (targetSoundPos != null && (now - targetSetTime) < SOUND_REACTION_TIMEOUT) {
            if (VoiceConfig.DEBUG.get()) {
                EZVCSurvival.LOGGER.debug("[ReactToGeneralSoundGoal] canStart: Reacting to recent sound at {}", targetSoundPos);
            }
            return true;
        }

        if (VoiceConfig.DEBUG.get() && targetSoundPos != null) {
            EZVCSurvival.LOGGER.debug("[ReactToGeneralSoundGoal] canStart: Sound expired ({} > {})",
                    now - targetSetTime, SOUND_REACTION_TIMEOUT);
        }

        return false;
    }


    @Override
    public boolean canContinueToUse() {
        if (mob.getTarget() != null) return false;

        long now = System.currentTimeMillis();
        if (targetSoundPos == null) return false;

        if (lastPrioritySoundPos != null && (now - lastPrioritySoundTimestamp) <= PRIORITY_SOUND_DURATION_MS) {
            return true;
        }

        if ((now - targetSetTime) > SOUND_REACTION_TIMEOUT) {
            if (VoiceConfig.DEBUG.get()) {
                EZVCSurvival.LOGGER.debug("[ReactToGeneralSoundGoal] shouldContinue: Sound reaction timeout");
            }
            return false;
        }

        Vec3 mobPos = mob.position();
        double effectiveRange = range * targetRangeMultiplier;
        if (mob.level().isRaining() || mob.level().isThundering()) {
            effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
        }

        boolean inRange = mobPos.distanceToSqr(targetSoundPos) <= effectiveRange * effectiveRange;

        if (VoiceConfig.DEBUG.get() && !inRange) {
            EZVCSurvival.LOGGER.debug("[ReactToGeneralSoundGoal] shouldContinue: Out of range ({} > {})",
                    mobPos.distanceTo(targetSoundPos), effectiveRange);
        }

        return inRange;
    }

    @Override
    public void start() {
        if (VoiceConfig.DEBUG.get()) {
            EZVCSurvival.LOGGER.debug("[ReactToGeneralSoundGoal] start() for {} moving to sound at {}",
                    mob.getType(), targetSoundPos);
        }

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

        if (VoiceConfig.DEBUG.get() && tickCounter % 40 == 0) {
            EZVCSurvival.LOGGER.debug("[ReactToGeneralSoundGoal] tick: Updating navigation for {} to {}",
                    mob.getType(), targetSoundPos);
        }

        updateNavigation();

        if (isMonster && targetSoundPos != null && tickCounter % LOOK_UPDATE_INTERVAL == 0) {
            mob.getLookControl().setLookAt(targetSoundPos.x, targetSoundPos.y, targetSoundPos.z, 30.0F, 30.0F);
        }
    }

    @Override
    public void stop() {
        if (VoiceConfig.DEBUG.get()) {
            EZVCSurvival.LOGGER.debug("[ReactToGeneralSoundGoal] stop() for {}", mob.getType());
        }
        targetSoundPos = null;
        tickCounter = 0;
    }

    private void updateNavigation() {
        if (targetSoundPos == null || mob.getTarget() != null) return;

        long now = System.currentTimeMillis();
        if (lastPrioritySoundPos != null && (now - lastPrioritySoundTimestamp) > PRIORITY_SOUND_DURATION_MS) {
            lastPrioritySoundPos = null;
        }

        Vec3 currentPos = mob.position();
        boolean isPriority = targetSoundPos.equals(lastPrioritySoundPos);

        double effectiveRange = range * targetRangeMultiplier;
        double effectiveSpeed = speed * targetSpeedMultiplier;

        if (isPriority) {
            effectiveRange *= 1.5;
            effectiveSpeed *= 1.3;
        }

        if (mob.level().isRaining() || mob.level().isThundering()) {
            effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
        }

        double distance = currentPos.distanceTo(targetSoundPos);

        if (distance > effectiveRange) {
            if (VoiceConfig.DEBUG.get()) {
                EZVCSurvival.LOGGER.debug("[ReactToGeneralSoundGoal] updateNavigation: Too far ({} > {})",
                        distance, effectiveRange);
            }
            return;
        }

        if (isPriority && distance < 2.0) {
            if (VoiceConfig.DEBUG.get()) {
                EZVCSurvival.LOGGER.debug("[ReactToGeneralSoundGoal] updateNavigation: Reached priority sound");
            }
            lastPrioritySoundPos = null;
            targetSoundPos = null;
            return;
        }

        if (distance > 50.0) effectiveSpeed *= 0.8;

        Vec3 target = isMonster ?
                grounded(targetSoundPos) :
                grounded(currentPos.add(currentPos.subtract(targetSoundPos).normalize().multiply(effectiveRange,0, effectiveRange)));

        boolean pathStarted = mob.getNavigation().moveTo(target.x, target.y, target.z, effectiveSpeed);

        if (VoiceConfig.DEBUG.get()) {
            EZVCSurvival.LOGGER.debug("[ReactToGeneralSoundGoal] updateNavigation: Pathfinding {} to {} with speed {}",
                    pathStarted ? "succeeded" : "failed", target, effectiveSpeed);
        }
    }

    private Vec3 grounded(Vec3 desiredXZ) {
        BlockPos base = BlockPos.containing(desiredXZ.x, 0, desiredXZ.z);
        BlockPos top = mob.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base);
        return new Vec3(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
    }

    public static void setPrioritySound(Vec3 position) {
        if (VoiceConfig.DEBUG.get()) {
            EZVCSurvival.LOGGER.debug("[ReactToGeneralSoundGoal] Setting priority sound at {}", position);
        }
        lastPrioritySoundPos = position;
        lastPrioritySoundTimestamp = System.currentTimeMillis();
    }
}