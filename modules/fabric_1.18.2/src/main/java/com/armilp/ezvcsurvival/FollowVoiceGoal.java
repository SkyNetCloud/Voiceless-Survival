package com.armilp.ezvcsurvival;

import com.armilp.ezvcsurvival.config.VoiceConfig;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Shadow;

import java.util.EnumSet;
import java.util.Map;

import static net.minecraft.util.registry.Registry.ENTITY_TYPE;

public class FollowVoiceGoal extends Goal {


    private final MobEntity mob;
    private final double speedModifier;
    private final int voiceDetectionRange;
    private final double threshold;
    private final long attackCooldown = 2000;

    private PlayerEntity targetPlayer;
    private BlockPos targetSoundPosition;
    private long timePlayerInRange;
    private long lastAttackTime = 0;

    public FollowVoiceGoal(MobEntity mob, double speedModifier, int detectionRange, double threshold) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.voiceDetectionRange = detectionRange;
        this.threshold = threshold;
        this.setControls(EnumSet.of(Control.MOVE, Control.TARGET));
    }

    @Override
    public boolean canStart() {
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
        // Check if the player is still valid and not in creative mode
        if (targetPlayer != null) {
            if (targetPlayer.isCreative()) {
                targetPlayer = null;  // Reset target player if they are in creative mode
                mob.getNavigation().stop();  // Stop the mob from moving towards the player
                return false;  // Stop following the player
            }
            return true;  // Continue pursuing the player if not in creative mode
        }

        // Continue pursuing the sound if targetSoundPosition is not null and mob is still moving
        return targetSoundPosition != null && !mob.getNavigation().isIdle();
    }


    @Override
    public void tick() {
        if (targetPlayer != null) {
            handlePlayerInteraction();
        } else if (targetSoundPosition != null) {
            handleSoundInteraction();
        }

        // If the target sound position is null (i.e., sound has stopped), stop the navigation.
        if (targetSoundPosition == null) {
            mob.getNavigation().stop();  // Stop the mob from moving
            targetPlayer = null;  // Reset target player
        }
    }

    private void handlePlayerInteraction() {
        if (targetPlayer == null) return;

        double distanceToPlayer = mob.distanceTo(targetPlayer);

        // Stop tracking if the player is creative or out of range
        if (targetPlayer.isCreative() || distanceToPlayer > 2.0) {
            targetPlayer = null;  // Stop following the player
            mob.getNavigation().stop();  // Ensure the mob stops moving
            return;
        }

        // Attack if within range
        if (distanceToPlayer <= 1.0) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAttackTime >= attackCooldown) {
                mob.getNavigation().stop();  // Stop moving to attack
                mob.swingHand(mob.preferredHand);
                mob.tryAttack(targetPlayer);
                lastAttackTime = currentTime;
            }
            return;
        }

        // Move towards the player
        mob.getNavigation().startMovingTo(targetPlayer, speedModifier);
        targetSoundPosition = null;
    }

    private void handleSoundInteraction() {
        if (targetSoundPosition == null) return;

        double distanceToTarget = mob.getBlockPos().getSquaredDistance(targetSoundPosition);

        // If reached the sound position, update or stop
        if (distanceToTarget <= 4.0) {
            targetSoundPosition = Plugin.getLastSoundLocation(mob.getBlockPos(), voiceDetectionRange);
            if (targetSoundPosition != null) {
                moveToSoundPosition();
            } else {
                mob.getNavigation().stop();  // Stop moving if no new sound location
            }
            return;
        }

        // If sound is out of range, fetch the latest position
        if (distanceToTarget > (voiceDetectionRange * voiceDetectionRange) / 2.0) {
            BlockPos newSoundPosition = Plugin.getLastSoundLocation(mob.getBlockPos(), voiceDetectionRange);
            if (newSoundPosition == null) {
                targetSoundPosition = null;
                mob.getNavigation().stop();  // Stop moving if no new sound
            } else {
                targetSoundPosition = newSoundPosition;
                moveToSoundPosition();
            }
        }

        // Adjust speed based on sound intensity
        double soundSpeed = Plugin.getLastSoundSpeed(mob.getBlockPos(), voiceDetectionRange);
        mob.getNavigation().setSpeed(soundSpeed);
    }

    @Override
    public void stop() {
        targetSoundPosition = null;
        targetPlayer = null;
        mob.getNavigation().stop();  // Stop the mob from moving
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
