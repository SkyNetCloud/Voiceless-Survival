package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.EzvcPlatform;
import com.armilp.ezvcsurvival.network.packets.GeneralSoundPacket;
import com.armilp.ezvcsurvival.network.packets.OpenConfigEditorPacket;
import com.armilp.ezvcsurvival.network.packets.UpdateConfigPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public final class EZVCNetwork {
    public static EZVCNetworkService ezvcNetworkService = EzvcPlatform.getInstance().getNetworkService();

    public EZVCNetwork() {
    }
    public static void registerPackets() {
        ezvcNetworkService.registerPacket(GeneralSoundPacket.class, EZVCNetworkService.Direction.CLIENT_TO_SERVER, GeneralSoundPacket::encode, GeneralSoundPacket::decode, GeneralSoundPacket::handle);
        ezvcNetworkService.registerPacket(UpdateConfigPacket.class, EZVCNetworkService.Direction.CLIENT_TO_SERVER, UpdateConfigPacket::encode, UpdateConfigPacket::decode, UpdateConfigPacket::handle);
        // Register S2C packet with no-op handler — handler is set on client side
        ezvcNetworkService.registerPacket(OpenConfigEditorPacket.class, EZVCNetworkService.Direction.SERVER_TO_CLIENT, OpenConfigEditorPacket::encode, OpenConfigEditorPacket::decode, (msg, ctx) -> {});
        ezvcNetworkService.onRegisteringPacketsCompleted();
    }

    public static void clientRegisterPackets() {
        // Re-register with real handler on client side
        ezvcNetworkService.registerPacket(OpenConfigEditorPacket.class, EZVCNetworkService.Direction.SERVER_TO_CLIENT, OpenConfigEditorPacket::encode, OpenConfigEditorPacket::decode, OpenConfigEditorPacket::handle);
        ezvcNetworkService.onRegisteringClientPacketsCompleted();
    }
}