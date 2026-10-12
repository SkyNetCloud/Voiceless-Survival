package com.armilp.ezvcsurvival.platform.fabric;


import com.armilp.ezvcsurvival.network.EZVCNetworkService;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.armilp.ezvcsurvival.EZVCSurvival.MOD_ID;

public class FabricEzvcNetworkService implements EZVCNetworkService {
    private final LinkedHashMap<Class<?>, PacketInfo<?>> packetRegistrations = new LinkedHashMap<>();

    public FabricEzvcNetworkService() {
    }

    @Override
    public <M, B extends RegistryFriendlyByteBuf> void registerPacket(
            Class<M> clazz,
            Direction dir,
            BiConsumer<M, RegistryFriendlyByteBuf> encoder,
            Function<RegistryFriendlyByteBuf, M> decoder,
            BiConsumer<M, MessageContext> handler) {
        StreamCodec<RegistryFriendlyByteBuf, M> mCodec =
                StreamCodec.of((buf, msg) -> encoder.accept(msg, buf), decoder::apply);
        StreamCodec<? super RegistryFriendlyByteBuf, FabricEzvcPayload<M>> codec =
                StreamCodec.composite(mCodec, FabricEzvcPayload::getData, FabricEzvcPayload::new);
        CustomPacketPayload.Type<FabricEzvcPayload<M>> type = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(MOD_ID, clazz.getSimpleName().toLowerCase()));
        this.packetRegistrations.put(clazz,
                new PacketInfo<>(dir, codec, type, (p, c) -> handler.accept(p.getData(), c)));
    }




    public void onRegisteringPacketsCompleted() {
        for(Map.Entry<Class<?>, FabricEzvcNetworkService.PacketInfo<?>> entry : this.packetRegistrations.entrySet()) {
            FabricEzvcNetworkService.PacketInfo<?> packetInfo = (FabricEzvcNetworkService.PacketInfo)entry.getValue();
            this.registerPacket(packetInfo);
        }

    }

    public void onRegisteringClientPacketsCompleted() {
        for(Map.Entry<Class<?>, FabricEzvcNetworkService.PacketInfo<?>> entry : this.packetRegistrations.entrySet()) {
            FabricEzvcNetworkService.PacketInfo<?> packetInfo = (FabricEzvcNetworkService.PacketInfo)entry.getValue();
            this.registerClientPacketReceiver(packetInfo);
        }

    }

    private <T> void registerPacket(PacketInfo<T> info) {
        if (info.direction == EZVCNetworkService.Direction.CLIENT_TO_SERVER) {
            PayloadTypeRegistry.serverboundPlay().register(info.type, info.codec);
            ServerPlayNetworking.registerGlobalReceiver(info.type,
                    (p, c) -> info.handler.accept(p, new FabricC2SMessageContext(c)));
        } else if (info.direction == EZVCNetworkService.Direction.SERVER_TO_CLIENT) {
            PayloadTypeRegistry.clientboundPlay().register(info.type, info.codec);
        }
    }

    private <T> void registerClientPacketReceiver(FabricEzvcNetworkService.PacketInfo<T> packetInfo) {
        if (packetInfo.direction == EZVCNetworkService.Direction.SERVER_TO_CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(packetInfo.type, (p, c) -> packetInfo.handler.accept(p, new FabricEzvcNetworkService.FabricS2CMessageContext(c)));
        }

    }




    public void sendToClient(Object message, Player toPlayer) {
        ServerPlayNetworking.send((ServerPlayer) toPlayer, new FabricEzvcPayload(message));
    }

    public void sendToServer(Object message) {
        ClientPlayNetworking.send(new FabricEzvcPayload(message));
    }

    public CustomPacketPayload createCustomPayload(Object packet) {
        return new FabricEzvcPayload(packet);
    }

    private static record PacketInfo<T>(EZVCNetworkService.Direction direction, StreamCodec<? super RegistryFriendlyByteBuf, FabricEzvcPayload<T>> codec, CustomPacketPayload.Type<FabricEzvcPayload<T>> type, BiConsumer<FabricEzvcPayload<T>, MessageContext> handler) {
    }

    static class FabricS2CMessageContext implements EZVCNetworkService.MessageContext {
        private final ClientPlayNetworking.Context delegate;

        FabricS2CMessageContext(ClientPlayNetworking.Context delegate) {
            this.delegate = delegate;
        }

        public void enqueueWork(Runnable runnable) {
            Minecraft client = this.delegate.client();
            client.execute(runnable);
        }

        public void setPacketHandled(boolean isPacketHandled) {
        }

        public Player getSender() {
            return this.delegate.player();
        }

        public boolean isClientSide() {
            return false;
        }
    }

    static class FabricC2SMessageContext implements EZVCNetworkService.MessageContext {
        private final ServerPlayNetworking.Context delegate;

        FabricC2SMessageContext(ServerPlayNetworking.Context delegate) {
            this.delegate = delegate;
        }

        public void enqueueWork(Runnable runnable) {
            this.delegate.player().level().getServer().execute(runnable);
        }

        public void setPacketHandled(boolean isPacketHandled) {
        }

        public Player getSender() {
            return this.delegate.player();
        }

        public boolean isClientSide() {
            return false;
        }
    }
}
