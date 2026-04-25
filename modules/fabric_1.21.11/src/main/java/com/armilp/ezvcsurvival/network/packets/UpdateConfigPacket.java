package com.armilp.ezvcsurvival.network.packets;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.MobGoalInjector;
import com.armilp.ezvcsurvival.network.EZVCNetworkService;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

public class UpdateConfigPacket {

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

    public static final PacketCodec<RegistryByteBuf, UpdateConfigPacket> STREAM_CODEC =
            PacketCodec.of(UpdateConfigPacket::encode, UpdateConfigPacket::decode);

    private final ConfigType configType;
    private final String targetId;
    private final boolean enabled;
    private final double value1;
    private final double value2;
    private final double value3;
    private final boolean boolValue;

    public UpdateConfigPacket(String entityId, boolean enabled, double speed, double range, double threshold) {
        this.configType = ConfigType.ENTITY_VOICE;
        this.targetId = entityId;
        this.enabled = enabled;
        this.value1 = speed;
        this.value2 = range;
        this.value3 = threshold;
        this.boolValue = false;
    }

    public UpdateConfigPacket(ConfigType type, String soundId, boolean enabled, double speedMultiplier, double rangeMultiplier) {
        this.configType = type;
        this.targetId = soundId;
        this.enabled = enabled;
        this.value1 = speedMultiplier;
        this.value2 = rangeMultiplier;
        this.value3 = 0.0;
        this.boolValue = false;
    }

    public UpdateConfigPacket(ConfigType type, String soundId, boolean enabled, double speedMultiplier, double rangeMultiplier, boolean isPriority) {
        this.configType = type;
        this.targetId = soundId;
        this.enabled = enabled;
        this.value1 = speedMultiplier;
        this.value2 = rangeMultiplier;
        this.value3 = 0.0;
        this.boolValue = isPriority;
    }

    public static void encode(UpdateConfigPacket msg, RegistryByteBuf buf) {
        buf.writeInt(msg.configType.getId());
        buf.writeString(msg.targetId);
        buf.writeBoolean(msg.enabled);
        buf.writeDouble(msg.value1);
        buf.writeDouble(msg.value2);
        buf.writeDouble(msg.value3);
        buf.writeBoolean(msg.boolValue);
    }

    public static UpdateConfigPacket decode(RegistryByteBuf buf) {
        ConfigType type = ConfigType.fromId(buf.readInt());
        String targetId = buf.readString();
        boolean enabled = buf.readBoolean();
        double value1 = buf.readDouble();
        double value2 = buf.readDouble();
        double value3 = buf.readDouble();
        boolean boolValue = buf.readBoolean();

        return switch (type) {
            case ENTITY_VOICE -> new UpdateConfigPacket(targetId, enabled, value1, value2, value3);
            case GENERAL_SOUND ->
                    new UpdateConfigPacket(ConfigType.GENERAL_SOUND, targetId, enabled, value1, value2, boolValue);
            case GENERAL_SOUND_ENTITY ->
                    new UpdateConfigPacket(ConfigType.GENERAL_SOUND_ENTITY, targetId, enabled, value1, value2);
            case SOUND_PRIORITY ->
                    new UpdateConfigPacket(ConfigType.SOUND_PRIORITY, targetId, enabled, value1, value2, boolValue);
        };
    }

    public static void handle(UpdateConfigPacket msg, EZVCNetworkService.MessageContext ctx) {
        ctx.enqueueWork(() -> {
            boolean configChanged = false;

            if ("global".equals(msg.targetId)) {
                handleGlobalConfigChange(msg);
                configChanged = true;
            } else if ("refresh".equals(msg.targetId)) {
                handleRefreshRequest();
                configChanged = true;
            } else {
                switch (msg.configType) {
                    case ENTITY_VOICE -> {
                        handleEntityVoiceConfig(msg);
                        configChanged = true;
                    }
                    case GENERAL_SOUND -> {
                        handleGeneralSoundConfig(msg);
                        configChanged = true;
                    }
                    case GENERAL_SOUND_ENTITY -> {
                        handleGeneralSoundEntityConfig(msg);
                        configChanged = true;
                    }
                    case SOUND_PRIORITY -> {
                        handleSoundPriorityConfig(msg);
                        configChanged = true;
                    }
                }
            }

            if (configChanged) {
                reloadConfigs(msg);
            }

            ctx.setPacketHandled(true);
        });
    }

    private static void reloadConfigs(UpdateConfigPacket msg) {
        try {
            switch (msg.configType) {
                case ENTITY_VOICE -> EntityVoiceConfig.init();
                case GENERAL_SOUND, GENERAL_SOUND_ENTITY, SOUND_PRIORITY -> GeneralSoundsConfig.init();
            }

            SoundConfig.loadConfigs();

            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival] Reloaded config: " + msg.configType + " - " + msg.targetId);
            }

        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Reload error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        MobGoalInjector.refreshAll();

        if (msg.configType == ConfigType.ENTITY_VOICE ||
                msg.configType == ConfigType.GENERAL_SOUND_ENTITY ||
                msg.configType == ConfigType.SOUND_PRIORITY) {
            MobGoalInjector.refreshEntityId(msg.targetId);
        }
    }

    private static void handleEntityVoiceConfig(UpdateConfigPacket msg) {
        EntityVoiceConfig.set(msg.targetId,
                new EntityVoiceConfig.EntityConfig(msg.enabled, msg.value1, msg.value2, msg.value3));
        EntityVoiceConfig.persist();
    }

    private static void handleGeneralSoundConfig(UpdateConfigPacket msg) {
        GeneralSoundsConfig.setSoundEntry(msg.targetId, msg.enabled, msg.value1, msg.value2, msg.boolValue);
        GeneralSoundsConfig.persist();
    }

    private static void handleGeneralSoundEntityConfig(UpdateConfigPacket msg) {
        GeneralSoundsConfig.setMobReaction(msg.targetId, msg.enabled, msg.value1, msg.value2);
        GeneralSoundsConfig.persist();
    }

    private static void handleSoundPriorityConfig(UpdateConfigPacket msg) {
        GeneralSoundsConfig.setSoundEntry(msg.targetId, msg.enabled, msg.value1, msg.value2, msg.boolValue);
        GeneralSoundsConfig.persist();
    }

    private static void handleGlobalConfigChange(UpdateConfigPacket msg) {
        switch (msg.configType) {
            case ENTITY_VOICE -> {
                if (EntityVoiceConfig.ROOT == null)
                    EntityVoiceConfig.ROOT = new EntityVoiceConfig.RootConfig();
                EntityVoiceConfig.ROOT.enabled = msg.enabled;
                EntityVoiceConfig.persist();
            }
            case GENERAL_SOUND -> {
                if (GeneralSoundsConfig.ROOT == null)
                    GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
                GeneralSoundsConfig.ROOT.enabled = msg.enabled;
                GeneralSoundsConfig.persist();
            }
            case SOUND_PRIORITY -> GeneralSoundsConfig.enableAllPrioritySounds(msg.enabled);
        }
    }

    private static void handleRefreshRequest() {
        try {
            GeneralSoundsConfig.init();
            EntityVoiceConfig.init();
            SoundConfig.loadConfigs();
            MobGoalInjector.refreshAll();
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                e.printStackTrace();
            }
        }
    }
}