package com.armilp.ezvcsurvival.network.packets;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.network.EZVCNetworkService;
import com.armilp.ezvcsurvival.voicechat.client.ClientVoiceLevel;
import net.minecraft.network.RegistryByteBuf;

public class VoiceLevelPacket {
    private final double db;

    public VoiceLevelPacket(double db) {
        this.db = db;
    }

    public static void encode(VoiceLevelPacket packet, RegistryByteBuf buf) {
        buf.writeDouble(packet.db);
    }

    public static VoiceLevelPacket decode(RegistryByteBuf buf) {
        return new VoiceLevelPacket(buf.readDouble());
    }

    public static void handle(VoiceLevelPacket packet, EZVCNetworkService.MessageContext ctx) {
        EZVCSurvival.LOGGER.info("[ezvc] client got level {}", packet.db);
        ctx.setPacketHandled(true);
        ClientVoiceLevel.update(packet.db);
    }
}
