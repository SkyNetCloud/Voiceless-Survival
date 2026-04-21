package com.armilp.ezvcsurvival.network;


import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import static com.armilp.ezvcsurvival.EZVCSurvival.MOD_ID;


public record OpenConfigEditorPayload() implements CustomPacketPayload {

    public static final Identifier OPEN_CONFIG_EDITOR_PAYLOAD = Identifier.fromNamespaceAndPath(MOD_ID, "open_config_editor");
    public static final CustomPacketPayload.Type<OpenConfigEditorPayload> OPEN_CONFIG_EDITOR_TYPE = new CustomPacketPayload.Type<>(OPEN_CONFIG_EDITOR_PAYLOAD);

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenConfigEditorPayload> OPEN_CONFIG_EDITOR_CODEC = StreamCodec.unit(new OpenConfigEditorPayload());


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return OPEN_CONFIG_EDITOR_TYPE;
    }



    public static void registerClientReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(OPEN_CONFIG_EDITOR_TYPE, (payload, context) -> {
            context.client().execute(ClientPayloadHandlers::handleOpenConfigEditor);
        });
    }

    public static void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(OPEN_CONFIG_EDITOR_TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player != null && player.permissions().hasPermission(Permissions.COMMANDS_ADMIN)) { // Ops only
                ServerPlayNetworking.send(player, payload);
            }
        });
    }

    public static void sendToClient(ServerPlayer player) {
        ServerPlayNetworking.send(player, new OpenConfigEditorPayload());
    }

    public static void sendToServer() {
        ClientPlayNetworking.send(new OpenConfigEditorPayload());
    }

}
