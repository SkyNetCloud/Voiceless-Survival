package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
import com.armilp.ezvcsurvival.data.ConfigManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;

public final class EZVCNetwork {
    public static final int PROTOCOL_VERSION = 1;


    public EZVCNetwork() {}




    public static void registerCommon() {

        PayloadTypeRegistry.clientboundPlay().register(GeneralSoundPayload.GENERAL_SOUND_TYPE, GeneralSoundPayload.GENERAL_SOUND_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenConfigEditorPayload.OPEN_CONFIG_EDITOR_TYPE, OpenConfigEditorPayload.OPEN_CONFIG_EDITOR_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UpdateConfigPayload.UPDATE_CONFIG_TYPE, UpdateConfigPayload.UPDATE_CONFIG_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GeneralSoundPayload.GENERAL_SOUND_TYPE, GeneralSoundPayload.GENERAL_SOUND_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(UpdateConfigPayload.UPDATE_CONFIG_TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();
                    context.server().execute(() -> {
                        ConfigManager.updateConfig(player, payload);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(GeneralSoundPayload.GENERAL_SOUND_TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();
                    context.server().execute(() -> {
                        GeneralSoundPayload.handle(payload, context);
                    });
                });
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(GeneralSoundPayload.GENERAL_SOUND_TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        handleSoundFromServer(payload);
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(OpenConfigEditorPayload.OPEN_CONFIG_EDITOR_TYPE,
                (payload, context) -> {
                    context.client().execute(EZVCNetwork::openConfigEditor);
                });
    }

    public static void sendSoundToClient(ServerPlayer player, Identifier soundId, float volume, float pitch) {
        ServerPlayNetworking.send(player, new GeneralSoundPayload(
                soundId, player.getX(), player.getY(), player.getZ(), pitch, volume
        ));
    }

    public static void sendSoundToAllTracking(ServerLevel world, BlockPos pos, Identifier soundId, float volume, float pitch) {
        GeneralSoundPayload payload = new GeneralSoundPayload(soundId,
                pos.getX(), pos.getY(), pos.getZ(), pitch, volume);

        for (ServerPlayer player : PlayerLookup.tracking(world, pos)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void sendGeneralSoundToServer(Identifier soundId, double x, double y, double z,
                                                double speedMultiplier, double rangeMultiplier) {
        ClientPlayNetworking.send(new GeneralSoundPayload(soundId, x, y, z, speedMultiplier, rangeMultiplier));
    }

    public static void sendConfigEditorToClient(ServerPlayer player) {
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
        Minecraft client = Minecraft.getInstance();
        client.getSoundManager().play(
                SimpleSoundInstance.forLocalAmbience(SoundEvent.createVariableRangeEvent(payload.sound()),
                        (float)payload.speedMultiplier(),
                        (float)payload.rangeMultiplier()
                )
        );
    }

    private static void openConfigEditor() {
        Minecraft.getInstance().setScreen(new ConfigEditorScreen());
    }
}