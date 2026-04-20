package com.armilp.ezvcsurvival.sculk;

import com.armilp.ezvcsurvival.EZVCSurvival;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


import static com.armilp.ezvcsurvival.sculk.ModGameEvent.VOICE_TALK;

public class SculkVibrationHelper {

    public static void generateVibration(ServerPlayer player, int distance, double audioLevel) {
        if (player == null) {
            return;
        }

        Objects.requireNonNull(player.level().getServer()).execute(() -> {
            try {
                generateVibrationSync(player, distance, audioLevel);
            } catch (Exception e) {
                EZVCSurvival.LOGGER.error("Error generating sculk vibration", e);
            }
        });
    }

    private static void generateVibrationSync(ServerPlayer player, int distance, double audioLevel) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        GameEvent event = VOICE_TALK;
        if (event == null) {
            EZVCSurvival.LOGGER.warn("VOICE_TALK GameEvent not registered yet");
            return;
        }

        BlockPos playerPos = player.getOnPos();
        Vec3 sourcePos = player.position().add(0, 0.5, 0);

        //PreparableModelLoadingPlugin.Holder<GameEvent> eventHolder = Registries.GAME_EVENT.get(event);

        List<SculkSensorBlockEntity> sensors = findNearbySculkSensors(level, playerPos, distance);

        for (SculkSensorBlockEntity sensor : sensors) {
            try {
                VibrationSystem.Listener listener = sensor.getListener();
                if (listener != null) {
                    listener.forceScheduleVibration(
                            level,
                            GameEvent.STEP,
                            GameEvent.Context.of(player),
                            sourcePos
                    );
                }
            } catch (Exception e) {
                EZVCSurvival.LOGGER.error("Failed to activate sensor", e);
            }
        }
    }

    private static List<SculkSensorBlockEntity> findNearbySculkSensors(ServerLevel level, BlockPos center, int distance) {
        List<SculkSensorBlockEntity> sensors = new ArrayList<>();

        BlockPos minPos = new BlockPos(
                center.getX() - distance,
                center.getY() - distance,
                center.getZ() - distance
        );

        BlockPos maxPos = new BlockPos(
                center.getX() + distance,
                center.getY() + distance,
                center.getZ() + distance
        );

        // Use the correct method: BlockPos.iterate()
        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SculkSensorBlockEntity sensor) {
                sensors.add(sensor);
            }
        }

        return sensors;
    }

}