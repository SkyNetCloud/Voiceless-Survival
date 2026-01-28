package com.armilp.ezvcsurvival.network;

import com.mojang.datafixers.types.Type;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class OpenConfigEditorPacket implements CustomPacketPayload {

    public static final CustomPayload.Type<OpenConfigEditorPacket> TYPE = new Type<>(
            Identifier.of("open_config_editor")
    );

    public OpenConfigEditorPacket() {}

    public OpenConfigEditorPacket(PacketByteBuf buf) {}

    @Override
    public void write(PacketByteBuf buf) {}

    public static final StreamCodec<PacketByteBuf, OpenConfigEditorPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> packet.write(buf),
            buf -> new OpenConfigEditorPacket(buf)
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // Server-side packet handler
    public static class ServerHandler {
        public static void register() {
            ServerPlayNetworking.registerGlobalReceiver(TYPE, (packet, player, responseSender) -> {
                // Handle packet on server
                // Send to client if needed
                responseSender.sendPacket(packet);
            });
        }
    }

    // Client-side packet handler
    public static class ClientHandler {
        public static void register() {
            ClientPlayNetworking.registerGlobalReceiver(TYPE, (packet, player) -> {
                handleClientSide();
            });
        }

        private static void handleClientSide() {
            // Use DistExecutor equivalent for Fabric
            // For Fabric, you'd typically check the environment
            if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                handleOpenConfigEditor();
            }
        }

        private static void handleOpenConfigEditor() {
            // Your client-side handling code here
            // This will only run on client
            // Open GUI, etc.
        }
    }
}
