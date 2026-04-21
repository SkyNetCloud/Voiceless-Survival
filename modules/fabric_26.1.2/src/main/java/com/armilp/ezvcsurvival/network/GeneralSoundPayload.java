package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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

    public static final Identifier GENERAL_SOUND_PAYLOAD = Identifier.fromNamespaceAndPath(MOD_ID, "general_sound");
    public static final CustomPacketPayload.Type<GeneralSoundPayload> GENERAL_SOUND_TYPE = new CustomPacketPayload.Type<>(GENERAL_SOUND_PAYLOAD);



    public static final StreamCodec<RegistryFriendlyByteBuf, GeneralSoundPayload> GENERAL_SOUND_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, GeneralSoundPayload::sound,
            ByteBufCodecs.DOUBLE, GeneralSoundPayload::x,
            ByteBufCodecs.DOUBLE, GeneralSoundPayload::y,
            ByteBufCodecs.DOUBLE, GeneralSoundPayload::z,
            ByteBufCodecs.DOUBLE, GeneralSoundPayload::speedMultiplier,
            ByteBufCodecs.DOUBLE, GeneralSoundPayload::rangeMultiplier,
            GeneralSoundPayload::new
    );


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
                    soundPos.x(),
                    soundPos.y(),
                    soundPos.z(),
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

    public static void registerHandler() {
        ServerPlayNetworking.registerGlobalReceiver(GENERAL_SOUND_TYPE, (payload, context) -> {
            handle(payload, context);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return GENERAL_SOUND_TYPE;
    }
}