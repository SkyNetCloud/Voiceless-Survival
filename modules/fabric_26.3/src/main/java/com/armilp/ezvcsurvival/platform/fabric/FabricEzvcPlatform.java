package com.armilp.ezvcsurvival.platform.fabric;

import com.armilp.ezvcsurvival.EzvcPlatform;
import com.armilp.ezvcsurvival.network.EZVCNetworkService;

public class FabricEzvcPlatform implements EzvcPlatform {

    private final FabricEzvcNetworkService networkService;

    public FabricEzvcPlatform(FabricEzvcNetworkService networkService) {
        this.networkService = networkService;
    }

    public FabricEzvcPlatform() {
        this.networkService = new FabricEzvcNetworkService();
    }

    public EZVCNetworkService getNetworkService() {
        return this.networkService;
    }


    public void onRegistrationCompleted() {

    }
}
