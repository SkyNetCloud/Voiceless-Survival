package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;


import static com.armilp.ezvcsurvival.EZVCSurvival.MOD_ID;

public record GeneralSoundPayload(
        Identifier sound,
        double x,
        double y,
        double z,
        double speedMultiplier,
        double rangeMultiplier
) implements CustomPacketPayload {
    public static final Identifier PAYLOAD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "general_sound");

    public static final CustomPacketPayload.Type<GeneralSoundPayload> ID =
            new CustomPacketPayload.Type<>(PAYLOAD_ID);


//
//    public static final StreamCodec<FriendlyByteBuf, GeneralSoundPayload> CODEC = PacketCodec.tuple(
//            Identifier.PACKET_CODEC, GeneralSoundPayload::sound,
//            Packe.DOUBLE, GeneralSoundPayload::x,
//            PacketCodecs.DOUBLE, GeneralSoundPayload::y,
//            PacketCodecs.DOUBLE, GeneralSoundPayload::z,
//            PacketCodecs.DOUBLE, GeneralSoundPayload::speedMultiplier,
//            PacketCodecs.DOUBLE, GeneralSoundPayload::rangeMultiplier,
//            GeneralSoundPayload::new
//    );


    // Handler method that can be registered with Fabric's networking
    public static void handle(GeneralSoundPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        if (player == null) return;

        // Run on server thread
        context.server().execute(() -> {
            ServerLevel level = player.level();
            Vec3 soundPos = new Vec3(payload.x(), payload.y(), payload.z());

            SoundEventTracker.setLastPlayedPosition(

                    payload.sound(),
                    soundPos.x(),
                    soundPos.y(),
                    soundPos.z(),
                    payload.speedMultiplier(),
                    payload.rangeMultiplier()
            );

            SoundEventTracker.notifyNearbyMobs(
                    level,
                    payload.sound(),
                    soundPos.getX(),
                    soundPos.getY(),
                    soundPos.getZ(),
                    payload.speedMultiplier(),
                    payload.rangeMultiplier()
            );

            var soundMap = GeneralSoundsConfig.getSounds();
            if (soundMap != null) {
                GeneralSoundsConfig.SoundEntry cfg = soundMap.get(payload.sound().toString());
                if (cfg != null && cfg.is_priority) {
                    ReactToGeneralSoundGoal.setPrioritySound(soundPos);
                }
            }
        });
    }

    // Alternative handler that can be used directly with ServerPlayNetworking.registerGlobalReceiver
    public static void registerHandler() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            handle(payload, context);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}