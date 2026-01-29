package com.armilp.ezvcsurvival;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.data.SoundData;
import com.armilp.ezvcsurvival.events.ArmorEventHandler;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ForgeVoicechatPlugin
public class Plugin implements VoicechatPlugin {

    public static final Logger LOGGER = LoggerFactory.getLogger("EZVCSurvival");
    private static boolean DEBUG;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Store sound data with timestamps
    private static class TimedSoundData {
        public final SoundData data;
        public final long timestamp;

        public TimedSoundData(SoundData data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
    }

    private static final Map<UUID, TimedSoundData> playerSoundLocations = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastSculkVibrationTime = new ConcurrentHashMap<>();
    private static final long SCULK_VIBRATION_COOLDOWN_MS = 500;
    private static VoicechatApi voicechatApi;

    @Override
    public String getPluginId() {
        return "ezvcsurvival";
    }

    @Nullable
    private OpusDecoder decoder;

    @Override
    public void initialize(VoicechatApi api) {
        voicechatApi = api;
        this.DEBUG = VoiceConfig.DEBUG.get();

        // Start cleanup task for old sounds
        startCleanupTask();

        if (DEBUG) {
            LOGGER.debug("[EZVCSurvival] VoiceChat Plugin initialized");
            LOGGER.debug("[EZVCSurvival] Debug mode enabled");
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        if (DEBUG) {
            LOGGER.debug("[EZVCSurvival] Registered MicrophonePacketEvent");
        }
    }

    public static double getMaxAudioLevel(short[] samples) {
        double rms = 0D;

        for (int i = 0; i < samples.length; i++) {
            double sample = (double) samples[i] / (double) Short.MAX_VALUE;
            rms += sample * sample;
        }

        int sampleCount = samples.length / 2;
        rms = (sampleCount == 0) ? 0 : Math.sqrt(rms / sampleCount);

        double db;
        if (rms > 0D) {
            db = Math.min(Math.max(20D * Math.log10(rms), -127D), 0D);
        } else {
            db = -127D;
        }

        return db;
    }

    @Nullable
    public static BlockPos getLastSoundLocation(BlockPos mobPosition, double range, double minDb) {
        long now = System.currentTimeMillis();

        // Get timeout from config or use default (15 seconds)
        long maxAge;
        try {
            maxAge = VoiceConfig.SOUND_TIMEOUT_SECONDS.get() * 1000L;
        } catch (Exception e) {
            maxAge = 15000L; // Default 15 seconds if config not loaded
        }

        if (DEBUG) {
            LOGGER.debug("[EZVCSurvival] getLastSoundLocation called - Mob at: {}, Range: {}, Min dB: {}",
                    mobPosition, range, minDb);
            LOGGER.debug("[EZVCSurvival] Tracking {} sound locations", playerSoundLocations.size());
        }

        long finalMaxAge = maxAge;
        return playerSoundLocations.values().stream()
                .filter(timedData -> {
                    long age = now - timedData.timestamp;
                    boolean isValid = age <= finalMaxAge;
                    if (DEBUG && !isValid) {
                        LOGGER.debug("[EZVCSurvival] Filtered out sound - too old: {}ms > {}ms", age, finalMaxAge);
                    }
                    return isValid;
                })
                .filter(timedData -> {
                    boolean meetsThreshold = timedData.data.audioLevelDb() >= minDb;
                    if (DEBUG && !meetsThreshold) {
                        LOGGER.debug("[EZVCSurvival] Filtered out sound - dB too low: {} < {}",
                                timedData.data.audioLevelDb(), minDb);
                    }
                    return meetsThreshold;
                })
                .filter(timedData -> {
                    double distanceSq = mobPosition.getSquaredDistance(timedData.data.position());
                    boolean inRange = distanceSq <= range * range;
                    if (DEBUG && !inRange) {
                        LOGGER.debug("[EZVCSurvival] Filtered out sound - out of range: {} > {} blocks",
                                Math.sqrt(distanceSq), range);
                    }
                    return inRange;
                })
                .min(Comparator.comparingDouble(timedData ->
                        mobPosition.getSquaredDistance(timedData.data.position())))
                .map(timedData -> {
                    if (DEBUG) {
                        LOGGER.debug("[EZVCSurvival] Found valid sound at: {} ({}dB, {}ms old)",
                                timedData.data.position(),
                                timedData.data.audioLevelDb(),
                                now - timedData.timestamp);
                    }
                    return timedData.data.position();
                })
                .orElseGet(() -> {
                    if (DEBUG) {
                        LOGGER.debug("[EZVCSurvival] No valid sound found for mob at {}", mobPosition);
                    }
                    return null;
                });
    }

    public void onMicrophonePacket(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null || sender.getPlayer() == null) return;

        if (sender.getPlayer().getPlayer() instanceof ServerPlayerEntity player
                && (player.isCreative() || player.isSpectator())) return;

        OpusDecoder localDecoder = decoder;
        if (localDecoder == null || localDecoder.isClosed()) {
            localDecoder = voicechatApi.createDecoder();
            decoder = localDecoder;
        }
        if (localDecoder == null) {
            return;
        }
        localDecoder.resetState();

        byte[] opusEncodedData = event.getPacket().getOpusEncodedData();
        short[] decoded;
        try {
            decoded = localDecoder.decode(opusEncodedData);
        } catch (Exception e) {
            if (DEBUG) {
                LOGGER.error("[EZVCSurvival] Failed to decode audio packet: {}", e.getMessage());
            }
            return;
        }

        double audioLevel = getMaxAudioLevel(decoded);

        // Debug: Log all sounds above a certain threshold
        if (DEBUG && audioLevel > -40.0) {
            LOGGER.debug("[EZVCSurvival] Raw sound detected: {}dB", audioLevel);
        }

        UUID playerUUID = sender.getPlayer().getUuid();
        Position voicechatPosition = sender.getPlayer().getPosition();

        Vec3d senderVec = new Vec3d(
                voicechatPosition.getX(),
                voicechatPosition.getY(),
                voicechatPosition.getZ()
        );
        BlockPos playerPosition = new BlockPos(
                (int) Math.floor(senderVec.x),
                (int) Math.floor(senderVec.y),
                (int) Math.floor(senderVec.z)
        );

        boolean isWhispering = event.getPacket().isWhispering();
        double whisperRangeMultiplier = VoiceConfig.WHISPER_RANGE_MULTIPLIER.get();
        double whisperSpeedMultiplier = VoiceConfig.WHISPER_SPEED_MULTIPLIER.get();
        double thunderRangeMultiplier = VoiceConfig.THUNDER_RANGE_MULTIPLIER.get();
        double sneakingRangeMultiplier = VoiceConfig.SNEAKING_RANGE_MULTIPLIER.get();

        List<String> allIds = new ArrayList<>(EntityVoiceConfig.getAllEntityIds());

        if (DEBUG) {
            LOGGER.debug("[EZVCSurvival] Processing sound for {} entities", allIds.size());
        }

        long currentTime = System.currentTimeMillis();
        boolean anyEntityDetectedSound = false;

        for (String id : allIds) {
            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getMonster(id);
            if (cfg == null) cfg = EntityVoiceConfig.getAnimal(id);
            if (cfg == null || !cfg.enabled) continue;

            double threshold = cfg.threshold;
            double detectionRange = cfg.range;
            double speed = cfg.speed;

            if (isWhispering) {
                detectionRange *= whisperRangeMultiplier;
                speed *= whisperSpeedMultiplier;
            }

            if (sender.getPlayer().getPlayer() instanceof ServerPlayerEntity p) {
                if (p.isSneaking()) detectionRange *= sneakingRangeMultiplier;
                if (p.getWorld().isRaining() || p.getWorld().isThundering())
                    detectionRange *= thunderRangeMultiplier;
                double[] armorMult = ArmorEventHandler.getArmorMultipliers(p);
                detectionRange *= armorMult[1];
                speed *= armorMult[0];
            }

            double modifiedRange = detectionRange;

            // Calculate actual distance from sound source to mobs (this would need mob positions)
            // For now, we'll just check if the sound is loud enough
            double distanceVolume = 1.0; // Simplified for now

            // Check if sound meets threshold and range requirements
            boolean meetsThreshold = audioLevel >= threshold;
            boolean meetsRange = true; // We'll check range when mobs query for sounds

            if (meetsThreshold && meetsRange) {
                BlockPos precisePos = new BlockPos(
                        (int) Math.floor(senderVec.x),
                        (int) Math.floor(senderVec.y),
                        (int) Math.floor(senderVec.z)
                );

                // Store the sound with timestamp
                playerSoundLocations.put(
                        playerUUID,
                        new TimedSoundData(new SoundData(precisePos, audioLevel), currentTime)
                );

                anyEntityDetectedSound = true;

                if (DEBUG) {
                    LOGGER.debug("[EZVCSurvival] {} detected sound! Threshold: {}dB | Audio: {}dB | Range: {} | Position: {}",
                            id, threshold, audioLevel, modifiedRange, precisePos);
                }
            } else if (DEBUG && audioLevel > -60.0) {
                LOGGER.debug("[EZVCSurvival] {} did NOT detect sound - Threshold: {}dB | Audio: {}dB | Range: {}",
                        id, threshold, audioLevel, modifiedRange);
            }
        }

        // Sculk Sensor Voice Detection (commented out for now)
        // if (VoiceConfig.SCULK_SENSOR_ENABLED.get() && sender.getPlayer().getPlayer() instanceof ServerPlayerEntity serverPlayer) {
        //     if (!lastSculkVibrationTime.containsKey(playerUUID)
        //             || currentTime - lastSculkVibrationTime.get(playerUUID) > SCULK_VIBRATION_COOLDOWN_MS) {
        //         if (audioLevel >= VoiceConfig.SCULK_SENSOR_THRESHOLD.get()) {
        //             SculkVibrationHelper.generateVibration(serverPlayer, VoiceConfig.SCULK_SENSOR_RANGE.get(), audioLevel);
        //             lastSculkVibrationTime.put(playerUUID, currentTime);
        //             if (DEBUG) {
        //                 LOGGER.debug("[EZVCSurvival] Sculk vibration generated for player {} | AudioLevel: {} dB",
        //                         playerUUID, audioLevel);
        //             }
        //         }
        //     }
        // }

        // Don't schedule removal - let cleanup task handle it
        // This way sounds persist for the full timeout period
    }

    private void startCleanupTask() {
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            long maxAge = VoiceConfig.SOUND_TIMEOUT_SECONDS.get() * 1000L;

            int initialSize = playerSoundLocations.size();
            playerSoundLocations.entrySet().removeIf(entry ->
                    (now - entry.getValue().timestamp) > maxAge);

            int removed = initialSize - playerSoundLocations.size();
            if (DEBUG && removed > 0) {
                LOGGER.debug("[EZVCSurvival] Cleanup task removed {} old sounds", removed);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public static void debugSoundLocations() {
        if (LOGGER.isDebugEnabled()) {
            long now = System.currentTimeMillis();
            LOGGER.debug("[EZVCSurvival] === SOUND LOCATION DEBUG ===");
            LOGGER.debug("[EZVCSurvival] Currently tracking {} sound locations:", playerSoundLocations.size());

            if (playerSoundLocations.isEmpty()) {
                LOGGER.debug("[EZVCSurvival] No sounds being tracked");
            } else {
                playerSoundLocations.forEach((uuid, timedData) -> {
                    long age = now - timedData.timestamp;
                    LOGGER.debug("[EZVCSurvival]   Player: {}, Position: {}, Age: {}ms, Volume: {}dB",
                            uuid.toString().substring(0, 8) + "...",
                            timedData.data.position(),
                            age,
                            timedData.data.audioLevelDb());
                });
            }
            LOGGER.debug("[EZVCSurvival] === END DEBUG ===");
        }
    }

    public static int getTrackedSoundCount() {
        return playerSoundLocations.size();
    }

    public static void clearAllSounds() {
        playerSoundLocations.clear();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[EZVCSurvival] Cleared all tracked sounds");
        }
    }
}