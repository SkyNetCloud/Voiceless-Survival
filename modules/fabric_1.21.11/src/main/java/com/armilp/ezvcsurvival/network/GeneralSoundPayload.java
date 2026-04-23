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

    public static void handle(GeneralSoundPayload payload, ServerPlayNetworking.Context ctx) {
        SoundEventTracker.setLastPlayedPosition(
                payload.sound,
                payload.x,
                payload.y,
                payload.z,
                payload.speedMultiplier,
                payload.rangeMultiplier
        );
    }

}