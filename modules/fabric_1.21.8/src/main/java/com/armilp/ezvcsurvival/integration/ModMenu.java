package com.armilp.ezvcsurvival.integration;


import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
import com.armilp.ezvcsurvival.client.gui.edit.ConfigEditScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

public class ModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (currentScreen) -> {
            return new ConfigEditorScreen();
        };
    }
}
