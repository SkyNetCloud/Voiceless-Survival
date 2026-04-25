package com.armilp.ezvcsurvival.client;


import com.armilp.ezvcsurvival.EzvcPlatform;
import com.armilp.ezvcsurvival.platform.fabric.FabricEzvcPlatform;
import net.fabricmc.api.ClientModInitializer;


public class EZVCSurvivalClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricEzvcPlatform platform = (FabricEzvcPlatform) EzvcPlatform.getInstance();
        platform.onRegistrationCompleted();

        platform.getNetworkService().onRegisteringClientPacketsCompleted();
    }
}
