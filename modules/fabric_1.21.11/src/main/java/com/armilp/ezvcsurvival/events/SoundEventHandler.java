package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.packets.GeneralSoundPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.util.Identifier;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SoundEventHandler {

    private static final Map<Identifier, Long> SOUND_COOLDOWNS = new ConcurrentHashMap<>(128);
    private static final long EXPLOSION_COOLDOWN_MS = 300;
    private static final long DEFAULT_COOLDOWN_MS = 50;
    private static final long PRIORITY_COOLDOWN_MS = 50;

    private static long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL_MS = 10000;
    private static final int MAX_COOLDOWN_ENTRIES = 150;

    public static void onPlaySound(SoundInstance sound) {
        // Guard: need an active connection and player
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null
                || mc.getNetworkHandler().getConnection() == null
                || mc.player == null) {
            return;
        }

        var soundMap = GeneralSoundsConfig.getSounds();
        if (soundMap == null) return;

        Identifier soundLoc = sound.getId();
        String soundId = soundLoc.toString();

        // Only handle sounds registered in our config
        GeneralSoundsConfig.SoundEntry cfg = soundMap.get(soundId);
        if (cfg == null || !cfg.enabled) return;

        long currentTime = System.currentTimeMillis();

        // Apply cooldown
        long cooldown = getCooldownDuration(soundId, cfg.is_priority);
        Long lastTime = SOUND_COOLDOWNS.get(soundLoc);
        if (lastTime != null && (currentTime - lastTime) < cooldown) return;

        SOUND_COOLDOWNS.put(soundLoc, currentTime);

        // Periodic cleanup of stale cooldown entries
        if ((currentTime - lastCleanupTime) > CLEANUP_INTERVAL_MS) {
            cleanupCooldowns(currentTime);
            lastCleanupTime = currentTime;
        }

        EZVCNetwork.ezvcNetworkService.sendToServer(new GeneralSoundPacket(
                sound.getId(),
                sound.getX(),
                sound.getY(),
                sound.getZ(),
                cfg.speed_multiplier,
                cfg.range_multiplier
        ));
    }

    private static long getCooldownDuration(String soundId, boolean isPriority) {
        if (isPriority) return PRIORITY_COOLDOWN_MS;
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
