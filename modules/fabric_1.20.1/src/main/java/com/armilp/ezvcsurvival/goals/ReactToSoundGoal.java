package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.data.GunshotData;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.event.GunFireListener;
import com.armilp.ezvcsurvival.event.SoundEventTracker;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
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

    private static Vec3d lastPointBlankSoundPos = null;
    private static long lastPointBlankSoundTimestamp = 0;
    private static final long POINT_BLANK_SOUND_EXPIRATION_MS = 5000;

    public ReactToSoundGoal(MobEntity mob, double speed, int range, List<SoundGroupData> soundGroups) {
        this.mob = mob;
        this.speed = speed;
        this.range = range;
        this.soundGroups = soundGroups;
        activeGoals.add(this);
    }

    @Override
    public boolean canStart() {
        if (lastPointBlankSoundPos != null &&
                System.currentTimeMillis() - lastPointBlankSoundTimestamp > POINT_BLANK_SOUND_EXPIRATION_MS) {
            lastPointBlankSoundPos = null;
        }

        Vec3d mobCenterPos = mob.getPos();
        double effectiveRange = range;
        double effectiveSpeed = speed;

        GunshotData gunshotData = GunFireListener.getLastGunshotData();
        if (gunshotData != null) {
            double rangeMultiplier = SoundConfig.getRangeMultiplier(gunshotData.gunType().name().toLowerCase());
            effectiveRange = range * rangeMultiplier;
            double speedMultiplier = SoundConfig.getSpeedMultiplier(gunshotData.gunType().name().toLowerCase());
            effectiveSpeed = speed * speedMultiplier;
        }

        boolean gunshotTriggered = false;
        if (gunshotData != null) {
            double gunDistance = mobCenterPos.distanceTo(gunshotData.position());
            gunshotTriggered = gunDistance <= effectiveRange;
        }

        Vec3d soundEventPos = null;
        double groupSpeedMult = 1.0;
        double groupRangeMult = 1.0;
        outer:
        for (SoundGroupData group : soundGroups) {
            for (String soundStr : group.sounds()) {
                Identifier res = new Identifier(soundStr);
                Vec3d pos = SoundEventTracker.getLastPlayedPositionForSound(res);
                if (pos != null) {
                    soundEventPos = pos;
                    groupSpeedMult = group.speedMultiplier();
                    groupRangeMult = group.rangeMultiplier();
                    break outer;
                }
            }
        }

        boolean soundTriggered = soundEventPos != null && mobCenterPos.distanceTo(soundEventPos) <= (range * groupRangeMult);
        boolean pointBlankTriggered = lastPointBlankSoundPos != null && mobCenterPos.distanceTo(lastPointBlankSoundPos) <= effectiveRange;
        boolean hurtTriggered = lastAttackerPos != null;

        return gunshotTriggered || soundTriggered || pointBlankTriggered || hurtTriggered;
    }

    @Override
    public void start() {
        updateNavigation();
    }

    @Override
    public void tick() {
        updateNavigation();
    }

    @Override
    public void stop() {
        activeGoals.remove(this);
    }

    public void onHurt(Vec3d attackerPos) {
        this.lastAttackerPos = attackerPos;
    }

    private void updateNavigation() {
        if (lastPointBlankSoundPos != null &&
                System.currentTimeMillis() - lastPointBlankSoundTimestamp > POINT_BLANK_SOUND_EXPIRATION_MS) {
            lastPointBlankSoundPos = null;
        }

        Vec3d currentPos = mob.getPos();
        Vec3d target = null;
        double effectiveRange = range;
        double effectiveSpeed = speed;

        GunshotData gunshotData = GunFireListener.getLastGunshotData();
        if (gunshotData != null) {
            double rangeMultiplier = SoundConfig.getRangeMultiplier(gunshotData.gunType().name().toLowerCase());
            effectiveRange = range * rangeMultiplier;
            double speedMultiplier = SoundConfig.getSpeedMultiplier(gunshotData.gunType().name().toLowerCase());
            effectiveSpeed = speed * speedMultiplier;
        }

        Vec3d soundEventPos = null;
        double groupSpeedMult = 1.0;
        double groupRangeMult = 1.0;
        outer:
        for (SoundGroupData group : soundGroups) {
            for (String soundStr : group.sounds()) {
                Identifier res = new Identifier(soundStr);
                Vec3d pos = SoundEventTracker.getLastPlayedPositionForSound(res);
                if (pos != null) {
                    soundEventPos = pos;
                    groupSpeedMult = group.speedMultiplier();
                    groupRangeMult = group.rangeMultiplier();
                    break outer;
                }
            }
        }
        if (soundEventPos != null) {
            effectiveRange = range * groupRangeMult;
            effectiveSpeed = speed * groupSpeedMult;
        }

        if (gunshotData != null && currentPos.distanceTo(gunshotData.position()) <= effectiveRange) {
            target = gunshotData.position();
        } else if (soundEventPos != null && currentPos.distanceTo(soundEventPos) <= effectiveRange) {
            target = soundEventPos;
        } else if (lastPointBlankSoundPos != null && currentPos.distanceTo(lastPointBlankSoundPos) <= effectiveRange) {
            target = lastPointBlankSoundPos;
        }

        if (target != null) {
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
