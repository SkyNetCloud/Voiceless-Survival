package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.Plugin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.Control;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;


import java.util.EnumSet;

import static net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE;

public class FollowVoiceGoal extends Goal {

    private final Mob mob;
    private final double speedModifier;
    private final int voiceDetectionRange;
    private final double threshold;
    private BlockPos targetSoundPosition;
    private long timePlayerInRange;
    private final long maxFollowTime;
    private int updateCooldown;
    private static final int UPDATE_INTERVAL = 20; // Update every 20 ticks (1 second)

    public FollowVoiceGoal(Mob mob, double speedModifier, int detectionRange, double threshold, long maxFollowTime) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.voiceDetectionRange = detectionRange;
        this.threshold = threshold;
        this.maxFollowTime = maxFollowTime;
        this.setFlags(EnumSet.of(MOVE));
        this.updateCooldown = 0;
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null) {
            return false;
        }

        // Only check for sound location periodically to reduce lag
        if (updateCooldown <= 0) {
            targetSoundPosition = Plugin.getLastSoundLocation(mob.getBlockPos(), voiceDetectionRange, threshold);
            updateCooldown = UPDATE_INTERVAL;
        } else {
            updateCooldown--;
        }

        return targetSoundPosition != null;
    }

    @Override
    public void start() {
        timePlayerInRange = System.currentTimeMillis();
        updateCooldown = 0; // Reset cooldown when starting
        if (targetSoundPosition != null) {
            moveToSoundPosition();
        }
    }



    @Override
    public boolean canContinueToUse() {
        if (mob.getTarget() != null) {
            return false;
        }

        // Update sound position periodically while continuing
        if (updateCooldown <= 0) {
            targetSoundPosition = Plugin.getLastSoundLocation(mob.getBlockPos(), voiceDetectionRange, threshold);
            updateCooldown = UPDATE_INTERVAL;
        } else {
            updateCooldown--;
        }

        return targetSoundPosition != null;
    }

    @Override
    public void tick() {
        if (targetSoundPosition == null) {
            return;
        }

        // FIXED: Use a proper arrival distance (e.g., 3.0 blocks)
        double dx = mob.getX() - (targetSoundPosition.getX() + 0.5);
        double dz = mob.getZ() - (targetSoundPosition.getZ() + 0.5);
        double distanceSq = dx * dx + dz * dz;
        double ARRIVAL_DISTANCE_SQ = 3.0 * 3.0; // Arrive within 3 blocks

        if (distanceSq <= ARRIVAL_DISTANCE_SQ) {
            // Arrived at sound location, check for new sound
            targetSoundPosition = Plugin.getLastSoundLocation(mob.g(), voiceDetectionRange, threshold);
            if (targetSoundPosition != null) {
                moveToSoundPosition();
            } else {
                mob.getNavigation().stop();
            }
            return;
        }

        // Check if mob is stuck or needs path recalculation
        if (!mob.getNavigation().canNavigateGround() || mob.getNavigation().isDone()) {
            moveToSoundPosition();
        }
    }

    @Override
    public void stop() {
        targetSoundPosition = null;
        mob.getNavigation().stop();
    }

    private void moveToSoundPosition() {
        if (targetSoundPosition == null) {
            return;
        }

        // Get ground position at sound location
        BlockPos groundPos = mob.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetSoundPosition);

        // Move to the ground position
        boolean pathStarted = mob.getNavigation().moveTo(
                groundPos.getX() + 0.5,
                groundPos.getY(),
                groundPos.getZ() + 0.5,
                speedModifier
        );

        if (!pathStarted) {
            // Try moving directly to the sound position if pathfinding fails
            mob.getNavigation().moveTo(
                    targetSoundPosition.getX() + 0.5,
                    targetSoundPosition.getY(),
                    targetSoundPosition.getZ() + 0.5,
                    speedModifier
            );
        }
    }

    public long getTimePlayerInRange() {
        return timePlayerInRange;
    }

    public void setTimePlayerInRange(long timePlayerInRange) {
        this.timePlayerInRange = timePlayerInRange;
    }

    public long getMaxFollowTime() {
        return maxFollowTime;
    }
}