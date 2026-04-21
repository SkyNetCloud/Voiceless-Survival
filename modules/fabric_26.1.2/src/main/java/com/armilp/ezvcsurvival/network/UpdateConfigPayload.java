package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.MobGoalInjector;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import static com.armilp.ezvcsurvival.EZVCSurvival.MOD_ID;

public record UpdateConfigPayload(
        ConfigType configType,
        String  targetId,
        boolean enabled,
        double value1,
        double value2,
        double value3,
        boolean boolValue
) implements CustomPacketPayload {

    public static final Identifier UPDATE_CONFIG_PAYLOAD = Identifier.fromNamespaceAndPath(MOD_ID, "update_config");
    public static final CustomPacketPayload.Type<UpdateConfigPayload> UPDATE_CONFIG_TYPE = new CustomPacketPayload.Type<>(UPDATE_CONFIG_PAYLOAD);
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateConfigPayload> UPDATE_CONFIG_CODEC = new StreamCodec<RegistryFriendlyByteBuf, UpdateConfigPayload>() {
        @Override
        public UpdateConfigPayload decode(RegistryFriendlyByteBuf buffer) {
            ConfigType configType = ConfigType.fromId(ByteBufCodecs.VAR_INT.decode(buffer));
            String targetId = ByteBufCodecs.STRING_UTF8.decode(buffer);
            boolean enabled = ByteBufCodecs.BOOL.decode(buffer);
            double value1 = ByteBufCodecs.DOUBLE.decode(buffer);
            double value2 = ByteBufCodecs.DOUBLE.decode(buffer);
            double value3 = ByteBufCodecs.DOUBLE.decode(buffer);
            boolean boolValue = ByteBufCodecs.BOOL.decode(buffer);

            return new UpdateConfigPayload(configType, targetId, enabled, value1, value2, value3, boolValue);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, UpdateConfigPayload packet) {
            ByteBufCodecs.VAR_INT.encode(buffer, packet.configType().getId());
            ByteBufCodecs.STRING_UTF8.encode(buffer, packet.targetId());
            ByteBufCodecs.BOOL.encode(buffer, packet.enabled());
            ByteBufCodecs.DOUBLE.encode(buffer, packet.value1());
            ByteBufCodecs.DOUBLE.encode(buffer, packet.value2());
            ByteBufCodecs.DOUBLE.encode(buffer, packet.value3());
            ByteBufCodecs.BOOL.encode(buffer, packet.boolValue());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return UPDATE_CONFIG_TYPE;
    }


    public enum ConfigType {
        ENTITY_VOICE(0),
        GENERAL_SOUND(1),
        GENERAL_SOUND_ENTITY(4),
        SOUND_PRIORITY(5);

        private final int id;

        ConfigType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static ConfigType fromId(int id) {
            for (ConfigType type : values()) {
                if (type.id == id) return type;
            }
            return ENTITY_VOICE;
        }
    }

    public static UpdateConfigPayload forEntityVoice(String entityId, boolean enabled, double speed, double range, double threshold) {
        return new UpdateConfigPayload(
                ConfigType.ENTITY_VOICE,
                entityId,
                enabled,
                speed,
                range,
                threshold,
                false
        );
    }

    public static UpdateConfigPayload forGeneralSound(ConfigType type, String soundId, boolean enabled, double speedMultiplier, double rangeMultiplier) {
        return new UpdateConfigPayload(
                type,
                soundId,
                enabled,
                speedMultiplier,
                rangeMultiplier,
                0.0,
                false
        );
    }

    public static UpdateConfigPayload forSoundPriority(ConfigType type, String soundId, boolean enabled, double speedMultiplier, double rangeMultiplier, boolean isPriority) {
        return new UpdateConfigPayload(
                type,
                soundId,
                enabled,
                speedMultiplier,
                rangeMultiplier,
                0.0,
                isPriority
        );
    }


    public static void handle(UpdateConfigPayload packet, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            MinecraftServer server = context.server();
            boolean configChanged = false;

            if ("global".equals(packet.targetId)) {
                handleGlobalConfigChange(packet);
                configChanged = true;
            } else if ("refresh".equals(packet.targetId)) {
                handleRefreshRequest(packet, server);
                configChanged = true;
            } else {
                switch (packet.configType) {
                    case ENTITY_VOICE:
                        handleEntityVoiceConfig(packet);
                        configChanged = true;
                        break;
                    case GENERAL_SOUND:
                        handleGeneralSoundConfig(packet);
                        configChanged = true;
                        break;
                    case GENERAL_SOUND_ENTITY:
                        handleGeneralSoundEntityConfig(packet);
                        configChanged = true;
                        break;
                    case SOUND_PRIORITY:
                        handleSoundPriorityConfig(packet);
                        configChanged = true;
                        break;
                }
            }

            if (configChanged) {
                try {
                    // Reload configs to apply changes
                    switch (packet.configType) {
                        case ENTITY_VOICE:
                            EntityVoiceConfig.init();
                            break;
                        case GENERAL_SOUND:
                        case GENERAL_SOUND_ENTITY:
                        case SOUND_PRIORITY:
                            GeneralSoundsConfig.init();
                            break;
                    }
                    SoundConfig.loadConfigs();

                    if (VoiceConfig.DEBUG.get()) {
                        System.out.println("[EZVCSurvival] Config reload post update: " +
                                packet.configType + " - " + packet.targetId);
                    }


                    MobGoalInjector.refreshAll();
                    if (packet.configType == ConfigType.ENTITY_VOICE ||
                            packet.configType == ConfigType.GENERAL_SOUND_ENTITY) {

                        if (packet.targetId != null && !packet.targetId.isEmpty() &&
                                !"global".equals(packet.targetId) && !"refresh".equals(packet.targetId)) {
                            MobGoalInjector.refreshEntityId(packet.targetId);
                        }
                    }

                } catch (Exception e) {
                    if (VoiceConfig.DEBUG.get()) {
                        System.err.println("[EZVCSurvival] Error reloading configs post update: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private static void handleEntityVoiceConfig(UpdateConfigPayload packet) {
        EntityVoiceConfig.EntityConfig newConfig =
                new EntityVoiceConfig.EntityConfig(packet.enabled(), packet.value1(), packet.value2(), packet.value3());
        EntityVoiceConfig.set(packet.targetId(), newConfig);
        EntityVoiceConfig.persist();
        EntityVoiceConfig.EntityConfig updated = EntityVoiceConfig.get(packet.targetId());
        boolean ok = updated != null &&
                updated.enabled == packet.enabled() &&
                Math.abs(updated.speed - packet.value1()) < 0.001 &&
                Math.abs(updated.range - packet.value2()) < 0.001 &&
                Math.abs(updated.threshold - packet.value3()) < 0.001;
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] EntityVoice update " + packet.targetId() + " ok=" + ok);
        }
    }

    private static void handleGeneralSoundConfig(UpdateConfigPayload packet) {
        if (GeneralSoundsConfig.ROOT == null) GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
        if (GeneralSoundsConfig.ROOT.sounds == null) GeneralSoundsConfig.ROOT.sounds = new java.util.HashMap<>();
        GeneralSoundsConfig.setSoundEntry(packet.targetId(), packet.enabled(), packet.value1(), packet.value2(), packet.boolValue());
        GeneralSoundsConfig.persist();
        GeneralSoundsConfig.SoundEntry updated = GeneralSoundsConfig.getSounds().get(packet.targetId());
        boolean ok = updated != null &&
                updated.enabled == packet.enabled() &&
                Math.abs(updated.speed_multiplier - packet.value1()) < 0.001 &&
                Math.abs(updated.range_multiplier - packet.value2()) < 0.001 &&
                updated.is_priority == packet.boolValue();
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] GeneralSound update " + packet.targetId() + " ok=" + ok);
        }
    }

    private static void handleGeneralSoundEntityConfig(UpdateConfigPayload packet) {
        if (GeneralSoundsConfig.ROOT == null) GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
        if (GeneralSoundsConfig.ROOT.mobs == null) GeneralSoundsConfig.ROOT.mobs = new java.util.HashMap<>();
        GeneralSoundsConfig.setMobReaction(packet.targetId(), packet.enabled(), packet.value1(), packet.value2());
        GeneralSoundsConfig.persist();
        GeneralSoundsConfig.Reaction updated = GeneralSoundsConfig.getMobReactions().get(packet.targetId());
        boolean ok = updated != null &&
                updated.enabled == packet.enabled() &&
                Math.abs(updated.speed - packet.value1()) < 0.001 &&
                Math.abs(updated.range - packet.value2()) < 0.001;
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] GeneralSoundEntity update " + packet.targetId() + " ok=" + ok);
        }
    }

    private static void handleSoundPriorityConfig(UpdateConfigPayload packet) {
        if (GeneralSoundsConfig.ROOT == null) GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
        if (GeneralSoundsConfig.ROOT.sounds == null) GeneralSoundsConfig.ROOT.sounds = new java.util.HashMap<>();
        GeneralSoundsConfig.SoundEntry se = GeneralSoundsConfig.getSounds().get(packet.targetId());
        if (se != null) {
            se.is_priority = packet.boolValue();
            if (packet.value1() != 0.0) se.speed_multiplier = packet.value1();
            if (packet.value2() != 0.0) se.range_multiplier = packet.value2();
        } else {
            GeneralSoundsConfig.setSoundEntry(String.valueOf(packet.targetId()), packet.enabled(),
                    packet.value1() != 0.0 ? packet.value1() : 1.0,
                    packet.value2() != 0.0 ? packet.value2() : 1.0,
                    packet.boolValue());
        }
        GeneralSoundsConfig.persist();
        GeneralSoundsConfig.SoundEntry updated = GeneralSoundsConfig.getSounds().get(packet.targetId());
        boolean ok = updated != null && updated.is_priority == packet.boolValue();
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] SoundPriority update " + packet.targetId() + " ok=" + ok);
        }
    }

    private static void handleGlobalConfigChange(UpdateConfigPayload packet) {
        switch (packet.configType()) {
            case ENTITY_VOICE:
                if (EntityVoiceConfig.ROOT == null) EntityVoiceConfig.ROOT = new EntityVoiceConfig.RootConfig();
                EntityVoiceConfig.ROOT.enabled = packet.enabled();
                EntityVoiceConfig.persist();
                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival] Global EntityVoice enabled=" + packet.enabled());
                }
                break;
            case GENERAL_SOUND:
                if (GeneralSoundsConfig.ROOT == null) GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
                GeneralSoundsConfig.ROOT.enabled = packet.enabled();
                GeneralSoundsConfig.persist();
                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival] Global GeneralSound enabled=" + packet.enabled());
                }
                break;
            case SOUND_PRIORITY:
                GeneralSoundsConfig.enableAllPrioritySounds(packet.enabled());
                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival] Global Priority enabled=" + packet.enabled());
                }
                break;
        }
    }

    private static void handleRefreshRequest(UpdateConfigPayload packet, MinecraftServer server) {
        try {
            GeneralSoundsConfig.init();
            EntityVoiceConfig.init();
            SoundConfig.loadConfigs();
            MobGoalInjector.refreshAll();

            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival] Manual refresh completed");
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error refreshing configs: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}