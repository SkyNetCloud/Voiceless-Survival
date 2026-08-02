package com.armilp.ezvcsurvival;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.data.SoundData;
import com.armilp.ezvcsurvival.events.ArmorEventHandler;
import com.armilp.ezvcsurvival.sculk.SculkVibrationHelper;

import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.audio.codec.AudioDecoder;
import su.plo.voice.api.encryption.Encryption;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.capture.ServerActivation;
import su.plo.voice.api.server.player.VoiceServerPlayer;
import su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Addon(
        id = "pv-addon-ezvcsurvival",
        name = "Voiceless Survival",
        version = "1.0.0",
        authors = {"armilp", "skynetcloud"}
)
public final class PlasmoAddon implements AddonInitializer {

    @InjectPlasmoVoice
    private PlasmoVoiceServer voiceServer;

    private boolean DEBUG;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Map<UUID, SoundData> playerSoundLocations = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastSculkVibrationTime = new ConcurrentHashMap<>();
    private static final long SCULK_VIBRATION_COOLDOWN_MS = 500;
    private static final Map<UUID, Long> lastSoundTime = new ConcurrentHashMap<>();

    private final Map<UUID, AudioDecoder> decoders = new ConcurrentHashMap<>();

    @Override
    public void onAddonInitialize() {
        this.DEBUG = VoiceConfig.DEBUG.get();
        if (DEBUG) System.out.println("[DEBUG] Plasmo Voice addon initialized");

        ServerActivation proximity = voiceServer.getActivationManager()
                .getActivationByName("proximity")
                .orElseThrow(() -> new IllegalStateException("Proximity activation not found"));

        proximity.onPlayerActivation((player, packet) -> {
            onVoicePacket((VoiceServerPlayer) player, packet);
            return ServerActivation.Result.HANDLED;
        });

        // clear stale sound-location entries periodically, same as before
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            playerSoundLocations.entrySet().removeIf(entry -> {
                UUID uuid = entry.getKey();
                Long last = lastSoundTime.get(uuid);
                return last == null || now - last > 5000;
            });
        }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void onAddonShutdown() {
        decoders.values().forEach(AudioDecoder::close);
        decoders.clear();
        scheduler.shutdownNow();
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

    public static BlockPos getLastSoundLocation(BlockPos mobPosition, double range, double minDb) {
        return playerSoundLocations.values().stream()
                .filter(data -> data.getAudioLevelDb() >= minDb)
                .filter(data -> mobPosition.getSquaredDistance(data.getPosition()) <= range * range)
                .min(Comparator.comparingDouble(data -> mobPosition.getSquaredDistance(data.getPosition())))
                .map(SoundData::getPosition)
                .orElse(null);
    }

    private void onVoicePacket(VoiceServerPlayer voicePlayer, PlayerAudioPacket packet) {

        Object nativeInstance = voicePlayer.getInstance();
        if (!(nativeInstance instanceof ServerPlayerEntity player)) return;

        if (player.isCreative() || player.isSpectator()) return;

        AudioDecoder decoder = decoders.computeIfAbsent(
                voicePlayer.getInstance().hashCode() >= 0 ? player.getUuid() : player.getUuid(),
                id -> voiceServer.createOpusDecoder(false)
        );

        short[] decoded;
        try {
            Encryption encryption = voiceServer.getDefaultEncryption();
            byte[] decrypted = encryption.decrypt(packet.getData());
            decoded = decoder.decode(decrypted);
        } catch (Exception e) {
            return;
        }

        double audioLevel = getMaxAudioLevel(decoded);

        UUID playerUUID = player.getUuid();

        Vec3d senderVec = player.getEntityPos();
        BlockPos playerPosition = new BlockPos(
                (int) Math.floor(senderVec.x),
                (int) Math.floor(senderVec.y),
                (int) Math.floor(senderVec.z)
        );

        boolean isWhispering = false;

        double whisperRangeMultiplier = VoiceConfig.WHISPER_RANGE_MULTIPLIER.get();
        double whisperSpeedMultiplier = VoiceConfig.WHISPER_SPEED_MULTIPLIER.get();
        double thunderRangeMultiplier = VoiceConfig.THUNDER_RANGE_MULTIPLIER.get();
        double sneakingRangeMultiplier = VoiceConfig.SNEAKING_RANGE_MULTIPLIER.get();

        List<String> allIds = new ArrayList<>(EntityVoiceConfig.getAllEntityIds());
        long currentTime = System.currentTimeMillis();

        for (String id : allIds) {
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

            double modifiedRange = detectionRange;
            double distance = senderVec.distanceTo(new Vec3d(playerPosition.getX(), playerPosition.getY(), playerPosition.getZ()));
            double distanceVolume = 1.0 - Math.min(distance, modifiedRange) / modifiedRange;

            if (audioLevel >= threshold && distanceVolume > 0.0) {
                BlockPos precisePos = new BlockPos(
                        (int) Math.floor(senderVec.x),
                        (int) Math.floor(senderVec.y),
                        (int) Math.floor(senderVec.z)
                );
                playerSoundLocations.put(playerUUID, new SoundData(precisePos, audioLevel));
                lastSoundTime.put(playerUUID, currentTime);

                if (DEBUG) {
                    System.out.println("[DEBUG] " + id + " detects sound! " +
                            "Threshold: " + threshold + " dB | " +
                            "AudioLevel: " + audioLevel + " dB | " +
                            "Range: " + detectionRange + " | " +
                            "Speed: " + speed + " | " +
                            "Position: " + precisePos);
                }
            }
        }

        // Sculk Sensor Voice Detection
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
    public static void register() {
        PlasmoVoiceServer.getAddonsLoader().load(new PlasmoAddon());
    }
}