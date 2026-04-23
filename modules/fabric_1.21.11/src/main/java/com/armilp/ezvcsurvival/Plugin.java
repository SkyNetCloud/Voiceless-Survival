package com.armilp.ezvcsurvival;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.data.SoundData;
import com.armilp.ezvcsurvival.events.ArmorEventHandler;
import com.armilp.ezvcsurvival.sculk.SculkVibrationHelper;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ForgeVoicechatPlugin
public class Plugin implements VoicechatPlugin {

    private boolean DEBUG;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Map<UUID, SoundData> playerSoundLocations = new ConcurrentHashMap<>();
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
        if (DEBUG) {
            System.out.println("[DEBUG] VoiceChat Plugin initialized");
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        if (DEBUG) {
            System.out.println("[DEBUG] Registered MicrophonePacketEvent");
        }
    }

    public static double getMaxAudioLevel(short[] samples) {
        double rms = 0D;

        for (short value : samples) {
            double sample = (double) value / (double) Short.MAX_VALUE;
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
        return playerSoundLocations.values().stream()
                .filter(data -> data.audioLevelDb() >= minDb)
                .filter(data -> mobPosition.getSquaredDistance(data.position()) <= range * range)
                .min(Comparator.comparingDouble(data -> mobPosition.getSquaredDistance(data.position())))
                .map(SoundData::position)
                .orElse(null);
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
            return;
        }

        double audioLevel = getMaxAudioLevel(decoded);

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

        long currentTime = System.currentTimeMillis();

        for (String id : allIds) {
            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.get(id);
            if (cfg == null) cfg = EntityVoiceConfig.get(id);
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
                if (p.getEntityWorld().isRaining() || p.getEntityWorld().isThundering())
                    detectionRange *= thunderRangeMultiplier;
                double[] armorMult = ArmorEventHandler.getArmorMultipliers(p);
                detectionRange *= armorMult[1];
                speed *= armorMult[0];
            }

            double modifiedRange = detectionRange;

            double distance = senderVec.distanceTo(new Vec3d(playerPosition.getX(), playerPosition.getY(), playerPosition.getZ()));
            double distanceVolume = 1.0 - Math.min(distance, modifiedRange) / modifiedRange;

            if (audioLevel >= threshold && distanceVolume > 0.0) {
                BlockPos precisePos = new BlockPos(
                        (int) Math.floor(senderVec.x),
                        (int) Math.floor(senderVec.y),
                        (int) Math.floor(senderVec.z)
                );
                playerSoundLocations.put(
                        playerUUID,
                        new SoundData(precisePos, audioLevel)
                );
                if (DEBUG) {
                    System.out.println("[DEBUG] " + id + " detects sound! " +
                            "Threshold: " + threshold + " dB | " +
                            "AudioLevel: " + audioLevel + " dB | " +
                            "Range: " + detectionRange + " | " +
                            "Speed: " + speed + " | " +
                            "Position: " + precisePos);
                }
                if (DEBUG) {
                    System.out.println("[DEBUG] Intensity/range too low for " + id + ": "
                            + audioLevel + " dB | " + distanceVolume);
                }
            }
        }

        // Sculk Sensor Voice Detection
        if (VoiceConfig.SCULK_SENSOR_ENABLED.get() && sender.getPlayer().getPlayer() instanceof ServerPlayerEntity serverPlayer) {
            if (!lastSculkVibrationTime.containsKey(playerUUID)
                    || currentTime - lastSculkVibrationTime.get(playerUUID) > SCULK_VIBRATION_COOLDOWN_MS) {
                if (audioLevel >= VoiceConfig.SCULK_SENSOR_THRESHOLD.get()) {
                    SculkVibrationHelper.generateVibration(serverPlayer, VoiceConfig.SCULK_SENSOR_RANGE.get(), audioLevel);
                    lastSculkVibrationTime.put(playerUUID, currentTime);
                    if (DEBUG) {
                        System.out.println("[DEBUG] Sculk vibration generated for player " + playerUUID +
                                " | AudioLevel: " + audioLevel + " dB");
                    }
                }
            }
        }

        //scheduler.schedule(() -> playerSoundLocations.remove(playerUUID), 5, TimeUnit.SECONDS);
    }
}