package com.armilp.ezvcsurvival.sculk;

import com.armilp.ezvcsurvival.EZVCSurvival;
import de.maxhenkel.voicechat.api.ServerLevel;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SculkSensorBlockEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.Vibrations;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


import static com.armilp.ezvcsurvival.sculk.ModGameEvent.VOICE_TALK;

public class SculkVibrationHelper {

    public static void generateVibration(ServerPlayerEntity player, int distance, double audioLevel) {
        if (player == null) {
            return;
        }

        Objects.requireNonNull(player.getEntityWorld().getServer()).execute(() -> {
            try {
                generateVibrationSync(player, distance, audioLevel);
            } catch (Exception e) {
                EZVCSurvival.LOGGER.error("Error generating sculk vibration", e);
            }
        });
    }

    private static void generateVibrationSync(ServerPlayerEntity player, int distance, double audioLevel) {
        if (!(player.getEntityWorld() instanceof ServerWorld level)) {
            return;
        }

        GameEvent event = VOICE_TALK;
        if (event == null) {
            EZVCSurvival.LOGGER.warn("VOICE_TALK GameEvent not registered yet");
            return;
        }

        BlockPos playerPos = player.getBlockPos();
        Vec3d sourcePos = player.getEntityPos().add(0, 0.5, 0);

        //PreparableModelLoadingPlugin.Holder<GameEvent> eventHolder = Registries.GAME_EVENT.get(event);

        List<SculkSensorBlockEntity> sensors = findNearbySculkSensors(level, playerPos, distance);

        for (SculkSensorBlockEntity sensor : sensors) {
            try {
                Vibrations.VibrationListener listener = sensor.getEventListener();
                if (listener != null) {
                    listener.forceListen(
                            level,
                            GameEvent.STEP,
                            GameEvent.Emitter.of(player),
                            sourcePos
                    );
                }
            } catch (Exception e) {
                EZVCSurvival.LOGGER.error("Failed to activate sensor", e);
            }
        }
    }

    private static List<SculkSensorBlockEntity> findNearbySculkSensors(ServerWorld level, BlockPos center, int distance) {
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
        for (BlockPos pos : BlockPos.iterate(minPos, maxPos)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SculkSensorBlockEntity sensor) {
                sensors.add(sensor);
            }
        }

        return sensors;
    }

}