package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
import com.armilp.ezvcsurvival.client.gui.list.ConfigListScreen;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.data.ConfigManager;
import com.armilp.ezvcsurvival.utils.ConfigType;
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

import static com.mojang.text2speech.Narrator.LOGGER;

public final class EZVCNetwork {


    public EZVCNetwork() {}

    public static void registerCommon() {
        LOGGER.info("[EZVCNetwork] Registering common payloads...");

        PayloadTypeRegistry.playS2C().register(GeneralSoundPayload.ID, GeneralSoundPayload.CODEC);
        LOGGER.info("Registered S2C: GeneralSoundPayload");

        PayloadTypeRegistry.playS2C().register(UpdateConfigPayload.ID, UpdateConfigPayload.CODEC);
        LOGGER.info("Registered S2C: UpdateConfigPayload");


        PayloadTypeRegistry.playC2S().register(UpdateConfigPayload.ID, UpdateConfigPayload.CODEC);
        LOGGER.info("Registered C2S: UpdateConfigPayload");

        PayloadTypeRegistry.playC2S().register(GeneralSoundPayload.ID, GeneralSoundPayload.CODEC);
        LOGGER.info("Registered C2S: GeneralSoundPayload");

        ServerPlayNetworking.registerGlobalReceiver(
                UpdateConfigPayload.ID,
                (payload, context) -> {
                    LOGGER.info("Server received UpdateConfigPayload: {}", payload);
                    UpdateConfigPayload.handle(payload, context);
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                GeneralSoundPayload.ID,
                (payload, context) -> {
                    LOGGER.info("Server received GeneralSoundPayload: {}", payload);
                    GeneralSoundPayload.handle(payload, context);
                }
        );

    }

    public static void registerClient() {



        LOGGER.info("[EZVCNetwork] Registering client receivers...");

        ClientPlayNetworking.registerGlobalReceiver(
                GeneralSoundPayload.ID,
                (payload, context) -> {
                    LOGGER.info("Client received GeneralSoundPayload: {}", payload);
                    context.client().execute(() -> handleSoundFromServer(payload));
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                UpdateConfigPayload.ID,
                (payload, context) -> {
                    LOGGER.info("Client received UpdateConfigPayload: {}", payload);

                    context.client().execute(() -> {

                        switch (payload.configType()) {

                            case ENTITY_VOICE -> {
                                EntityVoiceConfig.set(
                                        payload.targetId(),
                                        new EntityVoiceConfig.EntityConfig(
                                                payload.enabled(),
                                                payload.value1(),
                                                payload.value2(),
                                                payload.value3()
                                        )
                                );
                                EntityVoiceConfig.persist();
                            }

                            case GENERAL_SOUND, SOUND_PRIORITY -> {
                                GeneralSoundsConfig.setSoundEntry(
                                        payload.targetId(),
                                        payload.enabled(),
                                        payload.value1(),
                                        payload.value2(),
                                        payload.boolValue()
                                );
                                GeneralSoundsConfig.persist();
                            }

                            case GENERAL_SOUND_ENTITY -> {
                                GeneralSoundsConfig.setMobReaction(
                                        payload.targetId(),
                                        payload.enabled(),
                                        payload.value1(),
                                        payload.value2()
                                );
                                GeneralSoundsConfig.persist();
                            }

                        }

                        if (MinecraftClient.getInstance().currentScreen instanceof ConfigListScreen screen) {
                            LOGGER.info("Refreshing GUI after sync");
                            screen.safeRefresh();
                        }
                    });
                }
        );

    }




    public static void sendGeneralSoundToServer(Identifier soundId, double x, double y, double z,
                                                double speedMultiplier, double rangeMultiplier) {
        ClientPlayNetworking.send(new GeneralSoundPayload(soundId, x, y, z, speedMultiplier, rangeMultiplier));
    }


    public static void sendConfigUpdateToServer(ConfigType configType, String targetId,
                                                boolean enabled, double value1, double value2, double value3, boolean boolValue) {
        ClientPlayNetworking.send(new UpdateConfigPayload(
                configType, targetId, enabled, value1, value2, value3, boolValue
        ));
    }

    public static void sendEntityConfigUpdate(String entityId, boolean enabled, double speed, double range, double threshold) {
        sendConfigUpdateToServer(
                ConfigType.ENTITY_VOICE,
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
                ConfigType.GENERAL_SOUND,
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
                ConfigType.GENERAL_SOUND_ENTITY,
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
                ConfigType.valueOf(configType.toUpperCase()),
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
                ConfigType configType = ConfigType.valueOf(
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
                PositionedSoundInstance.ambient(
                        SoundEvent.of(payload.sound()),
                        (float) payload.speedMultiplier(),
                        (float) payload.rangeMultiplier()
                )
        );
    }

    private static void openConfigEditor() {
        MinecraftClient.getInstance().setScreen(new ConfigEditorScreen());
    }}