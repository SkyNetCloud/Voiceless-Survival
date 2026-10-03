package com.armilp.ezvcsurvival.voicechat.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;


public class DbMeterOverlay implements HudElement {

    public static final DbMeterOverlay INSTANCE = new DbMeterOverlay();

    private static final Identifier BAR_EMPTY = Identifier.fromNamespaceAndPath("ezvcsurvival", "textures/gui/exp_bar.png");
    private static final Identifier BAR_FULL = Identifier.fromNamespaceAndPath("ezvcsurvival", "textures/gui/exp_bar_full.png");
    private static final int BAR_WIDTH = 81;
    private static final int BAR_HEIGHT = 6;
    private static final int X = 30;
    private static final int Y_OFFSET_FROM_BOTTOM = 24;
    private static final float TEXT_SCALE = 0.7f;



    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        double level = ClientVoiceLevel.getLevel();
        if (level <= 0.01) return;

        int y = guiGraphics.guiHeight() - Y_OFFSET_FROM_BOTTOM;
        int filledWidth = (int) Math.ceil(BAR_WIDTH * level);

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BAR_EMPTY, X, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
        if (filledWidth > 0) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BAR_FULL, X, y, 0, 0, filledWidth, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
        }

        double db = ClientVoiceLevel.MIN_DB + level * (ClientVoiceLevel.MAX_DB - ClientVoiceLevel.MIN_DB);
        Font font = Minecraft.getInstance().font;
        String text = Math.round(db) + " dB";

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(X + BAR_WIDTH + 5, y - 1);
        guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE);
        guiGraphics.text(font, text, 0, 0, 0xFFFFFFFF, true);
        guiGraphics.pose().popMatrix();
    }

}