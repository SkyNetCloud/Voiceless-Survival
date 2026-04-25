package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.EzvcPlatform;
import com.armilp.ezvcsurvival.network.packets.GeneralSoundPacket;
import com.armilp.ezvcsurvival.network.packets.OpenConfigEditorPacket;
import com.armilp.ezvcsurvival.network.packets.UpdateConfigPacket;

public final class EZVCNetwork {
    public static EZVCNetworkService ezvcNetworkService = EzvcPlatform.getInstance().getNetworkService();

    public EZVCNetwork() {
    }

    public static void registerPackets() {
        ezvcNetworkService.registerPacket(GeneralSoundPacket.class, EZVCNetworkService.Direction.CLIENT_TO_SERVER, GeneralSoundPacket::encode, GeneralSoundPacket::decode, GeneralSoundPacket::handle);
        ezvcNetworkService.registerPacket(UpdateConfigPacket.class, EZVCNetworkService.Direction.CLIENT_TO_SERVER, UpdateConfigPacket::encode, UpdateConfigPacket::decode, UpdateConfigPacket::handle);
        ezvcNetworkService.registerPacket(OpenConfigEditorPacket.class, EZVCNetworkService.Direction.SERVER_TO_CLIENT, OpenConfigEditorPacket::encode, OpenConfigEditorPacket::decode, OpenConfigEditorPacket::handle);
        ezvcNetworkService.onRegisteringPacketsCompleted();
    }
}