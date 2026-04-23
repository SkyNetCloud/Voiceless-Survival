package com.armilp.ezvcsurvival.goals;


import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;

//#todo fixed this ReactToGunfireGoal, it's currently disabled because I don't what I'm doing
public class ReactToGunfireGoal extends Goal {

    private final MobEntity mob;
    private final double baseSpeed;
    private final double baseRange;

    private static final long PRIORITY_SOUND_DURATION_MS = 3000;

    public ReactToGunfireGoal(MobEntity mob, double speed, double range) {
        this.mob = mob;
        this.baseSpeed = speed;
        this.baseRange = range;
    }

    @Override
    public boolean canStart() {
//        if (mob.getTarget() != null) {
//            return false;
//        }
//
//        if (ReactToGeneralSoundGoal.lastPrioritySoundPos != null &&
//                System.currentTimeMillis() - ReactToGeneralSoundGoal.lastPrioritySoundTimestamp > PRIORITY_SOUND_DURATION_MS) {
//            ReactToGeneralSoundGoal.lastPrioritySoundPos = null;
//        }
//
//        checkForPrioritySounds();
//
//        Vec3d mobCenterPos = mob.getEntityPos();
//
//        if (ReactToGeneralSoundGoal.lastPrioritySoundPos != null) {
//            double priorityRange = baseRange * 1.5;
//            if (mob.getEntityWorld().isRaining() || mob.getEntityWorld().isThundering()) {
//                priorityRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
//            }
//            if (mobCenterPos.distanceTo(ReactToGeneralSoundGoal.lastPrioritySoundPos) <= priorityRange) {
//                return true;
//            }
//        }
//
//        if (ReactToGeneralSoundGoal.lastPrioritySoundPos == null) {
//            double effectiveRange = baseRange;
//            String effectiveGunType = null;
//
//            GunshotData gunshotData = GunFireListener.getLastGunshotData();
//            if (gunshotData != null) {
//                effectiveGunType = gunshotData.gunType.name().toLowerCase();
//            }
//
//            if (effectiveGunType != null) {
//                double rangeMultiplier = SoundConfig.getRangeMultiplier(effectiveGunType);
//                effectiveRange = baseRange * rangeMultiplier;
//            }
//
//            if (mob.getEntityWorld().isRaining() || mob.getEntityWorld().isThundering()) {
//                effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
//            }
//
//            return gunshotData != null && mobCenterPos.distanceTo(gunshotData.position) <= effectiveRange;
//        }
//
//        return false;
        return false;
    }
//
//    @Override
//    public void start() {
//        updateNavigation();
//    }
//
//    @Override
//    public void tick() {
//        updateNavigation();
//    }
//
//    private void checkForPrioritySounds() {
//        for (SoundGroupData priority : SoundConfig.getPriorityGroups()) {
//            for (String soundStr : priority.sounds) {
//                Identifier soundLoc = toLocation(soundStr);
//                Vec3d priorityPos = SoundEventTracker.getLastPlayedPositionForSound(soundLoc);
//                if (priorityPos != null) {
//                    ReactToGeneralSoundGoal.setPrioritySound(priorityPos);
//                    return;
//                }
//            }
//        }
//
//        GunshotData gunshotData = GunFireListener.getLastGunshotData();
//        if (gunshotData != null) {
//            String gunType = gunshotData.gunType.name().toLowerCase();
//            if (isPriorityGunType(gunType)) {
//                ReactToGeneralSoundGoal.setPrioritySound(gunshotData.position);
//            }
//        }
//    }
//
//    private boolean isPriorityGunType(String gunType) {
//        double rangeMultiplier = SoundConfig.getRangeMultiplier(gunType);
//        return rangeMultiplier >= 6.0;
//    }
//
//    private void updateNavigation() {
//        if (ReactToGeneralSoundGoal.lastPrioritySoundPos != null &&
//                System.currentTimeMillis() - ReactToGeneralSoundGoal.lastPrioritySoundTimestamp > PRIORITY_SOUND_DURATION_MS) {
//            ReactToGeneralSoundGoal.lastPrioritySoundPos = null;
//        }
//
//        checkForPrioritySounds();
//
//        Vec3d currentPos = mob.position();
//        Vec3d target = null;
//        double effectiveRange = baseRange;
//        double effectiveSpeed = baseSpeed;
//
//        if (ReactToGeneralSoundGoal.lastPrioritySoundPos != null) {
//            effectiveRange = baseRange * 1.5;
//            effectiveSpeed = baseSpeed * 1.3;
//
//            if (mob.level().isRaining() || mob.level().isThundering()) {
//                effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
//            }
//
//            if (currentPos.distanceTo(ReactToGeneralSoundGoal.lastPrioritySoundPos) <= effectiveRange) {
//                if (mob instanceof MobEntity) {
//                    target = grounded(ReactToGeneralSoundGoal.lastPrioritySoundPos);
//                } else {
//                    Vec3 directionAway = currentPos.subtract(ReactToGeneralSoundGoal.lastPrioritySoundPos).normalize();
//                    target = grounded(currentPos.add(directionAway.scale(effectiveRange)));
//                }
//
//                if (target != null) {
//                    mob.getNavigation().moveTo(target.x, target.y, target.z, effectiveSpeed);
//                    if (currentPos.distanceTo(ReactToGeneralSoundGoal.lastPrioritySoundPos) < 2.0) {
//                        ReactToGeneralSoundGoal.lastPrioritySoundPos = null;
//                    }
//                }
//                return;
//            }
//        }
//
//        if (ReactToGeneralSoundGoal.lastPrioritySoundPos == null) {
//            String effectiveGunType = null;
//
//            GunshotData gunshotData = GunFireListener.getLastGunshotData();
//            if (gunshotData != null) {
//                effectiveGunType = gunshotData.gunType.name().toLowerCase();
//            }
//
//            if (effectiveGunType != null) {
//                double rangeMultiplier = SoundConfig.getRangeMultiplier(effectiveGunType);
//                double speedMultiplier = SoundConfig.getSpeedMultiplier(effectiveGunType);
//                effectiveRange = baseRange * rangeMultiplier;
//                effectiveSpeed = baseSpeed * speedMultiplier;
//            }
//
//            if (mob.level().isRaining() || mob.level().isThundering()) {
//                effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
//            }
//
//            if (mob instanceof MobEntity) {
//                if (gunshotData != null && currentPos.distanceTo(gunshotData.position) <= effectiveRange) {
//                    target = grounded(gunshotData.position);
//                }
//
//                if (target != null) {
//                    mob.getNavigation().moveTo(target.x, target.y, target.z, effectiveSpeed);
//                }
//            } else {
//                if (gunshotData != null && currentPos.distanceTo(gunshotData.position) <= effectiveRange) {
//                    Vec3 directionAway = currentPos.subtract(gunshotData.position).normalize();
//                    Vec3 fleeTarget = grounded(currentPos.add(directionAway.scale(effectiveRange)));
//                    mob.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, effectiveSpeed);
//                }
//            }
//        }
//    }
//
//    private Vec3d grounded(Vec3d desiredXZ) {
//        BlockPos base = BlockPos.ofFloored(desiredXZ.x, 0, desiredXZ.z);
//        BlockPos top = mob.getEntityWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, base);
//        return new Vec3(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
//    }
//
//    private Identifier toLocation(String soundStr) {
//        if (soundStr.contains(":")) {
//            return Identifier.of(soundStr);
//        }
//        return Identifier.of(soundStr);
//    }
}
