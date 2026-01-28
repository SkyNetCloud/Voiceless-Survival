package com.armilp.ezvcsurvival.client.gui;

import com.armilp.ezvcsurvival.client.gui.list.ConfigListScreen;
import com.armilp.ezvcsurvival.util.ModInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfigEditScreen extends Screen {

    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 20;

    private int centerX;
    private int centerY;
    private int startY;

    public ConfigEditScreen() {
        super(Text.translatable("screen.ezvcsurvival.config_editor"));
    }

    @Override
    protected void init() {
        super.init();

        centerX = this.width / 2;
        centerY = this.height / 2;
        startY = Math.max(centerY - 20, 70);

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("button.ezvcsurvival.entity_config"),
                btn -> MinecraftClient.getInstance().setScreen(new ConfigListScreen(ConfigListScreen.ListType.ENTITY_CONFIG, this))
        ).dimensions(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("button.ezvcsurvival.general_sounds"),
                btn -> MinecraftClient.getInstance().setScreen(new ConfigListScreen(ConfigListScreen.ListType.GENERAL_SOUNDS_CONFIG, this))
        ).dimensions(centerX - BUTTON_WIDTH / 2, startY + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("button.ezvcsurvival.gunfire_config"),
                btn -> MinecraftClient.getInstance().setScreen(new ConfigListScreen(ConfigListScreen.ListType.GUNFIRE_CONFIG, this))
        ).dimensions(centerX - BUTTON_WIDTH / 2, startY + BUTTON_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // translucent background
        context.fill(0, 0, this.width, this.height, 0x40000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        this.renderBackground(context, mouseX, mouseY, delta);

        int titleY = Math.max(30, this.height / 10);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX + 1, titleY + 1, 0x88000000);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, titleY, 0xFFFFFF);

        Text subtitle = Text.translatable("screen.ezvcsurvival.config_editor.subtitle");
        int subtitleY = titleY + 20;
        context.drawCenteredTextWithShadow(this.textRenderer, subtitle, centerX, subtitleY, 0xCCCCCC);

        int subtitleWidth = this.textRenderer.getWidth(subtitle);
        int lineY = subtitleY + 12;
        int lineWidth = Math.min(subtitleWidth + 30, this.width - 60);
        context.fill(centerX - lineWidth / 2, lineY,
                centerX + lineWidth / 2, lineY + 1, 0x44FFFFFF);

        Text info = Text.translatable("screen.ezvcsurvival.config_editor.info");
        int infoY = lineY + 15;
        context.drawCenteredTextWithShadow(this.textRenderer, info, centerX, infoY, 0x888888);

        Text version = ModInfo.getVersion();
        int versionWidth = this.textRenderer.getWidth(version);
        context.drawText(this.textRenderer, version, this.width - versionWidth - 10, 10, 0x666666, false);

        super.render(context, mouseX, mouseY, delta);
    }
}
