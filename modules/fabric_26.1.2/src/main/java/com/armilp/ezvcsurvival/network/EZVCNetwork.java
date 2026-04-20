package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
import com.armilp.ezvcsurvival.data.ConfigManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class EZVCNetwork {


    public EZVCNetwork() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(GeneralSoundPayload.ID, GeneralSoundPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenConfigEditorPayload.ID, OpenConfigEditorPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateConfigPayload.PACKET_ID, UpdateConfigPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GeneralSoundPayload.ID, GeneralSoundPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(UpdateConfigPayload.PACKET_ID,
                (payload, context) -> {
                    ServerPlayerEntity player = context.player();
                    context.server().execute(() -> {
                        ConfigManager.updateConfig(player, payload);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(GeneralSoundPayload.ID,
                (payload, context) -> {
                    ServerPlayerEntity player = context.player();
                    context.server().execute(() -> {
                        GeneralSoundPayload.handle(payload, context);
                    });
                });
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(GeneralSoundPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        handleSoundFromServer(payload);
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(OpenConfigEditorPayload.ID,
                (payload, context) -> {
                    context.client().execute(EZVCNetwork::openConfigEditor);
                });
    }

    public static void sendSoundToClient(ServerPlayerEntity player, Identifier soundId, float volume, float pitch) {
        ServerPlayNetworking.send(player, new GeneralSoundPayload(
                soundId, player.getX(), player.getY(), player.getZ(), pitch, volume
        ));
    }

    public static void sendSoundToAllTracking(ServerWorld world, BlockPos pos, Identifier soundId, float volume, float pitch) {
        GeneralSoundPayload payload = new GeneralSoundPayload(soundId,
                pos.getX(), pos.getY(), pos.getZ(), pitch, volume);

        for (ServerPlayerEntity player : PlayerLookup.tracking(world, pos)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void sendGeneralSoundToServer(Identifier soundId, double x, double y, double z,
                                                double speedMultiplier, double rangeMultiplier) {
        ClientPlayNetworking.send(new GeneralSoundPayload(soundId, x, y, z, speedMultiplier, rangeMultiplier));
    }

    public static void sendConfigEditorToClient(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new OpenConfigEditorPayload());
    }

    public static void sendConfigUpdateToServer(UpdateConfigPayload.ConfigType configType, String targetId,
                                                boolean enabled, double value1, double value2, double value3, boolean boolValue) {
        ClientPlayNetworking.send(new UpdateConfigPayload(
                configType, targetId, enabled, value1, value2, value3, boolValue
        ));
    }

    public static void sendEntityConfigUpdate(String entityId, boolean enabled, double speed, double range, double threshold) {
        sendConfigUpdateToServer(
                UpdateConfigPayload.ConfigType.ENTITY_VOICE,
                entityId,
                enabled,
                speed,
                range,
                threshold,
                false
        );
    }

    public static void sendGeneralSoundConfigUpdate(String soundId, boolean enabled, double speedMult, double rangeMult, boolean isPriority) {
        sendConfigUpdateToServer(
                UpdateConfigPayload.ConfigType.GENERAL_SOUND,
                soundId,
                enabled,
                speedMult,
                rangeMult,
                0.0,
                isPriority
        );
    }

    public static void sendGeneralSoundEntityUpdate(String entityId, boolean enabled, double speed, double range) {
        sendConfigUpdateToServer(
                UpdateConfigPayload.ConfigType.GENERAL_SOUND_ENTITY,
                entityId,
                enabled,
                speed,
                range,
                0.0,
                false
        );
    }

    public static void sendRefreshRequest(String configType) {
        sendConfigUpdateToServer(
                UpdateConfigPayload.ConfigType.valueOf(configType.toUpperCase()),
                "refresh",
                true,
                1.0,
                1.0,
                1.0,
                false
        );
    }

    public static void sendSimpleConfigUpdateToServer(String key, String value) {
        String[] parts = key.split(":");
        if (parts.length >= 2) {
            String configTypeStr = parts[0];
            String targetId = parts[1];

            try {
                UpdateConfigPayload.ConfigType configType = UpdateConfigPayload.ConfigType.valueOf(
                        configTypeStr.toUpperCase()
                );

                boolean enabled = value.contains("enabled=true");
                double speed = 1.0;
                double range = 1.0;
                double threshold = 0.0;
                boolean priority = false;

                String[] params = value.split(",");
                for (String param : params) {
                    String[] kv = param.split("=");
                    if (kv.length == 2) {
                        switch (kv[0]) {
                            case "speed": speed = Double.parseDouble(kv[1]); break;
                            case "range": range = Double.parseDouble(kv[1]); break;
                            case "threshold": threshold = Double.parseDouble(kv[1]); break;
                            case "priority": priority = Boolean.parseBoolean(kv[1]); break;
                        }
                    }
                }

                sendConfigUpdateToServer(
                        configType, targetId, enabled, speed, range, threshold, priority
                );
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid config type: " + configTypeStr);
            }
        }
    }

    private static void handleSoundFromServer(GeneralSoundPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.getSoundManager().play(
                PositionedSoundInstance.ambient(SoundEvent.of(payload.sound()),
                        (float)payload.speedMultiplier(),
                        (float)payload.rangeMultiplier()
                )
        );
    }

    private static void openConfigEditor() {
        MinecraftClient.getInstance().setScreen(new ConfigEditorScreen());
    }
}