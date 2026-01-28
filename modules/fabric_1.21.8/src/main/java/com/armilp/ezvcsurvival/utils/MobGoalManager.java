package com.armilp.ezvcsurvival.utils;

import com.armilp.ezvcsurvival.config.VoiceConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

public class MobGoalManager {

    public static int refreshAllExistingMobs(MinecraftServer server) {
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] Starting mob refresh for all existing mobs...");
        }

        int updatedCount = 0;

        try {
            // Actually update the mobs using MobGoalUpdater
            updatedCount = MobGoalUpdater.refreshAllExistingMobs(server);

            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival] Refresh completed: " + updatedCount + " mobs updated");
            }

        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error during mob refresh: " + e.getMessage());
                e.printStackTrace();
            }
            throw e;
        }

        return updatedCount;
    }

    public static int refreshMobsByEntityId(MinecraftServer server, String entityId) {
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] Starting mob refresh for entity: " + entityId);
        }

        int updatedCount = 0;

        try {
            // Actually update the specific mobs
            updatedCount = MobGoalUpdater.refreshMobsByEntityId(server, entityId);

        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error refreshing " + entityId + ": " + e.getMessage());
                e.printStackTrace();
            }
            throw e;
        }

        return updatedCount;
    }

    public static void notifyConfigReloaded(MinecraftServer server) {
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] Configurations reloaded");
        }

        // Send notification to ops
        server.getPlayerManager().getPlayerList().forEach(player -> {
            if (server.getPlayerManager().isOperator(player.getGameProfile())) {
                player.sendMessage(
                        Text.literal("[EZVCSurvival] §a✓ Configurations reloaded successfully"),
                        false
                );
            }
        });
    }

    public static void notifyEntityConfigChanged(MinecraftServer server, String entityId) {
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] Entity config updated: " + entityId);
        }

        // Send notification to ops
        server.getPlayerManager().getPlayerList().forEach(player -> {
            if (server.getPlayerManager().isOperator(player.getGameProfile())) {
                player.sendMessage(
                        Text.literal("[EZVCSurvival] §a✓ Updated configuration for: " + entityId),
                        false
                );
            }
        });
    }

    public static void refreshAndNotify(MinecraftServer server) {
        // Complete refresh: update configs AND existing mobs
        notifyConfigReloaded(server);

        int updatedCount = refreshAllExistingMobs(server);

        // Send final notification
        server.getPlayerManager().getPlayerList().forEach(player -> {
            if (server.getPlayerManager().isOperator(player.getGameProfile())) {
                player.sendMessage(
                        Text.literal("[EZVCSurvival] §a✓ Updated " + updatedCount + " existing mobs"),
                        false
                );
                player.sendMessage(
                        Text.literal("[EZVCSurvival] §7New mobs will automatically have updated AI"),
                        false
                );
            }
        });
    }
}