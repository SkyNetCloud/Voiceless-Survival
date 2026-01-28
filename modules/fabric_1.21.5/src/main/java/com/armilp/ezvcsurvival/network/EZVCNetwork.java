package com.armilp.ezvcsurvival.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public final class EZVCNetwork {

    // Packet channel identifiers
    public static final Identifier GENERAL_SOUND_CHANNEL =
            Identifier.of("ezvcsurvival", "general_sound");
    public static final Identifier OPEN_CONFIG_EDITOR_CHANNEL =
            Identifier.of("ezvcsurvival", "open_config_editor");
    public static final Identifier UPDATE_CONFIG_CHANNEL =
            Identifier.of("ezvcsurvival", "update_config");

    private EZVCNetwork() {
    }

    public static void registerPackets() {
        // Register server-bound packets (client -> server)
        ClientPlayNetworking.registerGlobalReceiver(GENERAL_SOUND_CHANNEL,
                (client, handler, buf, responseSender) -> {
                    GeneralSoundPacket packet = GeneralSoundPacket.decode(buf);
                    client.execute(() -> {
                        GeneralSoundPacket.handle(client, packet);
                    });
                });

        // Register server-bound packets
        ClientPlayNetworking.registerGlobalReceiver(OPEN_CONFIG_EDITOR_CHANNEL,
                (client, handler, buf, responseSender) -> {
                    OpenConfigEditorPacket packet = OpenConfigEditorPacket.decode(buf);
                    client.execute(() -> {
                        OpenConfigEditorPacket.handle(client, packet);
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(UPDATE_CONFIG_CHANNEL,
                (client, handler, buf, responseSender) -> {
                    UpdateConfigPacket packet = UpdateConfigPacket.decode(buf);
                    client.execute(() -> {
                        UpdateConfigPacket.handle(client, packet);
                    });
                });

        // Register client-bound packets (server -> client) - if needed
        ServerPlayNetworking.registerGlobalReceiver(GENERAL_SOUND_CHANNEL,
                (server, player, handler, buf, responseSender) -> {
                    GeneralSoundPacket packet = GeneralSoundPacket.decode(buf);
                    server.execute(() -> {
                        GeneralSoundPacket.handle(server, player, packet);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(OPEN_CONFIG_EDITOR_CHANNEL,
                (server, player, handler, buf, responseSender) -> {
                    OpenConfigEditorPacket packet = OpenConfigEditorPacket.decode(buf);
                    server.execute(() -> {
                        OpenConfigEditorPacket.handle(server, player, packet);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(UPDATE_CONFIG_CHANNEL,
                (server, player, handler, buf, responseSender) -> {
                    UpdateConfigPacket packet = UpdateConfigPacket.decode(buf);
                    server.execute(() -> {
                        UpdateConfigPacket.handle(server, player, packet);
                    });
                });
    }
}
