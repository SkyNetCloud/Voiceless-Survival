package com.armilp.ezvcsurvival.platform.fabric;


import com.armilp.ezvcsurvival.network.EZVCNetworkService;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.ValueFirstEncoder;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.armilp.ezvcsurvival.EZVCSurvival.MOD_ID;

public class FabricEzvcNetworkService implements EZVCNetworkService {
    private final LinkedHashMap<Class<?>, PacketInfo<?>> packetRegistrations = new LinkedHashMap<>();

    public FabricEzvcNetworkService() {
    }



    @Override
    public <M, B extends PacketByteBuf> void registerPacket(Class<M> var1, Direction var2, BiConsumer<M, RegistryByteBuf> var3, Function<RegistryByteBuf, M> var4, BiConsumer<M, MessageContext> var5) {
        Objects.requireNonNull(var3);
        ValueFirstEncoder<RegistryByteBuf, M> var10000 = var3::accept;
        Objects.requireNonNull(var4);
        PacketCodec<RegistryByteBuf, M> mCodec = PacketCodec.of(var10000, var4::apply);
        PacketCodec<? super RegistryByteBuf, FabricEzvcPayload<M>> codec = PacketCodec.tuple(mCodec, FabricEzvcPayload::getData, FabricEzvcPayload::new);
        CustomPayload.Id<FabricEzvcPayload<M>> type = new CustomPayload.Id<>(Identifier.of(MOD_ID, var1.getSimpleName().toLowerCase()));
        this.packetRegistrations.put(var1, new PacketInfo<>(var2, codec, type, (p, c) -> var5.accept(p.getData(), c)));
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

    private <T> void registerPacket(FabricEzvcNetworkService.PacketInfo<T> packetInfo) {
        if (packetInfo.direction == EZVCNetworkService.Direction.CLIENT_TO_SERVER) {
            PayloadTypeRegistry.playC2S().register(packetInfo.type, packetInfo.codec);
            ServerPlayNetworking.registerGlobalReceiver(packetInfo.type, (p, c) -> packetInfo.handler.accept(p, new FabricEzvcNetworkService.FabricC2SMessageContext(c)));
        } else if (packetInfo.direction == EZVCNetworkService.Direction.SERVER_TO_CLIENT) {
            PayloadTypeRegistry.playS2C().register(packetInfo.type, packetInfo.codec);
        }

    }

    private <T> void registerClientPacketReceiver(FabricEzvcNetworkService.PacketInfo<T> packetInfo) {
        if (packetInfo.direction == EZVCNetworkService.Direction.SERVER_TO_CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(packetInfo.type, (p, c) -> packetInfo.handler.accept(p, new FabricEzvcNetworkService.FabricS2CMessageContext(c)));
        }

    }



    public void sendToClient(Object message, PlayerEntity toPlayer) {
        ServerPlayNetworking.send((ServerPlayerEntity)toPlayer, new FabricEzvcPayload(message));
    }

    public void sendToServer(Object message) {
        ClientPlayNetworking.send(new FabricEzvcPayload(message));
    }

    public CustomPayload createCustomPayload(Object packet) {
        return new FabricEzvcPayload(packet);
    }

    private static record PacketInfo<T>(EZVCNetworkService.Direction direction, PacketCodec<? super RegistryByteBuf, FabricEzvcPayload<T>> codec, CustomPayload.Id<FabricEzvcPayload<T>> type, BiConsumer<FabricEzvcPayload<T>, MessageContext> handler) {
    }

    static class FabricS2CMessageContext implements EZVCNetworkService.MessageContext {
        private final ClientPlayNetworking.Context delegate;

        FabricS2CMessageContext(ClientPlayNetworking.Context delegate) {
            this.delegate = delegate;
        }

        public void enqueueWork(Runnable runnable) {
            MinecraftClient client = this.delegate.client();
            client.execute(runnable);
        }

        public void setPacketHandled(boolean isPacketHandled) {
        }

        public PlayerEntity getSender() {
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
            this.delegate.player().getEntityWorld().getServer().execute(runnable);
        }

        public void setPacketHandled(boolean isPacketHandled) {
        }

        public PlayerEntity getSender() {
            return this.delegate.player();
        }

        public boolean isClientSide() {
            return false;
        }
    }
}
