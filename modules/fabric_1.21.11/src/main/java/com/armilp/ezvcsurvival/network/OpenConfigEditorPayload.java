package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.EZVCSurvival;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public record OpenConfigEditorPayload() implements CustomPayload {

    public static final Id<OpenConfigEditorPayload> ID =
            new Id<>(EZVCSurvival.id("open_config_editor"));

    public static final PacketCodec<RegistryByteBuf, OpenConfigEditorPayload> CODEC =
            PacketCodec.unit(new OpenConfigEditorPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
