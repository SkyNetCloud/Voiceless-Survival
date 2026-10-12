package com.armilp.ezvcsurvival.client.gui;

import com.armilp.ezvcsurvival.client.gui.list.ConfigListScreen;
import com.armilp.ezvcsurvival.utils.ModInfo;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;


public class ConfigEditorScreen extends Screen {

    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 25;

    private int centerX;

    public ConfigEditorScreen() {
        super(Component.translatable("screen.ezvcsurvival.config_editor"));
    }

    @Override
    protected void init() {
        super.init();

        centerX = this.width / 2;
        int centerY = this.height / 2;

        int startY = Math.max(centerY - 30, 70);

        this.addRenderableWidget(Button.builder(
                Component.translatable("button.ezvcsurvival.entity_config"),
                btn -> minecraft.setScreen(new ConfigListScreen(ConfigListScreen.ListType.ENTITY_CONFIG, this))
        ).bounds(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("button.ezvcsurvival.general_sounds"),
                btn -> minecraft.setScreen(new ConfigListScreen(ConfigListScreen.ListType.GENERAL_SOUNDS_CONFIG, this))
        ).bounds(centerX - BUTTON_WIDTH / 2, startY + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        if (FabricLoader.getInstance().isModLoaded("tacz")) {
            this.addRenderableWidget(Button.builder(
                    Component.translatable("button.ezvcsurvival.gunfire_config"),
                    btn -> minecraft.setScreen(new ConfigListScreen(ConfigListScreen.ListType.GUNFIRE_CONFIG, this))
            ).bounds(centerX - BUTTON_WIDTH / 2, startY + BUTTON_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }

    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        // Render widgets (buttons) first
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);

        // Render texts directly without manipulating pose
        int titleY = Math.max(30, this.height / 10);

        // Title - FIRST the text, THEN the background so that the background does not cover the text
        context.centeredText(this.font, this.title, centerX, titleY, 0xFFFFFFFF);

        // Caption
        Component subtitle = Component.translatable("screen.ezvcsurvival.config_editor.subtitle");
        int subtitleY = titleY + 15;
        context.centeredText(this.font, subtitle, centerX, subtitleY, 0xFFCCCCCC);

        // decorative line
        int subtitleWidth = this.font.width(subtitle);
        int lineY = subtitleY + 10;
        int lineWidth = Math.min(subtitleWidth + 40, this.width - 80);
        context.fill(centerX - lineWidth / 2, lineY,
                centerX + lineWidth / 2, lineY + 1, 0x88FFFFFF);

        // Additional information
        Component info = Component.translatable("screen.ezvcsurvival.config_editor.info");
        int infoY = lineY + 12;
        context.centeredText(this.font, info, centerX, infoY, 0xFFAAAAAA);

        // Corner version
        Component version = ModInfo.getVersion();
        int versionWidth = this.font.width(version);
        context.text(this.font, version, this.width - versionWidth - 5, 5, 0xFFAAAAAA, true);
    }


    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == 256) {
            minecraft.setScreen(null);
            return true;
        }
        return super.keyPressed(input);
    }


    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

}

