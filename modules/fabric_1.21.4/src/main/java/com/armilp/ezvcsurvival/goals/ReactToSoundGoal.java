package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.event.GunFireListener;
import com.armilp.ezvcsurvival.event.SoundEventTracker;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReactToSoundGoal extends Goal {

    private final MobEntity mob;
    private final double speed;
    private final int range;
    private final List<String> soundIds;
    private Vec3d lastAttackerPos = null;

    private static final List<ReactToSoundGoal> activeGoals = new CopyOnWriteArrayList<>();

    public ReactToSoundGoal(MobEntity mob, double speed, int range, List<String> soundIds) {
        this.mob = mob;
        this.speed = speed;
        this.range = range;
        this.soundIds = soundIds;
        activeGoals.add(this);
    }

    @Override
    public boolean canStart() {
        Vec3d mobPos = mob.getPos();
        Vec3d soundPosition = SoundEventTracker.getLastPlayedPositionForAny(soundIds);
        Vec3d gunshotPosition = GunFireListener.getLastGunshotPosition();

        boolean soundTriggered = soundPosition != null && soundPosition.squaredDistanceTo(mobPos) <= this.range * this.range;
        boolean hurtTriggered = !(mob instanceof Monster) && lastAttackerPos != null;
        boolean gunshotDetected = gunshotPosition != null && gunshotPosition.squaredDistanceTo(mobPos) <= this.range * this.range;

        return soundTriggered || hurtTriggered || gunshotDetected;
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

        if (mob instanceof Monster) {
            if (mob.getTarget() != null && mob.getTarget() instanceof PlayerEntity) {
                return;
            }

            Vec3d gunshotPosition = GunFireListener.getLastGunshotPosition();
            if (gunshotPosition != null) {
                target = new Vec3d(gunshotPosition.x, mob.getY(), gunshotPosition.z);
            }

            Vec3d soundPosition = SoundEventTracker.getLastPlayedPositionForAny(soundIds);
            if (soundPosition != null) {
                target = new Vec3d(soundPosition.x, mob.getY(), soundPosition.z);
            }
        } else {
            if (lastAttackerPos != null) {
                Vec3d diff = currentPos.subtract(lastAttackerPos);
                if (diff.lengthSquared() < 1e-4) {
                    diff = new Vec3d(1, 0, 0);
                }
                target = currentPos.add(diff.normalize().multiply(range));
                lastAttackerPos = null;
            } else {
                Vec3d soundPosition = SoundEventTracker.getLastPlayedPositionForAny(soundIds);
                if (soundPosition != null) {
                    Vec3d diff = currentPos.subtract(soundPosition);
                    if (diff.lengthSquared() < 1e-4) {
                        diff = new Vec3d(1, 0, 0);
                    }
                    target = currentPos.add(diff);
                }
            }

            if (target != null) {
                target = new Vec3d(target.x, mob.getY(), target.z);
            }
        }

        if (target != null) {
            mob.getNavigation().startMovingTo(target.x, target.y, target.z, speed);
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
