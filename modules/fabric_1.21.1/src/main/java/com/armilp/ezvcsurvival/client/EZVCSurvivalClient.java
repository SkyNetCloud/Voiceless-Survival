package com.armilp.ezvcsurvival.client;


import com.armilp.ezvcsurvival.network.EZVCNetwork;
import net.fabricmc.api.ClientModInitializer;


public class EZVCSurvivalClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        EZVCNetwork.registerClient();

    }
}
