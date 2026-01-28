package com.armilp.ezvcsurvival.util;

import com.armilp.ezvcsurvival.EZVCSurvival;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.text.Text;

public class ModInfo {


    public static Text getVersion() {
        return Text.literal(EZVCSurvival.MOD_ID + "-v" + getVersionString());
    }

    public static String getVersionString() {
        return FabricLoader.getInstance()
                .getModContainer(EZVCSurvival.MOD_ID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("2.0.0");
    }

}
