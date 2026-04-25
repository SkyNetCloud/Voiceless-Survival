package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.packets.GeneralSoundPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SoundEventHandler {

    private static final Map<Identifier, Long> SOUND_COOLDOWNS = new ConcurrentHashMap<>(128);
    private static final long EXPLOSION_COOLDOWN_MS = 300;
    private static final long DEFAULT_COOLDOWN_MS = 50;

    private static long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL_MS = 10000;
    private static final int MAX_COOLDOWN_ENTRIES = 150;

    public static void onPlaySound(SoundEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
        if (networkHandler == null || mc.player == null) {
            return;
        }

        Identifier soundLoc = event.id();

        var soundMap = GeneralSoundsConfig.getSounds();
        if (soundMap == null) {
            return;
        }

        String soundId = soundLoc.toString();
        GeneralSoundsConfig.SoundEntry cfg = soundMap.get(soundId);
        if (cfg == null || !cfg.enabled) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (cfg.is_priority) {
            Long lastTime = SOUND_COOLDOWNS.get(soundLoc);
            if (lastTime != null && (currentTime - lastTime) < 50) {
                return;
            }
            SOUND_COOLDOWNS.put(soundLoc, currentTime);
        } else if (shouldApplyCooldown(soundId, cfg.is_priority)) {
            Long lastTime = SOUND_COOLDOWNS.get(soundLoc);
            long cooldownDuration = getCooldownDuration(soundId, cfg.is_priority);

            if (lastTime != null && (currentTime - lastTime) < cooldownDuration) {
                return;
            }

            SOUND_COOLDOWNS.put(soundLoc, currentTime);
        }

        if ((currentTime - lastCleanupTime) > CLEANUP_INTERVAL_MS) {
            cleanupCooldowns(currentTime);
            lastCleanupTime = currentTime;
        }

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        // Use the EZVCNetwork method to send sound to server
        EZVCNetwork.ezvcNetworkService.sendToServer(
                new GeneralSoundPacket(soundLoc, x, y, z, cfg.speed_multiplier, cfg.range_multiplier, null)
        );
    }

    private static boolean shouldApplyCooldown(String soundId, boolean isPriority) {
        if (isPriority) {
            return true;
        }

        if (soundId.contains("explode") || soundId.contains("explosion")) {
            return true;
        }

        return soundId.contains("tnt");
    }

    private static long getCooldownDuration(String soundId, boolean isPriority) {
        if (isPriority) {
            return 50;
        }

        if (soundId.contains("explode") || soundId.contains("explosion") || soundId.contains("tnt")) {
            return EXPLOSION_COOLDOWN_MS;
        }

        return DEFAULT_COOLDOWN_MS;
    }

    private static void cleanupCooldowns(long currentTime) {
        if (SOUND_COOLDOWNS.size() > MAX_COOLDOWN_ENTRIES) {
            SOUND_COOLDOWNS.clear();
            return;
        }

        Iterator<Map.Entry<Identifier, Long>> iterator = SOUND_COOLDOWNS.entrySet().iterator();
        long threshold = EXPLOSION_COOLDOWN_MS * 4;

        while (iterator.hasNext()) {
            Map.Entry<Identifier, Long> entry = iterator.next();
            if ((currentTime - entry.getValue()) > threshold) {
                iterator.remove();
            }
        }
    }
}