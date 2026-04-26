package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public record GeneralSoundPayload(
        Identifier sound,
        double x,
        double y,
        double z,
        double speedMultiplier,
        double rangeMultiplier
) implements CustomPayload {
    public static final CustomPayload.Id<GeneralSoundPayload> ID =
            new CustomPayload.Id<>(Identifier.of("ezvcsurvival", "general_sound"));

    public static final PacketCodec<RegistryByteBuf, GeneralSoundPayload> CODEC = PacketCodec.tuple(
            Identifier.PACKET_CODEC, GeneralSoundPayload::sound,
            PacketCodecs.DOUBLE, GeneralSoundPayload::x,
            PacketCodecs.DOUBLE, GeneralSoundPayload::y,
            PacketCodecs.DOUBLE, GeneralSoundPayload::z,
            PacketCodecs.DOUBLE, GeneralSoundPayload::speedMultiplier,
            PacketCodecs.DOUBLE, GeneralSoundPayload::rangeMultiplier,
            GeneralSoundPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    // Handler method that can be registered with Fabric's networking
    public static void handle(GeneralSoundPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        if (player == null) return;

        // Run on server thread
        context.server().execute(() -> {
            ServerWorld level = player.getServerWorld();
            Vec3d soundPos = new Vec3d(payload.x(), payload.y(), payload.z());

            SoundEventTracker.setLastPlayedPosition(
                    payload.sound(),
                    soundPos.getX(),
                    soundPos.getY(),
                    soundPos.getZ(),
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
}