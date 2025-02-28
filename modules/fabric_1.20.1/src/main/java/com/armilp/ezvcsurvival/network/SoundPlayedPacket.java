package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.commands.SoundEffectCommand;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class SoundPlayedPacket {
    private final String soundName;

    public SoundPlayedPacket(String soundName) {
        this.soundName = soundName;
    }

    public void encode(PacketByteBuf buf) {
        buf.writeString(soundName);
    }

    public static SoundPlayedPacket decode(PacketByteBuf buf) {
        return new SoundPlayedPacket(buf.readString());
    }

    public static void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        SoundPlayedPacket packet = decode(buf);
        server.execute(() -> {
            if (player != null) {
                SoundEffectCommand.applyEffect(player);
            }
        });
    }
}
