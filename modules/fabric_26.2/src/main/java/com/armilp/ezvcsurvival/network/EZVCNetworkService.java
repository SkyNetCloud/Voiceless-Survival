package com.armilp.ezvcsurvival.network;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;

import java.util.function.BiConsumer;
import java.util.function.Function;

public interface EZVCNetworkService {
    <M, B extends PacketByteBuf> void registerPacket(Class<M> var1, Direction var2, BiConsumer<M, RegistryByteBuf> var3, Function<RegistryByteBuf, M> var4, BiConsumer<M, MessageContext> var5);

    void sendToClient(Object var1, PlayerEntity var2);

    void sendToServer(Object var1);

    CustomPayload createCustomPayload(Object var1);

    default void onRegisteringPacketsCompleted() {
    }

    default void onRegisteringClientPacketsCompleted() {
    }


    public static enum Direction {
        CLIENT_TO_SERVER,
        SERVER_TO_CLIENT;

        private Direction() {
        }
    }

    public interface MessageContext {
        void enqueueWork(Runnable var1);

        void setPacketHandled(boolean var1);

        PlayerEntity getSender();

        boolean isClientSide();
    }
}
