package com.armilp.ezvcsurvival.network;


import com.armilp.ezvcsurvival.event.SoundEventTracker;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class PointBlankSoundPacket {
    private final Identifier sound;
    private final double x;
    private final double y;
    private final double z;

    public PointBlankSoundPacket(Identifier sound, double x, double y, double z) {
        this.sound = sound;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void encode(PacketByteBuf buf) {
        buf.writeIdentifier(sound);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public static PointBlankSoundPacket decode(PacketByteBuf buf) {
        Identifier sound = buf.readIdentifier();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        return new PointBlankSoundPacket(sound, x, y, z);
    }

    public static void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        PointBlankSoundPacket packet = decode(buf);
        server.execute(() -> {
            if (packet.sound.getNamespace().equals("pointblank") && !packet.sound.getPath().contains("_s")) {
                SoundEventTracker.setLastPlayedPosition(packet.sound, packet.x, packet.y, packet.z);
            }
        });
    }
}
