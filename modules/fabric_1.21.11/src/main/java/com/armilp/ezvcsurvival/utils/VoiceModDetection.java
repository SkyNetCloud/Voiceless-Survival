package com.armilp.ezvcsurvival.utils;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.data.SoundData;
import com.armilp.ezvcsurvival.events.ArmorEventHandler;
import com.armilp.ezvcsurvival.sculk.SculkVibrationHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VoiceModDetection {

    private VoiceModDetection() {}

    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static boolean DEBUG;

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Map<UUID, SoundData> playerSoundLocations = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastSculkVibrationTime = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastSoundTime = new ConcurrentHashMap<>();
    private static final long SCULK_VIBRATION_COOLDOWN_MS = 500;

    /**
     * Call once, from EZVCSurvival.onInitialize(). Idempotent -- safe if
     * called more than once (e.g. also called from Plugin.initialize()).
     */
    public static void init() {
        if (!initialized.compareAndSet(false, true)) return;

        DEBUG = VoiceConfig.DEBUG.get();
        if (DEBUG) System.out.println("[DEBUG] VoiceDetectionCore initialized");

        // NOTE: this cleanup task is now scheduled exactly once, ever.
        // The old Plugin.java scheduled a brand new recurring task on
        // EVERY microphone packet -- that was leaking scheduled tasks
        // continuously and is fixed by centralizing this here.
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            playerSoundLocations.entrySet().removeIf(entry -> {
                Long last = lastSoundTime.get(entry.getKey());
                return last == null || now - last > 5000;
            });
        }, 1, 1, TimeUnit.SECONDS);
    }

    public static double getMaxAudioLevel(short[] samples) {
        double rms = 0D;

        for (short value : samples) {
            double sample = (double) value / (double) Short.MAX_VALUE;
            rms += sample * sample;
        }

        int sampleCount = samples.length / 2;
        rms = (sampleCount == 0) ? 0 : Math.sqrt(rms / sampleCount);

        if (rms > 0D) {
            return Math.min(Math.max(20D * Math.log10(rms), -127D), 0D);
        }
        return -127D;
    }

    @Nullable
    public static BlockPos getLastSoundLocation(BlockPos mobPosition, double range, double minDb) {
        return playerSoundLocations.values().stream()
                .filter(data -> data.getAudioLevelDb() >= minDb)
                .filter(data -> mobPosition.getSquaredDistance(data.getPosition()) <= range * range)
                .min(Comparator.comparingDouble(data -> mobPosition.getSquaredDistance(data.getPosition())))
                .map(SoundData::getPosition)
                .orElse(null);
    }

    /**
     * Called by whichever voice-chat integration is active (Plugin or
     * PlasmoAddon), once per decoded audio frame.
     */
    public static void onAudioLevel(ServerPlayerEntity player, double audioLevel, boolean isWhispering) {
        if (player.isCreative() || player.isSpectator()) return;

        UUID playerUUID = player.getUuid();
        Vec3d senderVec = player.getEntityPos();
        BlockPos playerPosition = new BlockPos(
                (int) Math.floor(senderVec.x),
                (int) Math.floor(senderVec.y),
                (int) Math.floor(senderVec.z)
        );

        double whisperRangeMultiplier = VoiceConfig.WHISPER_RANGE_MULTIPLIER.get();
        double whisperSpeedMultiplier = VoiceConfig.WHISPER_SPEED_MULTIPLIER.get();
        double thunderRangeMultiplier = VoiceConfig.THUNDER_RANGE_MULTIPLIER.get();
        double sneakingRangeMultiplier = VoiceConfig.SNEAKING_RANGE_MULTIPLIER.get();

        long currentTime = System.currentTimeMillis();

        for (String id : new ArrayList<>(EntityVoiceConfig.getAllEntityIds())) {
            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.get(id);
            if (cfg == null || !cfg.enabled) continue;

            double threshold = cfg.threshold;
            double detectionRange = cfg.range;
            double speed = cfg.speed;

            if (isWhispering) {
                detectionRange *= whisperRangeMultiplier;
                speed *= whisperSpeedMultiplier;
            }

            if (player.isSneaking()) detectionRange *= sneakingRangeMultiplier;
            if (player.getEntityWorld().isRaining() || player.getEntityWorld().isThundering())
                detectionRange *= thunderRangeMultiplier;

            double[] armorMult = ArmorEventHandler.getArmorMultipliers(player);
            detectionRange *= armorMult[1];
            speed *= armorMult[0];

            double distance = senderVec.distanceTo(new Vec3d(playerPosition.getX(), playerPosition.getY(), playerPosition.getZ()));
            double distanceVolume = 1.0 - Math.min(distance, detectionRange) / detectionRange;

            if (audioLevel >= threshold && distanceVolume > 0.0) {
                playerSoundLocations.put(playerUUID, new SoundData(playerPosition, audioLevel));
                lastSoundTime.put(playerUUID, currentTime);

                if (DEBUG) {
                    System.out.println("[DEBUG] " + id + " detects sound! " +
                            "Threshold: " + threshold + " dB | " +
                            "AudioLevel: " + audioLevel + " dB | " +
                            "Range: " + detectionRange + " | " +
                            "Speed: " + speed + " | " +
                            "Position: " + playerPosition);
                }
            }
        }

        if (VoiceConfig.SCULK_SENSOR_ENABLED.get()) {
            Long last = lastSculkVibrationTime.get(playerUUID);
            if (last == null || currentTime - last > SCULK_VIBRATION_COOLDOWN_MS) {
                if (audioLevel >= VoiceConfig.SCULK_SENSOR_THRESHOLD.get()) {
                    SculkVibrationHelper.generateVibration(player, VoiceConfig.SCULK_SENSOR_RANGE.get(), audioLevel);
                    lastSculkVibrationTime.put(playerUUID, currentTime);
                    if (DEBUG) {
                        System.out.println("[DEBUG] Sculk vibration generated for player " + playerUUID +
                                " | AudioLevel: " + audioLevel + " dB");
                    }
                }
            }
        }
    }
}

