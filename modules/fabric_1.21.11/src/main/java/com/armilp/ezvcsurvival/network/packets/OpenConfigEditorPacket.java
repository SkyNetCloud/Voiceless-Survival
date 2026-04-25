package com.armilp.ezvcsurvival.network.packets;

import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
import com.armilp.ezvcsurvival.network.EZVCNetworkService;
import com.armilp.ezvcsurvival.utils.DistExecutor;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;


public class OpenConfigEditorPacket {

    public OpenConfigEditorPacket() {
    }

    public OpenConfigEditorPacket(PacketByteBuf buf) {
    }

    public void encode(PacketByteBuf buf) {
    }

    public static OpenConfigEditorPacket decode(PacketByteBuf buf) {
        return new OpenConfigEditorPacket(buf);
    }

    public void handle(EZVCNetworkService.MessageContext ctx) {
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeCallWhenOn(EnvType.CLIENT, () -> () -> {
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().setScreen(new ConfigEditorScreen());
                });
                return null;
            });
        });
        ctx.setPacketHandled(true);
    }
}
