package com.armilp.ezvcsurvival.network;


import com.armilp.ezvcsurvival.network.PointBlankSoundPacket;
import com.armilp.ezvcsurvival.network.SoundPlayedPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public class EZVCNetwork {
    public static final Identifier SOUND_PLAYED_PACKET_ID = new Identifier("ezvcsurvival", "sound_played");
    public static final Identifier POINT_BLANK_SOUND_PACKET_ID = new Identifier("ezvcsurvival", "point_blank_sound");

    public static void registerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(SOUND_PLAYED_PACKET_ID, SoundPlayedPacket::receive);
        ServerPlayNetworking.registerGlobalReceiver(POINT_BLANK_SOUND_PACKET_ID, PointBlankSoundPacket::receive);
    }
}
