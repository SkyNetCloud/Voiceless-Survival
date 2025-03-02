package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.Plugin;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

public class FollowVoiceGoal extends Goal {

    private final MobEntity mob;
    private final double speedModifier;
    private final int voiceDetectionRange;
    private PlayerEntity targetPlayer;
    private final double threshold;
    private BlockPos targetSoundPosition;
    private long timePlayerInRange;
    private final long maxFollowTime;

    public FollowVoiceGoal(MobEntity mob, double speedModifier, int detectionRange, double threshold, long maxFollowTime) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.voiceDetectionRange = detectionRange;
        this.threshold = threshold;
        this.maxFollowTime = maxFollowTime;
        this.setControls(EnumSet.of(Control.MOVE, Control.TARGET));
    }

    @Override
    public boolean canStart() {
        if (mob.getTarget() != null) {
            return false;
        }

        targetPlayer = getNearestPlayerInRange();
        targetSoundPosition = Plugin.getLastSoundLocation(mob.getBlockPos(), voiceDetectionRange);
        return targetPlayer != null || targetSoundPosition != null;
    }

    @Override
    public void start() {
        if (targetPlayer != null) {
            timePlayerInRange = System.currentTimeMillis();
        } else if (targetSoundPosition != null) {
            moveToSoundPosition();
        }
    }

    @Override
    public boolean shouldContinue() {
        if (mob.getTarget() != null) {
            return false;
        }
        return targetPlayer != null || (targetSoundPosition != null && !mob.getNavigation().isIdle());
    }

    @Override
    public void tick() {
        if (targetPlayer != null) {
            targetSoundPosition = null;
            handlePlayerInteraction();
        } else if (targetSoundPosition != null) {
            handleSoundInteraction();
        }
    }

    @Override
    public void stop() {
        targetSoundPosition = null;
        targetPlayer = null;
        mob.getNavigation().stop();
    }

    private void handlePlayerInteraction() {
        if (targetPlayer.isCreative()) {
            targetPlayer = null;
            mob.getNavigation().stop();
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - timePlayerInRange > maxFollowTime) {
            targetPlayer = null;
            mob.getNavigation().stop();
            return;
        }

        mob.getNavigation().setSpeed(speedModifier);

        if (mob.getTarget() == null) {
            mob.setTarget(targetPlayer);
        }
    }

    private void handleSoundInteraction() {
        double distanceToTarget = mob.getBlockPos().getSquaredDistance(targetSoundPosition);

        if (distanceToTarget <= 1.5 * 1.5) {
            targetSoundPosition = Plugin.getLastSoundLocation(mob.getBlockPos(), voiceDetectionRange);
            if (targetSoundPosition != null) {
                moveToSoundPosition();
            } else {
                mob.getNavigation().stop();
            }
            return;
        }

        if (distanceToTarget > (double) (voiceDetectionRange * voiceDetectionRange) / 2) {
            BlockPos newSoundPosition = Plugin.getLastSoundLocation(mob.getBlockPos(), voiceDetectionRange);
            if (newSoundPosition == null) {
                targetSoundPosition = null;
                mob.getNavigation().stop();
                return;
            } else {
                targetSoundPosition = newSoundPosition;
                moveToSoundPosition();
            }
        }

        double soundSpeed = Plugin.getLastSoundSpeed(mob.getBlockPos(), voiceDetectionRange);
        mob.getNavigation().setSpeed(soundSpeed);
    }

    private PlayerEntity getNearestPlayerInRange() {
        return mob.getWorld().getClosestPlayer(mob, 5);
    }

    private void moveToSoundPosition() {
        if (targetSoundPosition != null) {
            mob.getNavigation().startMovingTo(
                    targetSoundPosition.getX() + 0.5,
                    targetSoundPosition.getY(),
                    targetSoundPosition.getZ() + 0.5,
                    speedModifier
            );
        }
    }
}