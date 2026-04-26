package com.armilp.ezvcsurvival.integration;


import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (currentScreen) -> new ConfigEditorScreen();
    }
}
