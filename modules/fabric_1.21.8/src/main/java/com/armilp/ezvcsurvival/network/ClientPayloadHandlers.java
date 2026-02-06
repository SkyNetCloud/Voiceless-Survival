package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
import net.minecraft.client.MinecraftClient;

import java.awt.*;
import java.io.File;

public class ClientPayloadHandlers {
    public static void handleOpenConfigEditor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        try {
            // Try to open the GUI screen
            client.setScreen(new ConfigEditorScreen());
        } catch (Exception e) {
            try {
                // Fallback: Open config directory
                File configDir = new File("config/ezvcsurvival");
                if (!configDir.exists()) {
                    configDir.mkdirs();
                }

                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(configDir);
                }
            } catch (Exception ex) {
                EZVCSurvival.LOGGER.warn("Failed to open the configuration");
            }
        }
    }
}