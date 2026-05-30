package com.armilp.ezvcsurvival.client;


import com.armilp.ezvcsurvival.EzvcPlatform;
import com.armilp.ezvcsurvival.events.SoundEventHandler;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.platform.fabric.FabricEzvcPlatform;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundInstanceListener;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;


public class EZVCSurvivalClient implements ClientModInitializer {

    private boolean soundManagerRegistered = false;

    @Override
    public void onInitializeClient() {
        FabricEzvcPlatform platform = (FabricEzvcPlatform) EzvcPlatform.getInstance();
        platform.onRegistrationCompleted();
        EZVCNetwork.clientRegisterPackets();
        platform.getNetworkService().onRegisteringClientPacketsCompleted();

    }

}
