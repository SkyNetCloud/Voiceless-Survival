package com.armilp.ezvcsurvival.sculk;
//
//import com.armilp.ezvcsurvival.EZVCSurvival;
//import de.maxhenkel.voicechat.api.ServerLevel;
//import de.maxhenkel.voicechat.api.ServerPlayer;
//import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
//import net.minecraft.block.entity.BlockEntity;
//import net.minecraft.block.entity.SculkSensorBlockEntity;
//import net.minecraft.registry.Registries;
//import net.minecraft.server.network.ServerPlayerEntity;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.world.event.GameEvent;
//
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//
//public class SculkVibrationHelper {
//
//    public static void generateVibration(ServerPlayer player, int distance, double audioLevel) {
//        if (player == null) {
//            return;
//        }
//
//        Objects.requireNonNull(player.getServer()).execute(() -> {
//            try {
//                generateVibrationSync(player, distance, audioLevel);
//            } catch (Exception e) {
//                EZVCSurvival.LOGGER.error("Error generating sculk vibration", e);
//            }
//        });
//    }
//
//    private static void generateVibrationSync(ServerPlayerEntity player, int distance, double audioLevel) {
//        if (!(player.level() instanceof ServerLevel level)) {
//            return;
//        }
//
//        GameEvent event = ModGameEvent.VOICE_TALK.get();
//        if (event == null) {
//            EZVCSurvival.LOGGER.warn("VOICE_TALK GameEvent not registered yet");
//            return;
//        }
//
//        BlockPos playerPos = player.blockPosition();
//        Vec3 sourcePos = player.position().add(0, 0.5, 0);
//
//        PreparableModelLoadingPlugin.Holder<GameEvent> eventHolder = Registries.GAME_EVENT.wrapAsHolder(event);
//
//        List<SculkSensorBlockEntity> sensors = findNearbySculkSensors(level, playerPos, distance);
//
//        for (SculkSensorBlockEntity sensor : sensors) {
//            try {
//                VibrationSystem.Listener listener = sensor.getListener();
//                if (listener != null) {
//                    listener.forceScheduleVibration(
//                            level,
//                            eventHolder,
//                            new GameEvent.Context(player, null),
//                            sourcePos
//                    );
//                }
//            } catch (Exception e) {
//                EZVCSurvival.LOGGER.error("Failed to activate sensor", e);
//            }
//        }
//    }
//
//    private static List<SculkSensorBlockEntity> findNearbySculkSensors(ServerLevel level, BlockPos center, int distance) {
//        List<SculkSensorBlockEntity> sensors = new ArrayList<>();
//
//        for (BlockPos pos : BlockPos.betweenClosed(
//                center.offset(-distance, -distance, -distance),
//                center.offset(distance, distance, distance)
//        )) {
//            BlockEntity be = level.getBlockEntity(pos.immutable());
//            if (be instanceof SculkSensorBlockEntity sensor) {
//                sensors.add(sensor);
//            }
//        }
//
//        return sensors;
//    }
//}