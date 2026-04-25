package com.armilp.ezvcsurvival.platform.fabric;

import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import static com.armilp.ezvcsurvival.EZVCSurvival.MOD_ID;

public class FabricEzvcPayload<M> implements CustomPayload {
    private final M data;
    private final CustomPayload.Id<? extends CustomPayload> type;

    FabricEzvcPayload(M data) {
        this.data = data;
        this.type = new CustomPayload.Id(Identifier.of(MOD_ID, data.getClass().getSimpleName().toLowerCase()));
    }


    public CustomPayload.@NotNull Id<? extends CustomPayload> getId() {
        return this.type;
    }

    public M getData() {
        return this.data;
    }
}
