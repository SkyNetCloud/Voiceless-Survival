package com.armilp.ezvcsurvival.platform.fabric;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import static com.armilp.ezvcsurvival.EZVCSurvival.MOD_ID;

public class FabricEzvcPayload<M> implements CustomPacketPayload {
    private final M data;
    private final CustomPacketPayload.Type<? extends CustomPacketPayload> type;

    @SuppressWarnings("unchecked")
    FabricEzvcPayload(M data) {
        this.data = data;
        // Using ResourceLocation (MojMap) instead of Identifier
        Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, data.getClass().getSimpleName().toLowerCase());
        this.type = new CustomPacketPayload.Type<>(id);
    }


    public CustomPacketPayload.@NotNull Type<? extends CustomPacketPayload> getId() {
        return this.type;
    }

    public M getData() {
        return this.data;
    }

    @Override
    @NotNull
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.type;
    }
}
