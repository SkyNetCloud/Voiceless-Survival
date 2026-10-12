package com.armilp.ezvcsurvival.network.packets;


import com.armilp.ezvcsurvival.network.EZVCNetworkService;
import com.armilp.ezvcsurvival.utils.DistExecutor;
import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;


public class OpenConfigEditorPacket {

    public OpenConfigEditorPacket() {
    }

    public OpenConfigEditorPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public static OpenConfigEditorPacket decode(FriendlyByteBuf buf) {
        return new OpenConfigEditorPacket(buf);
    }

    public void handle(EZVCNetworkService.MessageContext ctx) {
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeCallWhenOn(EnvType.CLIENT, () -> () -> {
                Minecraft.getInstance().execute(OpenConfigEditorPacketClient::openScreen);
                return null;
            });
        });
        ctx.setPacketHandled(true);
    }
}
