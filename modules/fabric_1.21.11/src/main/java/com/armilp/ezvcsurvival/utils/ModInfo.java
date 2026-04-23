package com.armilp.ezvcsurvival.utils;


import com.armilp.ezvcsurvival.EZVCSurvival;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.text.Text;

public class ModInfo {

    public static Text getVersion() {
        return Text.literal(EZVCSurvival.MOD_ID + "-v" + getVersionString());
    }

    public static String getVersionString() {
        ModContainer modContainer = FabricLoader.getInstance().getModContainer(EZVCSurvival.MOD_ID).orElse(null);
        if (modContainer != null) {
            return modContainer.getMetadata().getVersion().getFriendlyString();
        }
        return "2.1.1";
    }

}