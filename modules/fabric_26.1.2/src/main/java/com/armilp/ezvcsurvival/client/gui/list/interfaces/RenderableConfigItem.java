package com.armilp.ezvcsurvival.client.gui.list.interfaces;

import net.minecraft.network.chat.Component;

public interface RenderableConfigItem {
    Component getDisplayName();
    boolean isEnabled();
    boolean shouldTruncate();
}
