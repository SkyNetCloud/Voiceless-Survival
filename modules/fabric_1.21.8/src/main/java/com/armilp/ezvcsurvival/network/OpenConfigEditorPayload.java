package com.armilp.ezvcsurvival.network;


import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;


public record OpenConfigEditorPayload() implements CustomPayload {
    public static final CustomPayload.Id<OpenConfigEditorPayload> ID =
            new CustomPayload.Id<>(Identifier.of("ezvcsurvival", "open_config_editor"));

    public static final PacketCodec<RegistryByteBuf, OpenConfigEditorPayload> CODEC =
            PacketCodec.unit(new OpenConfigEditorPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void registerClientReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            context.client().execute(ClientPayloadHandlers::handleOpenConfigEditor);
        });
    }

    public static void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player != null && player.hasPermissionLevel(2)) { // Ops only
                ServerPlayNetworking.send(player, payload);
            }
        });
    }

    public static void sendToClient(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new OpenConfigEditorPayload());
    }

    public static void sendToServer() {
        ClientPlayNetworking.send(new OpenConfigEditorPayload());
    }
}
