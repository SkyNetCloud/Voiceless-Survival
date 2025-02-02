package com.armilp.ezvcsurvival;

import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.data.SoundData;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Plugin implements VoicechatPlugin {

    private static final boolean DEBUG = false;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Map<UUID, SoundData> playerSoundLocations = new ConcurrentHashMap<>();

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
        if (DEBUG) {
            System.out.println("[DEBUG] VoiceChat Plugin initialized");
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        if (DEBUG) {
            System.out.println("[DEBUG] Registro del evento MicrophonePacketEvent");
        }
    }

    public static double calculateAudioLevel(short[] samples) {
        double rms = 0D;

        for (short sample : samples) {
            double normalizedSample = (double) sample / (double) Short.MAX_VALUE;
            rms += normalizedSample * normalizedSample;
        }

        int sampleCount = samples.length;
        rms = (sampleCount == 0) ? 0 : Math.sqrt(rms / sampleCount);

        if (rms > 0D) {
            return Math.min(Math.max(20D * Math.log10(rms), -127D), 0D);
        } else {
            return -127D;
        }
    }

    public static BlockPos getLastSoundLocation(BlockPos zombiePosition, double range) {
        return playerSoundLocations.values().stream()
                .filter(data -> zombiePosition.getSquaredDistance(data.position()) <= data.range() * data.range())
                .min(Comparator.comparingDouble(data -> zombiePosition.getSquaredDistance(data.position())))
                .map(SoundData::position)
                .orElse(null);
    }

    public static double getLastSoundSpeed(BlockPos zombiePosition, double range) {
        return playerSoundLocations.values().stream()
                .filter(data -> zombiePosition.getSquaredDistance(data.position()) <= data.range() * data.range())
                .min(Comparator.comparingDouble(data -> zombiePosition.getSquaredDistance(data.position())))
                .map(SoundData::speed)
                .orElse(1.0);
    }

    public void onMicrophonePacket(MicrophonePacketEvent event) {
        // Get the player who sent the packet
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null || sender.getPlayer() == null) {
            return; // Ensure the player exists
        }

        // Check if the player is in creative mode and exit early
        if (sender.getPlayer().getPlayer() instanceof ServerPlayerEntity player) {
            if (player.isCreative()) {
                return; // Skip all processing for creative players
            }
        }

        if (decoder == null || decoder.isClosed()) {
            decoder = voicechatApi.createDecoder();
        }

        decoder.resetState();
        byte[] opusEncodedData = event.getPacket().getOpusEncodedData();
        short[] decoded;

        try {
            decoded = decoder.decode(opusEncodedData);
        } catch (Exception e) {
            return;
        }

        double audioLevel = calculateAudioLevel(decoded);

        UUID playerUUID = sender.getPlayer().getUuid();
        Position voicechatPosition = sender.getPlayer().getPosition();
        BlockPos playerPosition = new BlockPos(
                (int) Math.floor(voicechatPosition.getX()),
                (int) Math.floor(voicechatPosition.getY()),
                (int) Math.floor(voicechatPosition.getZ())
        );

        boolean isWhispering = event.getPacket().isWhispering();

        double whisperRangeMultiplier = VoiceConfig.WHISPER_RANGE_MULTIPLIER.get();
        double whisperSpeedMultiplier = VoiceConfig.WHISPER_SPEED_MULTIPLIER.get();
        double thunderRangeMultiplier = VoiceConfig.THUNDER_RANGE_MULTIPLIER.get();
        double sneakingRangeMultiplier = VoiceConfig.SNEAKING_RANGE_MULTIPLIER.get();

        List<String> mobIds = getConfiguredMobIds();
        List<String> animalIds = getConfiguredAnimalIds();

        for (String animalId : animalIds) {
            double threshold = getActivationThreshold(animalId, true);
            double detectionRange = getDetectionRange(animalId, true);
            double speed = getSpeed(animalId, true);

            if (isWhispering) {
                detectionRange *= whisperRangeMultiplier;
                speed *= whisperSpeedMultiplier;

                if (sender.getPlayer().getPlayer() instanceof ServerPlayerEntity player) {
                    if (player.isSneaking()) {
                        detectionRange *= sneakingRangeMultiplier;
                    }

                    if (player.getWorld().isRaining() || player.getWorld().isThundering()) {
                        detectionRange *= thunderRangeMultiplier;
                    }
                }
            } else {
                if (sender.getPlayer().getPlayer() instanceof ServerPlayerEntity player) {
                    if (player.isSneaking()) {
                        detectionRange *= sneakingRangeMultiplier;
                    }
                    if (player.getWorld().isRaining() || player.getWorld().isThundering()) {
                        detectionRange *= thunderRangeMultiplier;
                    }
                }
            }

            BlockPos senderPosition = new BlockPos(
                    (int) Math.floor(sender.getPlayer().getPosition().getX()),
                    (int) Math.floor(sender.getPlayer().getPosition().getY()),
                    (int) Math.floor(sender.getPlayer().getPosition().getZ())
            );

            double distance = playerPosition.getSquaredDistance(senderPosition);
            double perceivedIntensity = audioLevel - 20 * Math.log10(distance + 1);

            if (DEBUG) {
                System.out.println("[DEBUG] Perceived Intensity for " + animalId + ": " + perceivedIntensity + " dB at distance " + distance);
            }

            if (perceivedIntensity < threshold) {
                if (DEBUG) {
                    System.out.println("[DEBUG] Intensity too low for " + animalId + ": " + perceivedIntensity + " dB");
                }
                continue;
            }

            if (distance <= detectionRange * detectionRange) {
                playerSoundLocations.put(playerUUID, new SoundData(playerPosition, detectionRange, speed));

                if (DEBUG) {
                    System.out.println("[DEBUG] " + animalId + " detects sound at range " + detectionRange + " with speed " + speed + " from position " + playerPosition);
                }
            }
        }

        for (String mobId : mobIds) {
            double threshold = getActivationThreshold(mobId, false);
            double detectionRange = getDetectionRange(mobId, false);
            double speed = getSpeed(mobId, false);

            if (isWhispering) {
                detectionRange *= whisperRangeMultiplier;
                speed *= whisperSpeedMultiplier;

                if (sender.getPlayer().getPlayer() instanceof ServerPlayerEntity player) {
                    if (player.isSneaking()) {
                        detectionRange *= sneakingRangeMultiplier;
                    }

                    if (player.getWorld().isRaining() || player.getWorld().isThundering()) {
                        detectionRange *= thunderRangeMultiplier;
                    }
                }
            } else {
                if (sender.getPlayer().getPlayer() instanceof ServerPlayerEntity player) {
                    if (player.isSneaking()) {
                        detectionRange *= sneakingRangeMultiplier;
                    }
                    if (player.getWorld().isRaining() || player.getWorld().isThundering()) {
                        detectionRange *= thunderRangeMultiplier;
                    }
                }
            }

            BlockPos senderPosition = new BlockPos(
                    (int) Math.floor(sender.getPlayer().getPosition().getX()),
                    (int) Math.floor(sender.getPlayer().getPosition().getY()),
                    (int) Math.floor(sender.getPlayer().getPosition().getZ())
            );

            double distance = playerPosition.getSquaredDistance(senderPosition);
            double perceivedIntensity = audioLevel - 20 * Math.log10(distance + 1);

            if (DEBUG) {
                System.out.println("[DEBUG] Perceived Intensity for " + mobId + ": " + perceivedIntensity + " dB at distance " + distance);
            }

            if (perceivedIntensity < threshold) {
                if (DEBUG) {
                    System.out.println("[DEBUG] Intensity too low for " + mobId + ": " + perceivedIntensity + " dB");
                }
                continue;
            }

            if (distance <= detectionRange * detectionRange) {
                playerSoundLocations.put(playerUUID, new SoundData(playerPosition, detectionRange, speed));

                if (DEBUG) {
                    System.out.println("[DEBUG] " + mobId + " detects sound at range " + detectionRange + " with speed " + speed + " from position " + playerPosition);
                }
            }
        }

        // Remove sound data after 5 seconds
        scheduler.schedule(() -> playerSoundLocations.remove(playerUUID), 5, TimeUnit.SECONDS);
    }



    private List<String> getConfiguredMobIds() {
        Map<String, Map<String, Double>> mobConfigs = VoiceConfig.getMobVoiceConfigs();
        return new ArrayList<>(mobConfigs.keySet());
    }

    private List<String> getConfiguredAnimalIds() {
        Map<String, Map<String, Double>> mobConfigs = VoiceConfig.getAnimalVoiceConfigs();
        return new ArrayList<>(mobConfigs.keySet());
    }



    private double getActivationThreshold(String mobId,boolean isAnimal) {
        Map<String, Double> mobConfig = isAnimal ? VoiceConfig.getMobVoiceConfigs().get(mobId):
                VoiceConfig.getAnimalVoiceConfigs().get(mobId);
        if (mobConfig != null && mobConfig.containsKey("threshold")) {
            return mobConfig.get("threshold");
        }
        return -40.0;
    }

    private double getDetectionRange(String mobId,boolean isAnimal) {
        Map<String, Double> mobConfig = isAnimal ? VoiceConfig.getMobVoiceConfigs().get(mobId):
                VoiceConfig.getAnimalVoiceConfigs().get(mobId);
        if (mobConfig != null && mobConfig.containsKey("range")) {
            return mobConfig.get("range");
        }
        return 16.0;
    }

    private double getSpeed(String mobId,boolean isAnimal) {
        Map<String, Double> mobConfig =isAnimal ? VoiceConfig.getMobVoiceConfigs().get(mobId):
                VoiceConfig.getAnimalVoiceConfigs().get(mobId);

        if (mobConfig != null && mobConfig.containsKey("speed")) {
            return mobConfig.get("speed");
        }
        return 1.0;
    }
}
