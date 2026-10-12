package com.armilp.ezvcsurvival.voicechat.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
;

public class DbMeterOverlay implements HudElement {

    public static final DbMeterOverlay INSTANCE = new DbMeterOverlay();

    private static final Identifier BAR_EMPTY = Identifier.of("ezvcsurvival", "textures/gui/exp_bar.png");
    private static final Identifier BAR_FULL = Identifier.of("ezvcsurvival", "textures/gui/exp_bar_full.png");
    private static final int BAR_WIDTH = 81;
    private static final int BAR_HEIGHT = 6;
    private static final int X = 30;
    private static final int Y_OFFSET_FROM_BOTTOM = 24;
    private static final float TEXT_SCALE = 0.7f;

    @Override
    public void render(DrawContext guiGraphics, RenderTickCounter tickCounter) {
        double level = ClientVoiceLevel.getLevel();
        if (level <= 0.01) return;

        int y = guiGraphics.getScaledWindowHeight() - Y_OFFSET_FROM_BOTTOM;
        int filledWidth = (int) Math.ceil(BAR_WIDTH * level);

        guiGraphics.drawTexture(RenderPipelines.GUI_TEXTURED, BAR_EMPTY, X, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
        if (filledWidth > 0) {
            guiGraphics.drawTexture(RenderPipelines.GUI_TEXTURED, BAR_FULL, X, y, 0, 0, filledWidth, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
        }

        double db = ClientVoiceLevel.MIN_DB + level * (ClientVoiceLevel.MAX_DB - ClientVoiceLevel.MIN_DB);
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        String text = Math.round(db) + " dB";

        guiGraphics.getMatrices().pushMatrix();
        guiGraphics.getMatrices().translate(X + BAR_WIDTH + 5, y - 1);
        guiGraphics.getMatrices().scale(TEXT_SCALE, TEXT_SCALE);
        guiGraphics.drawText(font, text, 0, 0, 0xFFFFFFFF, true); // ARGB: alpha must be FF
        guiGraphics.getMatrices().popMatrix();
    }

}