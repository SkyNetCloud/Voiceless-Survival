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
    private long lastAttackTime = 0;
    private final long attackCooldown = 2000;

    public FollowVoiceGoal(MobEntity mob, double speedModifier, int detectionRange, double threshold) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.voiceDetectionRange = detectionRange;
        this.threshold = threshold;
        this.setControls(EnumSet.of(Control.MOVE, Control.TARGET));
    }

    @Override
    public boolean canStart() {
        // Detect the last sound or a nearby player
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
        return targetPlayer != null || (targetSoundPosition != null && !mob.getNavigation().isFollowingPath());
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
        double distanceToPlayer = mob.distanceTo(targetPlayer);

        if (targetPlayer.isCreative()){
            targetPlayer = null;
            mob.getNavigation().stop();
            return;
        }

        // If the player is out of detection range, reset the target
        if (distanceToPlayer > voiceDetectionRange) {
            targetPlayer = null;
            mob.getNavigation().stop();
            return;
        }

        // If the mob is close to the player, attack
        if (distanceToPlayer <= 1.0) {
            long currentTime = System.currentTimeMillis(); // Current time in milliseconds

            // Check if the mob can attack again
            if (currentTime - lastAttackTime >= attackCooldown) {
                mob.getNavigation().stop();
                mob.swingHand(mob.preferredHand); // Attack animation
                mob.tryAttack(targetPlayer);   // Deal damage to the player
                lastAttackTime = currentTime;     // Update the time of the last attack
            }
            return;
        }

        mob.getNavigation().startMovingTo(targetPlayer, speedModifier);
        targetSoundPosition = null;
    }

    private void handleSoundInteraction() {
        double distanceToTarget = mob.getBlockPos().getSquaredDistance(targetSoundPosition);

        // If the mob is close enough to the sound source, follow it
        if (distanceToTarget <= 1.5 * 1.5) {
            targetSoundPosition = Plugin.getLastSoundLocation(mob.getBlockPos(), voiceDetectionRange);
            if (targetSoundPosition != null) {
                moveToSoundPosition();
            } else {
                mob.getNavigation().stop();
            }
            return;
        }

        // If the mob is far from the sound, check for new sound positions
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

        // Set the mob's movement speed based on the sound's intensity or speed
        double soundSpeed = Plugin.getLastSoundSpeed(mob.getBlockPos(), voiceDetectionRange);
        mob.getNavigation().setSpeed(soundSpeed);
    }

    private PlayerEntity getNearestPlayerInRange() {
        return mob.world.getClosestPlayer(mob, 5);
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
