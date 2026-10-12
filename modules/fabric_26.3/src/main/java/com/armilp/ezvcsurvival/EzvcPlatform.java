package com.armilp.ezvcsurvival;

import com.armilp.ezvcsurvival.network.EZVCNetworkService;


import java.util.ServiceLoader;

public interface EzvcPlatform {

    EzvcPlatform INSTNANCE = load(EzvcPlatform.class);

    static EzvcPlatform getInstance() {
        return INSTNANCE;
    }

    private static <T> T load(Class<T> clazz) {
        T loadedService = (T) ServiceLoader.load(clazz).findFirst().orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        EZVCSurvival.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }

    EZVCNetworkService getNetworkService();

    void onRegistrationCompleted();
}
