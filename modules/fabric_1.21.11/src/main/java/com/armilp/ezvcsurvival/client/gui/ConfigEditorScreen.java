package com.armilp.ezvcsurvival.client.gui;

import com.armilp.ezvcsurvival.client.gui.list.ConfigListScreen;
import com.armilp.ezvcsurvival.utils.ModInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public class ConfigEditorScreen extends Screen {

    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;

    private int centerX;
    private int centerY;
    private int startY;

    public ConfigEditorScreen() {
        super(Text.translatable("screen.ezvcsurvival.config_editor"));
    }

    @Override
    protected void init() {
        super.init();

        centerX = this.width / 2;
        centerY = this.height / 2;

        startY = Math.max(centerY - 10, 90);

        // For Fabric, use TextWidget for button labels (ButtonWidget is Forge-specific)
        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("button.ezvcsurvival.entity_config"),
                        btn -> client.setScreen(new ConfigListScreen(ConfigListScreen.ListType.ENTITY_CONFIG, this))
                )
                .dimensions(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("button.ezvcsurvival.general_sounds"),
                        btn -> client.setScreen(new ConfigListScreen(ConfigListScreen.ListType.GENERAL_SOUNDS_CONFIG, this))
                )
                .dimensions(centerX - BUTTON_WIDTH / 2, startY + BUTTON_HEIGHT + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // Renderizar los widgets (botones) primero
        super.render(context, mouseX, mouseY, deltaTicks);

        // Renderizar textos directamente sin manipular pose
        int titleY = Math.max(30, this.height / 10);

        // Título - PRIMERO el texto, LUEGO el fondo para que el fondo no tape el texto
        context.drawCenteredTextWithShadow(getTextRenderer(), this.title, centerX, titleY, 0xFFFFFFFF);

        // Subtítulo
        Text subtitle = Text.translatable("screen.ezvcsurvival.config_editor.subtitle");
        int subtitleY = titleY + 15;
        context.drawCenteredTextWithShadow(getTextRenderer(), subtitle, centerX, subtitleY, 0xFFCCCCCC);

        // Línea decorativa
        int subtitleWidth = getTextRenderer().getWidth(subtitle);
        int lineY = subtitleY + 10;
        int lineWidth = Math.min(subtitleWidth + 40, this.width - 80);
        context.fill(centerX - lineWidth / 2, lineY,
                centerX + lineWidth / 2, lineY + 1, 0x88FFFFFF);

        // Información adicional
        Text info = Text.translatable("screen.ezvcsurvival.config_editor.info");
        int infoY = lineY + 12;
        context.drawCenteredTextWithShadow(getTextRenderer(), info, centerX, infoY, 0xFFAAAAAA);

        // Versión en la esquina
        Text version = ModInfo.getVersion();
        int versionWidth = getTextRenderer().getWidth(version);
        context.drawText(getTextRenderer(), version, this.width - versionWidth - 5, 5, 0xFFAAAAAA, true);
    }


    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == 256) {
            MinecraftClient.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(input);
    }


    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }
    @Override
    public boolean shouldPause() {
        return false;
    }
}
