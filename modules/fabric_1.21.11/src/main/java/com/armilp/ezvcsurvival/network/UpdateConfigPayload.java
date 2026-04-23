package com.armilp.ezvcsurvival.network;


import com.armilp.ezvcsurvival.config.*;
import com.armilp.ezvcsurvival.goals.MobGoalInjector;
import com.armilp.ezvcsurvival.utils.ConfigType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.util.Identifier;

import static com.armilp.ezvcsurvival.client.gui.edit.ConfigEditScreen.EditType.GUNFIRE_ENTITY;


public record UpdateConfigPayload(
        ConfigType configType,
        String targetId,
        boolean enabled,
        double value1,
        double value2,
        double value3,
        boolean boolValue
) implements CustomPayload {

    public static final CustomPayload.Id<UpdateConfigPayload> ID =
            new CustomPayload.Id<>(Identifier.of("ezvcsurvival", "update_config"));

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
        return ID;
    }


    public static void handle(UpdateConfigPayload msg, ServerPlayNetworking.Context ctx) {
        boolean configChanged = false;
        if ("global".equals(msg.targetId)) {
            handleGlobalConfigChange(msg);
            configChanged = true;
        } else if ("refresh".equals(msg.targetId)) {
            handleRefreshRequest(msg);
            configChanged = true;
        } else {
            switch (msg.configType) {
                case ENTITY_VOICE:
                    handleEntityVoiceConfig(msg);
                    configChanged = true;
                    break;
                case GENERAL_SOUND:
                    handleGeneralSoundConfig(msg);
                    configChanged = true;
                    break;
                case GENERAL_SOUND_ENTITY:
                    handleGeneralSoundEntityConfig(msg);
                    configChanged = true;
                    break;
                case SOUND_PRIORITY:
                    handleSoundPriorityConfig(msg);
                    configChanged = true;
                    break;
                case GUNFIRE_SOUND:
                case GUNFIRE_ENTITY:
                    GunfireConfig.init();
                    break;

            }
        }
        if (configChanged) {
            try {
                switch (msg.configType) {
                    case ENTITY_VOICE:
                        EntityVoiceConfig.init();
                        break;
                    case GENERAL_SOUND:
                    case GENERAL_SOUND_ENTITY:
                    case SOUND_PRIORITY:
                        GeneralSoundsConfig.init();
                        break;
                    case GUNFIRE_SOUND:
                    case GUNFIRE_ENTITY:
                        GunfireConfig.init();
                        break;
                }
                SoundConfig.loadConfigs();
                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival] Config reload post update: " + msg.configType + " - " + msg.targetId);
                }
            } catch (Exception e) {
                if (VoiceConfig.DEBUG.get()) {
                    System.err.println("[EZVCSurvival] Error reloading configs post update: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            MobGoalInjector.refreshAll();
            if (msg.configType == ConfigType.ENTITY_VOICE ||
                    msg.configType == ConfigType.GENERAL_SOUND_ENTITY ||
                    msg.configType == ConfigType.SOUND_PRIORITY) {
                MobGoalInjector.refreshEntityId(msg.targetId);
            }
            ServerPlayNetworking.send(ctx.player(), new UpdateConfigPayload(
                    msg.configType, msg.targetId, msg.enabled, msg.value1, msg.value2, msg.value3, msg.boolValue
            ));
        }
    }

    private static void handleEntityVoiceConfig(UpdateConfigPayload msg) {
        EntityVoiceConfig.EntityConfig newConfig =
                new EntityVoiceConfig.EntityConfig(msg.enabled, msg.value1, msg.value2, msg.value3);
        EntityVoiceConfig.set(msg.targetId, newConfig);
        EntityVoiceConfig.persist();
        EntityVoiceConfig.EntityConfig updated = EntityVoiceConfig.get(msg.targetId);
        boolean ok = updated != null &&
                updated.enabled == msg.enabled &&
                Math.abs(updated.speed - msg.value1) < 0.001 &&
                Math.abs(updated.range - msg.value2) < 0.001 &&
                Math.abs(updated.threshold - msg.value3) < 0.001;
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] EntityVoice update " + msg.targetId + " ok=" + ok);
        }
    }

    private static void handleGeneralSoundConfig(UpdateConfigPayload msg) {
        if (GeneralSoundsConfig.ROOT == null) GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
        if (GeneralSoundsConfig.ROOT.sounds == null) GeneralSoundsConfig.ROOT.sounds = new java.util.HashMap<>();
        GeneralSoundsConfig.setSoundEntry(msg.targetId, msg.enabled, msg.value1, msg.value2, msg.boolValue);
        GeneralSoundsConfig.persist();
        GeneralSoundsConfig.SoundEntry updated = GeneralSoundsConfig.getSounds().get(msg.targetId);
        boolean ok = updated != null &&
                updated.enabled == msg.enabled &&
                Math.abs(updated.speed_multiplier - msg.value1) < 0.001 &&
                Math.abs(updated.range_multiplier - msg.value2) < 0.001 &&
                updated.is_priority == msg.boolValue;
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] GeneralSound update " + msg.targetId + " ok=" + ok);
        }
    }

    private static void handleGeneralSoundEntityConfig(UpdateConfigPayload msg) {
        if (GeneralSoundsConfig.ROOT == null) GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
        if (GeneralSoundsConfig.ROOT.mobs == null) GeneralSoundsConfig.ROOT.mobs = new java.util.HashMap<>();
        GeneralSoundsConfig.setMobReaction(msg.targetId, msg.enabled, msg.value1, msg.value2);
        GeneralSoundsConfig.persist();
        GeneralSoundsConfig.Reaction updated = GeneralSoundsConfig.getMobReactions().get(msg.targetId);
        boolean ok = updated != null &&
                updated.enabled == msg.enabled &&
                Math.abs(updated.speed - msg.value1) < 0.001 &&
                Math.abs(updated.range - msg.value2) < 0.001;
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] GeneralSoundEntity update " + msg.targetId + " ok=" + ok);
        }
    }

    private static void handleSoundPriorityConfig(UpdateConfigPayload msg) {
        if (GeneralSoundsConfig.ROOT == null) GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
        if (GeneralSoundsConfig.ROOT.sounds == null) GeneralSoundsConfig.ROOT.sounds = new java.util.HashMap<>();
        GeneralSoundsConfig.SoundEntry se = GeneralSoundsConfig.getSounds().get(msg.targetId);
        if (se != null) {
            se.is_priority = msg.boolValue;
            if (msg.value1 != 0.0) se.speed_multiplier = msg.value1;
            if (msg.value2 != 0.0) se.range_multiplier = msg.value2;
        } else {
            GeneralSoundsConfig.setSoundEntry(msg.targetId, msg.enabled,
                    msg.value1 != 0.0 ? msg.value1 : 1.0,
                    msg.value2 != 0.0 ? msg.value2 : 1.0,
                    msg.boolValue);
        }
        GeneralSoundsConfig.persist();
        GeneralSoundsConfig.SoundEntry updated = GeneralSoundsConfig.getSounds().get(msg.targetId);
        boolean ok = updated != null && updated.is_priority == msg.boolValue;
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] SoundPriority update " + msg.targetId + " ok=" + ok);
        }
    }

    private static void handleGlobalConfigChange(UpdateConfigPayload msg) {
        switch (msg.configType) {
            case ENTITY_VOICE:
                if (EntityVoiceConfig.ROOT == null) {
                    EntityVoiceConfig.ROOT = new EntityVoiceConfig.RootConfig();
                }
                EntityVoiceConfig.ROOT.enabled = msg.enabled;
                EntityVoiceConfig.persist();

                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival] Server updated EntityVoice global config: enabled=" + msg.enabled);
                }
                break;
            case GENERAL_SOUND:
                if (GeneralSoundsConfig.ROOT == null) {
                    GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
                }
                GeneralSoundsConfig.ROOT.enabled = msg.enabled;
                GeneralSoundsConfig.persist();

                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival] Server updated GeneralSound global config: enabled=" + msg.enabled);
                }
                break;
            case SOUND_PRIORITY:
                GeneralSoundsConfig.enableAllPrioritySounds(msg.enabled);

                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival] Server updated Sound Priority global config: enabled=" + msg.enabled);
                }
                break;
            case GUNFIRE_SOUND:
                if (GunfireConfig.ROOT == null) {
                    GunfireConfig.ROOT = new GunfireConfig.Root();
                }
                GunfireConfig.ROOT.enabled = msg.enabled;
                GunfireConfig.persist();

                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival] Server updated Gunfire global config: enabled=" + msg.enabled);
                }
                break;
        }
    }

    private static void handleRefreshRequest(UpdateConfigPayload msg) {
        try {
            GeneralSoundsConfig.init();
            EntityVoiceConfig.init();
            SoundConfig.loadConfigs();
            MobGoalInjector.refreshAll();
            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival] Manual refresh");
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error refreshing configs: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


}