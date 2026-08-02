package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.utils.VoiceModDetection;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.EnumSet;

public class RunawayVoiceGoal extends Goal {

    private final AnimalEntity mob;
    private final double speedModifier;
    private final double voiceDetectionRange;
    private final double threshold;
    private BlockPos targetSoundPosition;
    private int ambientSoundCount;
    private int distanceCovered = 0;

    public RunawayVoiceGoal(AnimalEntity mob, double speedModifier, double detectionRange, double threshold) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.voiceDetectionRange = detectionRange;
        this.threshold = threshold;
        this.ambientSoundCount = 0;
        this.setControls(EnumSet.of(Control.MOVE, Control.TARGET));
    }

    @Override
    public boolean canStart() {
        targetSoundPosition = VoiceModDetection.getLastSoundLocation(mob.getBlockPos(), voiceDetectionRange, threshold);
        boolean canStart = targetSoundPosition != null;

        return targetSoundPosition != null;
    }
    
    @Override
    public boolean shouldContinue() {
        return targetSoundPosition != null && !mob.getNavigation().isIdle();
    }

    @Override
    public void start() {
        if (targetSoundPosition != null) {
            Vec3d groundedDanger = grounded(Vec3d.ofCenter(targetSoundPosition));
            fleeFrom(groundedDanger);
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
        ambientSoundCount = 0;
        mob.getNavigation().stop();
    }

    private void handleSoundThreat() {
        distanceCovered++;

        BlockPos groundedPos = mob.getEntityWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, targetSoundPosition);
        double gx = groundedPos.getX() + 0.5;
        double gz = groundedPos.getZ() + 0.5;

        if (distanceCovered > 10 && distanceCovered % 20 == 0) {
            mob.getNavigation().stop();
            mob.getLookControl().lookAt(
                    gx,
                    groundedPos.getY(),
                    gz,
                    30.0F,
                    30.0F
            );
        }

        double dx = mob.getX() - gx;
        double dz = mob.getZ() - gz;
        double distanceSq2D = dx * dx + dz * dz;

        if (targetSoundPosition == null || distanceSq2D > threshold * threshold) {
            targetSoundPosition = VoiceModDetection.getLastSoundLocation(mob.getBlockPos(), voiceDetectionRange, threshold);
        } else {
            fleeFrom(new Vec3d(gx, groundedPos.getY(), gz));
        }
    }

    private void fleeFrom(Vec3d dangerPosition) {
        Vec3d fleeDirection = mob.getEntityPos().subtract(dangerPosition).normalize().multiply(20.0);
        double randomOffsetX = (mob.getRandom().nextDouble() - 0.5) * 5.0;
        double randomOffsetZ = (mob.getRandom().nextDouble() - 0.5) * 5.0;

        Vec3d fleeTarget = mob.getEntityPos().add(fleeDirection).add(randomOffsetX, 0, randomOffsetZ);
        Vec3d groundedTarget = grounded(fleeTarget);

        if (isDangerousBlock(BlockPos.ofFloored(groundedTarget))) {
            fleeDirection = fleeDirection.add(mob.getRandom().nextDouble() * 5.0, 0, mob.getRandom().nextDouble() * 5.0);
            fleeTarget = mob.getEntityPos().add(fleeDirection);
            groundedTarget = grounded(fleeTarget);
        }

        mob.getNavigation().startMovingTo(groundedTarget.x, groundedTarget.y, groundedTarget.z, speedModifier);

        if (ambientSoundCount < 2 && mob.getRandom().nextDouble() < 0.5) {
            mob.playAmbientSound();
            ambientSoundCount++;
        }
    }

    private Vec3d grounded(Vec3d desiredXZ) {
        BlockPos base = BlockPos.ofFloored(desiredXZ.x, 0, desiredXZ.z);
        BlockPos top = mob.getEntityWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, base);
        return new Vec3d(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
    }

    private boolean isDangerousBlock(BlockPos pos) {
        ServerWorld getWorld = (ServerWorld) mob.getEntityWorld();
        BlockState blockState = getWorld.getBlockState(pos);

        return !blockState.getFluidState().isEmpty() || !blockState.isSolidBlock(getWorld, pos);
    }
}
