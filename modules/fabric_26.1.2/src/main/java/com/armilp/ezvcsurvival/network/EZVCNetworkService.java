package com.armilp.ezvcsurvival.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;
import java.util.function.Function;

public interface EZVCNetworkService {
    <M, B extends RegistryFriendlyByteBuf> void registerPacket(Class<M> var1, Direction var2, BiConsumer<M, RegistryFriendlyByteBuf> var3, Function<RegistryFriendlyByteBuf, M> var4, BiConsumer<M, MessageContext> var5);

    void sendToClient(Object var1, Player var2);

    void sendToServer(Object var1);

    CustomPacketPayload createCustomPayload(Object var1);

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

        Player getSender();

        boolean isClientSide();
    }
}
