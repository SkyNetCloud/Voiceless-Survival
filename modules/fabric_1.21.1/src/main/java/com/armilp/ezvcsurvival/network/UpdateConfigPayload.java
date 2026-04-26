package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.MobGoalInjector;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

public record UpdateConfigPayload(
        ConfigType configType,
        String targetId,
        boolean enabled,
        double value1,
        double value2,
        double value3,
        boolean boolValue
) implements CustomPayload {

    public static final CustomPayload.Id<UpdateConfigPayload> PACKET_ID =
            new CustomPayload.Id<>(Identifier.of("ezvcsurvival", "update_config"));

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

    public static final PacketCodec<RegistryByteBuf, UpdateConfigPayload> CODEC = PacketCodec.of(
            (packet, buf) -> {
                buf.writeInt(packet.configType.getId());
                buf.writeString(packet.targetId);
                buf.writeBoolean(packet.enabled);
                buf.writeDouble(packet.value1);
                buf.writeDouble(packet.value2);
                buf.writeDouble(packet.value3);
                buf.writeBoolean(packet.boolValue);
            },
            buf -> {
                ConfigType type = ConfigType.fromId(buf.readInt());
                String targetId = buf.readString();
                boolean enabled = buf.readBoolean();
                double value1 = buf.readDouble();
                double value2 = buf.readDouble();
                double value3 = buf.readDouble();
                boolean boolValue = buf.readBoolean();

                return new UpdateConfigPayload(type, targetId, enabled, value1, value2, value3, boolValue);
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
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
            GeneralSoundsConfig.setSoundEntry(packet.targetId(), packet.enabled(),
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