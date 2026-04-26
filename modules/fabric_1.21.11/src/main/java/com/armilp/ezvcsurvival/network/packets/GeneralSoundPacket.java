package com.armilp.ezvcsurvival.network.packets;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import com.armilp.ezvcsurvival.network.EZVCNetworkService;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.Map;

public class GeneralSoundPacket {
    public static final PacketCodec<PacketByteBuf, GeneralSoundPacket> STREAM_CODEC = PacketCodec.of(GeneralSoundPacket::encode, GeneralSoundPacket::decode);
    private final Identifier sound;
    private final double x;
    private final double y;
    private final double z;
    private final double speedMultiplier;
    private final double rangeMultiplier;
    private PacketByteBuf buf;

    public GeneralSoundPacket(Identifier sound, double x, double y, double z,
                              double speedMultiplier, double rangeMultiplier) {
        this.sound = sound;
        this.x = x;
        this.y = y;
        this.z = z;
        this.speedMultiplier = speedMultiplier;
        this.rangeMultiplier = rangeMultiplier;
    }

    public static void encode(GeneralSoundPacket packet, PacketByteBuf buf) {
        buf.writeIdentifier(packet.sound);
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
        buf.writeDouble(packet.speedMultiplier);
        buf.writeDouble(packet.rangeMultiplier);
    }

    public static GeneralSoundPacket decode(PacketByteBuf buf) {
        Identifier sound = buf.readIdentifier();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        double speedMultiplier = buf.readDouble();
        double rangeMultiplier = buf.readDouble();
        return new GeneralSoundPacket(sound, x, y, z, speedMultiplier, rangeMultiplier);
    }

    public static void handle(GeneralSoundPacket packet, EZVCNetworkService.MessageContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayerEntity player = (ServerPlayerEntity) ctx.getSender();
            if (player == null) return;

            ServerWorld level = player.getEntityWorld();
            Vec3d soundPos = new Vec3d(packet.x, packet.y, packet.z);

            SoundEventTracker.setLastPlayedPosition(packet.sound, soundPos.x, soundPos.y, soundPos.z, packet.speedMultiplier, packet.rangeMultiplier);

            SoundEventTracker.notifyNearbyMobs(level, packet.sound, soundPos.x, soundPos.y, soundPos.z, packet.speedMultiplier, packet.rangeMultiplier);

            Map<String, GeneralSoundsConfig.SoundEntry> soundMap = GeneralSoundsConfig.getSounds();
            if (soundMap != null) {
                GeneralSoundsConfig.SoundEntry cfg = soundMap.get(packet.sound.toString());
                if (cfg != null && cfg.is_priority) {
                    ReactToGeneralSoundGoal.setPrioritySound(soundPos);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}