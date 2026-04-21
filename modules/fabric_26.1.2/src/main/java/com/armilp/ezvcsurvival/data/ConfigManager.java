package com.armilp.ezvcsurvival.data;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.network.UpdateConfigPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public class ConfigManager {

    public static void updateConfig(ServerPlayer player, UpdateConfigPayload payload) {
        // Check player permissions if needed
        if (!player.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)) { // OP level 2
            System.out.println("Player " + player.getName().getString() + " lacks permission to update config");
            return;
        }

        switch (payload.configType()) {
            case ENTITY_VOICE:
                handleEntityConfig(payload);
                break;
            case GENERAL_SOUND:
                handleGeneralSoundConfig(payload);
                break;
            case GENERAL_SOUND_ENTITY:
                handleGeneralSoundEntityConfig(payload);
                break;
        }
    }

    private static void handleEntityConfig(UpdateConfigPayload payload) {
        String entityId = payload.targetId();
        if (entityId.equals("global")) {
            EntityVoiceConfig.ROOT.enabled = payload.enabled();
        } else {
            EntityVoiceConfig.set(entityId, new EntityVoiceConfig.EntityConfig(
                    payload.enabled(),
                    payload.value1(), // speed
                    payload.value2(), // range
                    payload.value3()  // threshold
            ));
        }
        EntityVoiceConfig.persist();
        System.out.println("Updated entity config: " + entityId);
    }

    private static void handleGeneralSoundConfig(UpdateConfigPayload payload) {
        String soundId = payload.targetId();
        if (soundId.equals("global")) {
            GeneralSoundsConfig.ROOT.enabled = payload.enabled();
        } else {
            GeneralSoundsConfig.setSoundEntry(
                    soundId,
                    payload.enabled(),
                    payload.value1(), // speed_multiplier
                    payload.value2(), // range_multiplier
                    payload.boolValue() // is_priority
            );
        }
        GeneralSoundsConfig.persist();
        System.out.println("Updated general sound config: " + soundId);
    }

    private static void handleGeneralSoundEntityConfig(UpdateConfigPayload payload) {
        String entityId = payload.targetId();
        if (!entityId.equals("refresh")) {
            GeneralSoundsConfig.setMobReaction(
                    entityId,
                    payload.enabled(),
                    payload.value1(), // speed
                    payload.value2()  // range
            );
            GeneralSoundsConfig.persist();
            System.out.println("Updated general sound entity config: " + entityId);
        }
    }
}
