package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.Plugin;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

@SuppressWarnings("unused")
public class RunawayVoiceGoal extends Goal {


    private final AnimalEntity mob;
    private final double speedModifier;
    private final int voiceDetectionRange;
    private PlayerEntity targetPlayer;
    private final double threshold;
    private BlockPos targetSoundPosition;
    private double targetSoundSpeed;
    private int ambientSoundCount;
    private int fleeTicks = 0;
    private int distanceCovered = 0;

    public RunawayVoiceGoal(AnimalEntity mob, double speedModifier, int voiceDetectionRange, double threshold) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.voiceDetectionRange = voiceDetectionRange;
        this.threshold = threshold;
        this.ambientSoundCount = 0;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }


    @Override
    public boolean canStart() {
        targetPlayer = findNearestPlayer();
        targetSoundPosition = Plugin.getLastSoundLocation(mob.getBlockPos(), voiceDetectionRange);
        targetSoundSpeed = Plugin.getLastSoundSpeed(mob.getBlockPos(), voiceDetectionRange);
        return targetSoundPosition != null;
    }

    @Override
    public boolean shouldContinue() {
        return targetSoundPosition != null && !mob.getNavigation().isIdle();
    }

    @Override
    public void start() {
        if (targetSoundPosition != null) {
            fleeFrom(Vec3d.ofCenter(targetSoundPosition));
        }
    }

    @Override
    public void tick() {
        if (targetSoundPosition != null) {
            handleSoundThreat();
        }
    }

    @Override
    public void stop() {
        targetSoundPosition = null;
        targetPlayer = null;
        ambientSoundCount = 0;
        mob.getNavigation().stop();
    }

    private PlayerEntity findNearestPlayer(){
        return mob.getWorld().getClosestPlayer(mob, voiceDetectionRange);
    }

    private void handleSoundThreat() {
        distanceCovered++;
        if (distanceCovered > 10 && distanceCovered % 20 == 0) {
            mob.getNavigation().stop();
            mob.getLookControl().lookAt(Vec3d.ofCenter(targetSoundPosition).x, Vec3d.ofCenter(targetSoundPosition).y, Vec3d.ofCenter(targetSoundPosition).z);
        }

        if (targetSoundPosition == null || mob.getBlockPos().getSquaredDistance(targetSoundPosition) > threshold * threshold) {
            targetSoundPosition = Plugin.getLastSoundLocation(mob.getBlockPos(), voiceDetectionRange);
            targetSoundSpeed = Plugin.getLastSoundSpeed(mob.getBlockPos(), voiceDetectionRange);
        } else {
            fleeFrom(Vec3d.ofCenter(targetSoundPosition));
        }
    }

    private void fleeFrom(Vec3d dangerPosition) {
        fleeTicks++;

        Vec3d fleeDirection = mob.getPos().subtract(dangerPosition).normalize().multiply(20.0);
        double randomOffsetX = (mob.getRandom().nextDouble() - 0.5) * 5.0;
        double randomOffsetZ = (mob.getRandom().nextDouble() - 0.5) * 5.0;

        Vec3d fleeTarget = mob.getPos().add(fleeDirection).add(randomOffsetX, 0, randomOffsetZ);
        if (isDangerousBlock(new BlockPos((int) fleeTarget.getX(), (int) fleeTarget.getY(), (int) fleeTarget.getZ()))) {
            fleeDirection = fleeDirection.add(randomOffsetX, 0, randomOffsetZ).normalize().multiply(20.0);
            fleeTarget = mob.getPos().add(fleeDirection);
        }

        mob.getNavigation().startMovingTo(fleeTarget.getX(), fleeTarget.getY(), fleeTarget.getZ(), speedModifier);

        if (ambientSoundCount < 2 && mob.getRandom().nextDouble() < 0.5) {
            mob.playAmbientSound();
            ambientSoundCount++;
        }
    }

    private boolean isDangerousBlock(BlockPos pos) {
        ServerWorld world = (ServerWorld) mob.getWorld();
        BlockState blockState = world.getBlockState(pos);
        return !blockState.getFluidState().isEmpty() || !blockState.isAir() || blockState.isSolidBlock(world, pos);
    }


}

