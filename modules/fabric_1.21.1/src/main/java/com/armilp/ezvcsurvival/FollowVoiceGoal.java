package com.armilp.ezvcsurvival;

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
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.TARGET));
    }

    @Override
    public boolean canStart() {
        // Detecta el último sonido o un jugador cercano
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
        return targetPlayer != null || (targetSoundPosition != null && !mob.getNavigation().isIdle());
    }

    @Override
    public void tick() {
        if (targetPlayer != null) {
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

        if (targetPlayer.isCreative()) {
            targetPlayer = null;
            mob.getNavigation().stop();
            return;
        }

        // If the player goes out of range, restart tracking
        if (distanceToPlayer > 2.0) {
            targetPlayer = null;
            mob.getNavigation().stop();
            return;
        }

        // If the mob is close to the player (e.g. attack range = 1.5 blocks), attack
        if (distanceToPlayer <= 1.0) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAttackTime >= attackCooldown) {
                mob.getNavigation().stop();
                mob.swingHand(mob.preferredHand); // Animación del ataque
                mob.tryAttack(targetPlayer);   // Realiza el ataque
                lastAttackTime = currentTime;
            }
            return;
        }


        mob.getNavigation().startMovingTo(targetPlayer, speedModifier);
        targetSoundPosition = null;
    }

    private void handleSoundInteraction() {
        double distanceToTarget = mob.getBlockPos().getSquaredDistance(targetSoundPosition);

        if (distanceToTarget <= 2.0 * 2.0) {
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
