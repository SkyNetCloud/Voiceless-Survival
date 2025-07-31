package com.armilp.ezvcsurvival;


import com.armilp.ezvcsurvival.compat.audio.AudioModifierFactory;
import com.armilp.ezvcsurvival.compat.audio.modifier.IAudioModifier;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.data.SoundData;
import com.armilp.ezvcsurvival.event.ArmorEventHandler;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
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

public class Plugin implements VoicechatPlugin {

    public static final boolean DEBUG = false;
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

    @SuppressWarnings("unused")
    public static double calculateAudioLevel(short[] samples) {
        double rms = 0D;
        for (short sample : samples) {
            double normalizedSample = (double) sample / (double) Short.MAX_VALUE;
            rms += sample * sample;
        }
        int sampleCount = samples.length;
        rms = (sampleCount == 0) ? 0 : Math.sqrt(rms / sampleCount);
        if (rms > 0D){
            return Math.min(Math.max(20D * Math.log10(rms), -127D), 0D);
        } else {
            return -127D;
        }
    }

    @SuppressWarnings("unused")
    public static BlockPos getLastSoundLocation(BlockPos zombiePosition, double range) {
        return playerSoundLocations.values().stream()
                .filter(data -> zombiePosition.getSquaredDistance(data.position()) <= data.range() * data.range())
                .min(Comparator.comparingDouble(data -> zombiePosition.getSquaredDistance(data.position())))
                .map(SoundData::position)
                .orElse(null);
    }

    @SuppressWarnings("unused")
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

        // Check if the player is in creative mode
        if (sender.getPlayer().getPlayer() instanceof ServerPlayerEntity player) {
            if (player.isCreative()) {
                return; // Cancel processing if the player is in creative mode
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
        de.maxhenkel.voicechat.api.Position voicechatPosition = sender.getPlayer().getPosition();
        BlockPos playerPosition = new BlockPos(
                (int) Math.floor(voicechatPosition.getX()),
                (int) Math.floor(voicechatPosition.getY()),
                (int) Math.floor(voicechatPosition.getZ())
        );

        boolean isWhispering = event.getPacket().isWhispering();

        double whisperRangeMultiplier = VoiceConfig.getWhisperConfig().rangeMultiplier;
        double whisperSpeedMultiplier = VoiceConfig.getWhisperConfig().speedMultiplier;
        double thunderRangeMultiplier = VoiceConfig.getMiscConfig().thunderRangeMultiplier;
        double sneakingRangeMultiplier = VoiceConfig.getMiscConfig().sneakingRangeMultiplier;

        List<String> mobIds = getConfiguredMobIds();
        List<String> animalIds = getConfiguredAnimalIds();

        for (String animalId : animalIds) {
            double threshold = getActivationThreshold(animalId);
            double detectionRange = getDetectionRange(animalId);
            double speed = getSpeed(animalId);

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
                    double[] armorMult = ArmorEventHandler.getArmorMultipliers(player);
                    detectionRange *= armorMult[1];
                    speed *= armorMult[0];
                }
            } else {
                if (sender.getPlayer().getPlayer() instanceof ServerPlayerEntity player) {
                    if (player.isSneaking()) {
                        detectionRange *= sneakingRangeMultiplier;
                    }
                    if (player.getWorld().isRaining() || player.getWorld().isThundering()) {
                        detectionRange *= thunderRangeMultiplier;
                    }
                    double[] armorMult = ArmorEventHandler.getArmorMultipliers(player);
                    detectionRange *= armorMult[1];
                    speed *= armorMult[0];
                }
            }

            BlockPos senderPosition = new BlockPos(
                    (int) Math.floor(sender.getPlayer().getPosition().getX()),
                    (int) Math.floor(sender.getPlayer().getPosition().getY()),
                    (int) Math.floor(sender.getPlayer().getPosition().getZ())
            );

            Vec3d senderVec = new Vec3d(voicechatPosition.getX(), voicechatPosition.getY(), voicechatPosition.getZ());
            Vec3d playerVec = new Vec3d(voicechatPosition.getX(), voicechatPosition.getY(), voicechatPosition.getZ());

            IAudioModifier audioModifier = AudioModifierFactory.createAudioModifier(0.5, "voicechat", playerVec, senderVec);
            detectionRange = audioModifier.computeModifiedRange(detectionRange);

            double distance = Math.sqrt(playerPosition.getSquaredDistance(senderPosition));
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

            if (playerPosition.getSquaredDistance(senderPosition) <= detectionRange * detectionRange) {
                playerSoundLocations.put(playerUUID, new SoundData(playerPosition, detectionRange, speed));

                if (DEBUG) {
                    System.out.println("[DEBUG] " + animalId + " detects sound at range " + detectionRange + " with speed " + speed + " from position " + playerPosition);
                }
            }
        }

        for (String mobId : mobIds) {
            double threshold = getActivationThreshold(mobId);
            double detectionRange = getDetectionRange(mobId);
            double speed = getSpeed(mobId);

            if (isWhispering) {
                detectionRange *= whisperRangeMultiplier;
                speed *= whisperSpeedMultiplier;

                Object minecraftPlayer = sender.getPlayer().getPlayer();
                if (minecraftPlayer instanceof ServerPlayerEntity player) {
                    if (player.isSneaking()) {
                        detectionRange *= sneakingRangeMultiplier;
                    }

                    if (player.getWorld().isRaining() || player.getWorld().isThundering()) {
                        detectionRange *= thunderRangeMultiplier;
                    }
                    double[] armorMult = ArmorEventHandler.getArmorMultipliers(player);
                    detectionRange *= armorMult[1];
                    speed *= armorMult[0];
                }
            } else {
                Object minecraftPlayer = sender.getPlayer().getPlayer();
                if (minecraftPlayer instanceof ServerPlayerEntity player) {
                    if (player.isSneaking()) {
                        detectionRange *= sneakingRangeMultiplier;
                    }
                    if (player.getWorld().isRaining() || player.getWorld().isThundering()) {
                        detectionRange *= thunderRangeMultiplier;
                    }
                    double[] armorMult = ArmorEventHandler.getArmorMultipliers(player);
                    detectionRange *= armorMult[1];
                    speed *= armorMult[0];
                }
            }

            BlockPos senderPosition = new BlockPos(
                    (int) Math.floor(sender.getPlayer().getPosition().getX()),
                    (int) Math.floor(sender.getPlayer().getPosition().getY()),
                    (int) Math.floor(sender.getPlayer().getPosition().getZ())
            );

            Vec3d senderVec = new Vec3d(voicechatPosition.getX(), voicechatPosition.getY(), voicechatPosition.getZ());
            Vec3d playerVec = new Vec3d(voicechatPosition.getX(), voicechatPosition.getY(), voicechatPosition.getZ());

            IAudioModifier audioModifier = AudioModifierFactory.createAudioModifier(0.5, "voicechat", playerVec, senderVec);
            detectionRange = audioModifier.computeModifiedRange(detectionRange);

            double distance = Math.sqrt(playerPosition.getSquaredDistance(senderPosition));
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

            if (playerPosition.getSquaredDistance(senderPosition) <= detectionRange * detectionRange) {
                playerSoundLocations.put(playerUUID, new SoundData(playerPosition, detectionRange, speed));

                if (DEBUG) {
                    System.out.println("[DEBUG] " + mobId + " detects sound at range " + detectionRange + " with speed " + speed + " from position " + playerPosition);
                }
            }
        }
        scheduler.schedule(() -> playerSoundLocations.remove(playerUUID), 5, TimeUnit.SECONDS);
    }
    private List<String> getConfiguredMobIds() {
        Map<String, VoiceConfig.VoiceAttributes> mobConfigs = VoiceConfig.getMobVoiceConfigs();
        return new ArrayList<>(mobConfigs.keySet());
    }

    private List<String> getConfiguredAnimalIds() {
        Map<String, VoiceConfig.VoiceAttributes> mobConfigs = VoiceConfig.getAnimalVoiceConfigs();
        return new ArrayList<>(mobConfigs.keySet());
    }

    private double getActivationThreshold(String mobId) {
        VoiceConfig.VoiceAttributes attributes = VoiceConfig.getMobVoiceConfigs().get(mobId);
        if (attributes != null) {
            return attributes.threshold;
        }
        return -40.0;
    }

    private double getDetectionRange(String mobId) {
        VoiceConfig.VoiceAttributes attributes = VoiceConfig.getMobVoiceConfigs().get(mobId);
        if (attributes != null) {
            return attributes.range;
        }
        return 16.0;
    }

    private double getSpeed(String mobId) {
        VoiceConfig.VoiceAttributes attributes = VoiceConfig.getMobVoiceConfigs().get(mobId);
        if (attributes != null) {
            return attributes.speed;
        }
        return 1.0;
    }

}
