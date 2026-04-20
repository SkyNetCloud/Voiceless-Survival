package com.armilp.ezvcsurvival.client.gui;

import com.armilp.ezvcsurvival.client.gui.list.ConfigListScreen;
import com.armilp.ezvcsurvival.utils.ModInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;


public class ConfigEditorScreen extends Screen {

    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;

    private int centerX;
    private int centerY;
    private int startY;

    public ConfigEditorScreen() {
        super(Component.translatable("screen.ezvcsurvival.config_editor"));
    }

    @Override
    protected void init() {
        super.init();

        centerX = this.width / 2;
        centerY = this.height / 2;

        startY = Math.max(centerY - 10, 90);


        this.addRenderableWidget(Button.builder(
                        Component.translatable("button.ezvcsurvival.entity_config"),
                        btn -> Minecraft.getInstance().setScreen(new ConfigListScreen(ConfigListScreen.ListType.ENTITY_CONFIG, this))
                )
                .bounds(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("button.ezvcsurvival.general_sounds"),
                        btn -> Minecraft.getInstance().setScreen(new ConfigListScreen(ConfigListScreen.ListType.GENERAL_SOUNDS_CONFIG, this))
                )
                .bounds(centerX - BUTTON_WIDTH / 2, startY + BUTTON_HEIGHT + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }


    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        // Renderizar los widgets (botones) primero
        super.extractRenderState(graphics, mouseX, mouseY, deltaTicks);

        // Renderizar textos directamente sin manipular pose
        int titleY = Math.max(30, this.height / 10);

        // Título - PRIMERO el texto, LUEGO el fondo para que el fondo no tape el texto
        graphics.text(this.font, this.title, centerX, titleY, 0xFFFFFFFF);

        // Subtítulo
        Component subtitle = Component.translatable("screen.ezvcsurvival.config_editor.subtitle");
        int subtitleY = titleY + 15;
        graphics.text(this.font, subtitle, centerX, subtitleY, 0xFFCCCCCC);


        int subtitleWidth = this.font.width(subtitle);
        int lineY = subtitleY + 10;
        int lineWidth = Math.min(subtitleWidth + 40, this.width - 80);
        graphics.fill(centerX - lineWidth / 2, lineY,
                centerX + lineWidth / 2, lineY + 1, 0x88FFFFFF);


        Component info = Component.translatable("screen.ezvcsurvival.config_editor.info");
        int infoY = lineY + 12;
        graphics.text(this.font, info, centerX, infoY, 0xFFAAAAAA);

        Component version = ModInfo.getVersion();
        int versionWidth = this.font.width(version);
        graphics.text(this.font, version, this.width - versionWidth - 5, 5, 0xFFAAAAAA, true);
    }



    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == 256) {
            Minecraft.getInstance().setScreen(null);
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
