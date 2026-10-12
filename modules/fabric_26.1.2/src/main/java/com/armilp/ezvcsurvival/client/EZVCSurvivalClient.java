package com.armilp.ezvcsurvival.client;


import com.armilp.ezvcsurvival.EzvcPlatform;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.platform.fabric.FabricEzvcPlatform;
import com.armilp.ezvcsurvival.voicechat.client.DbMeterOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

public class EZVCSurvivalClient implements ClientModInitializer {


    @Override
    public void onInitializeClient() {
        FabricEzvcPlatform platform = (FabricEzvcPlatform) EzvcPlatform.getInstance();
        platform.onRegistrationCompleted();
        EZVCNetwork.clientRegisterPackets();
        platform.getNetworkService().onRegisteringClientPacketsCompleted();

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("ezvcsurvival", "hud_test"),
                DbMeterOverlay.INSTANCE
        );

    }

}
