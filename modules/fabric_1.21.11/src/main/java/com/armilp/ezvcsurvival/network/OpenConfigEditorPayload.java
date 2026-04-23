package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.EZVCSurvival;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenConfigEditorPayload() implements CustomPayload {

    public static final CustomPayload.Id<UpdateConfigPayload> ID =
            new CustomPayload.Id<>(EZVCSurvival.);


    @Override
    public Id<? extends CustomPayload> getId() {
        return null;
    }
}
