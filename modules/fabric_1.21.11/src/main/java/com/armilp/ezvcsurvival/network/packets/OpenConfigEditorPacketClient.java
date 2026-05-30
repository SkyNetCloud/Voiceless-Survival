package com.armilp.ezvcsurvival.network.packets;

import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public class OpenConfigEditorPacketClient {
    public static void openScreen() {
        MinecraftClient.getInstance().setScreen(new ConfigEditorScreen());
    }
}
