package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.data.GunshotData;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.event.GunFireListener;
import com.armilp.ezvcsurvival.event.SoundEventTracker;
import com.armilp.ezvcsurvival.config.SoundConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReactToSoundGoal extends Goal {

    private final MobEntity mob;
    private final double speed;
    private final int range;
    private final List<SoundGroupData> soundGroups;

    private Vec3d lastAttackerPos = null;
    private static final List<ReactToSoundGoal> activeGoals = new CopyOnWriteArrayList<>();


    public ReactToSoundGoal(MobEntity mob, double speed, int range, List<SoundGroupData> soundGroups) {
        this.mob = mob;
        this.speed = speed;
        this.range = range;
        this.soundGroups = soundGroups;
        activeGoals.add(this);
    }

    @Override
    public boolean canStart() {
        Vec3d mobCenterPos = mob.getPos();
        double effectiveRange = range;
        double effectiveSpeed = speed;

        GunshotData gunshotData = GunFireListener.getLastGunshotData();
        if (gunshotData != null) {
            double rangeMultiplier = SoundConfig.getRangeMultiplier(gunshotData.gunType().name().toLowerCase());
            effectiveRange = (int)(range * rangeMultiplier);
            double speedMultiplier = SoundConfig.getSpeedMultiplier(gunshotData.gunType().name().toLowerCase());
            effectiveSpeed = speed * speedMultiplier;
        }

        boolean gunshotTriggered = false;
        if (gunshotData != null) {
            double gunDistance = mobCenterPos.squaredDistanceTo(gunshotData.position());
            gunshotTriggered = gunDistance <= effectiveRange;
        }

        Vec3d soundEventPos = null;
        double groupSpeedMult = 1.0;
        double groupRangeMult = 1.0;
        outer:
        for (SoundGroupData group : soundGroups) {
            for (String soundStr : group.sounds()) {
                String res =  String.valueOf(soundStr);
                Vec3d pos = SoundEventTracker.getLastPlayedPositionForAny(res);
                if (pos != null) {
                    soundEventPos = pos;
                    groupSpeedMult = group.speedMultiplier();
                    groupRangeMult = group.rangeMultiplier();
                    break outer;
                }
            }
        }

        boolean soundTriggered = false;
        if (soundEventPos != null) {
            double groupEffectiveRange = range * groupRangeMult;
            soundTriggered = mobCenterPos.distanceTo(soundEventPos) <= groupEffectiveRange;
        }

        boolean hurtTriggered = !(mob instanceof Monster) && lastAttackerPos != null;

        return gunshotTriggered || soundTriggered || hurtTriggered;
    }

    public void start() {
        updateNavigation();
    }

    public void stop() {
        activeGoals.remove(this);
    }

    public void tick() {
        updateNavigation();
    }

    public void onHurt(Vec3d attackerPos) {
        this.lastAttackerPos = attackerPos;
    }

    private void updateNavigation() {
        Vec3d currentPos = mob.getPos();
        Vec3d target = null;
        double effectiveRange = range;
        double effectiveSpeed = speed;

        GunshotData gunshotData = GunFireListener.getLastGunshotData();
        if (gunshotData != null) {
            double rangeMultiplier = SoundConfig.getRangeMultiplier(gunshotData.gunType().name().toLowerCase());
            effectiveRange = (int)(range * rangeMultiplier);
            double speedMultiplier = SoundConfig.getSpeedMultiplier(gunshotData.gunType().name().toLowerCase());
            effectiveSpeed = speed * speedMultiplier;
        }

        Vec3d soundEventPos = null;
        double groupSpeedMult = 1.0;
        double groupRangeMult = 1.0;
        outer:
        for (SoundGroupData group : soundGroups) {
            for (String soundStr : group.sounds()) {
                String res = String.valueOf(soundStr);
                Vec3d pos = SoundEventTracker.getLastPlayedPositionForAny(res);
                if (pos != null) {
                    soundEventPos = pos;
                    groupSpeedMult = group.speedMultiplier();
                    groupRangeMult = group.rangeMultiplier();
                    break outer;
                }
            }
        }
        if (soundEventPos != null) {
            effectiveRange = (int)(range * groupRangeMult);
            effectiveSpeed = speed * groupSpeedMult;
        }

        if (mob instanceof Monster) {
            if (mob.getTarget() instanceof PlayerEntity) {
                return;
            }
            if (gunshotData != null && currentPos.distanceTo(gunshotData.position()) <= effectiveRange) {
                target = new Vec3d(gunshotData.position().x, mob.getY(), gunshotData.position().z);
            } else if (soundEventPos != null && currentPos.distanceTo(soundEventPos) <= effectiveRange) {
                target = new Vec3d(soundEventPos.x, mob.getY(), soundEventPos.z);
            }
        } else {
            if (lastAttackerPos != null) {
                Vec3d diff = currentPos.subtract(lastAttackerPos);
                if (diff.lengthSquared() < 1e-4) {
                    diff = new Vec3d(1, 0, 0);
                }
                target = currentPos.add(diff.normalize().multiply(effectiveRange));
                lastAttackerPos = null;
            } else if (gunshotData != null && currentPos.distanceTo(gunshotData.position()) <= effectiveRange) {
                Vec3d diff = currentPos.subtract(gunshotData.position());
                if (diff.lengthSquared() < 1e-4) {
                    diff = new Vec3d(1, 0, 0);
                }
                target = currentPos.add(diff.normalize().multiply(effectiveRange));
            } else if (soundEventPos != null && currentPos.distanceTo(soundEventPos) <= effectiveRange) {
                Vec3d diff = currentPos.subtract(soundEventPos);
                if (diff.lengthSquared() < 1e-4) {
                    diff = new Vec3d(1, 0, 0);
                }
                target = currentPos.add(diff.normalize());
            }
        }

        if (target != null) {
            target = new Vec3d(target.x, mob.getY(), target.z);
            mob.getNavigation().startMovingTo(target.x, target.y, target.z, effectiveSpeed);
        }
    }

    public static void init() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((serverWorld, entity, livingEntity) -> {
            for (ReactToSoundGoal goal : activeGoals) {
                if (goal.mob == entity && !(goal.mob instanceof Monster) && livingEntity.getAttacker() != null) {
                    goal.onHurt(livingEntity.getAttacker().getPos());
                }
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ReactToSoundGoal goal : activeGoals) {
                if (goal.canStart()) {
                    goal.start();
                }
            }
        });
    }
}
