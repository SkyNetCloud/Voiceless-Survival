package com.armilp.ezvcsurvival.client.gui.list;

import com.armilp.ezvcsurvival.client.gui.edit.ConfigEditScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.report.ChatSelectionScreen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


import java.util.ArrayList;
import java.util.List;

public class ConfigListWidget extends AlwaysSelectedEntryListWidget<ConfigListWidget.Entry> {

    private final ConfigListScreen parent;
    private static final int MIN_ENTRY_WIDTH = 300;
    private static final int STATUS_AREA_WIDTH = 120;
    private static final int PADDING = 10;
    private static TextRenderer textrender = MinecraftClient.getInstance().textRenderer;

    public ConfigListWidget(ConfigListScreen parent, MinecraftClient mc, int width, int height, int top, int bottom, int itemHeight) {
        super(mc, width, height, top, bottom, itemHeight);
        this.parent = parent;
    }

    public void addItem(Object item) {
        this.addEntry(new UniversalEntry(item));
    }

    public void clear() {
        this.clearEntries();
    }

    public int getEntryIndexAt(double mouseX, double mouseY) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            return -1;
        }

        int rowLeft = this.getRowLeft();
        int rowWidth = this.getRowWidth();

        int count = this.children().size();
        for (int i = 0; i < count; i++) {
            int rowTop = this.getRowTop(i);
            int rowBottom = rowTop + this.itemHeight;

            if (mouseX >= rowLeft && mouseX <= rowLeft + rowWidth &&
                    mouseY >= rowTop && mouseY <= rowBottom) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getRowWidth() {
        return Math.max(MIN_ENTRY_WIDTH, this.width - 20);
    }

    @Override
    public int getRowLeft() {
        return 10;
    }

    @Override
    protected int getScrollbarX() {
        return this.getRowLeft() + this.getRowWidth() + 5;
    }

//    @Override
//    protected int getScrollbarPosition() {
//        return this.getRowLeft() + this.getRowWidth() + 5;
//    }

    public abstract static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> implements Element {
    }

    public class UniversalEntry extends Entry {
        private final Object item;

        public UniversalEntry(Object item) {
            this.item = item;
        }

        @Override
        public void render(DrawContext context, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {

            int actualLeft = ConfigListWidget.this.getRowLeft();
            int actualWidth = ConfigListWidget.this.getRowWidth();

            // Verificar si el hover es válido dentro de los límites reales
            boolean validHover = hovered && mouseX >= actualLeft && mouseX <= actualLeft + actualWidth;

            if (validHover) {
                // Fondo de hover más sutil y con gradiente
                context.fill(actualLeft, top, actualLeft + actualWidth, top + height, 0x30FFFFFF);
                context.fill(actualLeft, top, actualLeft + actualWidth, top + 1, 0x60FFFFFF);
                context.fill(actualLeft, top + height - 1, actualLeft + actualWidth, top + height, 0x60FFFFFF);
            }

            // Alternar color de fondo para mejorar la legibilidad
            if (index % 2 == 0) {
                context.fill(actualLeft, top, actualLeft + actualWidth, top + height, 0x10000000);
            }

            int centerY = top + height / 2;
            int textLeft = actualLeft + PADDING;
            int statusRight = actualLeft + actualWidth - PADDING;

            if (item instanceof ConfigListScreen.EntityConfigItem) {
                renderEntityConfig(context, textLeft, top, actualWidth, height, centerY, statusRight);
            } else if (item instanceof ConfigListScreen.SoundConfigItem) {
                renderSoundConfig(context, textLeft, top, actualWidth, height, centerY, statusRight);
            } else if (item instanceof ConfigListScreen.EntityReactionItem) {
                renderEntityReaction(context, textLeft, top, actualWidth, height, centerY, statusRight);
            }
        }

        private void renderEntityConfig(DrawContext graphics, int textLeft, int top, int width, int height,
                                        int centerY, int statusRight) {
            ConfigListScreen.EntityConfigItem entityItem = (ConfigListScreen.EntityConfigItem) item;
            String entityName = entityItem.getDisplayName();

            int maxTextWidth = width - STATUS_AREA_WIDTH - PADDING * 2;
            String displayName = truncateText(entityName, maxTextWidth);

            graphics.drawText(MinecraftClient.getInstance().textRenderer, displayName, textLeft, centerY - 4, 0xFFFFFF, false);

            Text statusText = entityItem.getConfig().enabled
                    ? Text.translatable("gui.ezvcsurvival.enabled")
                    : Text.translatable("gui.ezvcsurvival.disabled");

            int statusColor = entityItem.getConfig().enabled ? 0x55FF55 : 0xFF5555;
            int statusWidth = textrender.getWidth(statusText);

            graphics.fill(statusRight - statusWidth - 6, centerY - 8, statusRight, centerY + 8,
                    entityItem.getConfig().enabled ? 0x2055FF55 : 0x20FF5555);

            graphics.drawText(textrender, statusText,
                    statusRight - statusWidth - 3, centerY - 4, statusColor, false);
        }

        private void renderSoundConfig(DrawContext graphics, int textLeft, int top, int width, int height,
                                       int centerY, int statusRight) {
            ConfigListScreen.SoundConfigItem soundItem = (ConfigListScreen.SoundConfigItem) item;
            String soundName = soundItem.getId();

            int maxTextWidth = width - STATUS_AREA_WIDTH - PADDING * 2;
            String displayName = truncateText(soundName, maxTextWidth);

            graphics.drawText(textrender, displayName, textLeft, centerY - 4, 0xFFFFFF, false);

            Text statusText = soundItem.getConfig().enabled
                    ? Text.translatable("gui.ezvcsurvival.enabled")
                    : Text.translatable("gui.ezvcsurvival.disabled");

            int statusColor = soundItem.getConfig().enabled ? 0x55FF55 : 0xFF5555;
            int statusWidth = textrender.getWidth(statusText);

            graphics.fill(statusRight - statusWidth - 6, centerY - 8, statusRight, centerY + 8,
                    soundItem.getConfig().enabled ? 0x2055FF55 : 0x20FF5555);

            graphics.drawText(textrender, statusText,
                    statusRight - statusWidth - 3, centerY - 4, statusColor, false);
        }

        private void renderEntityReaction(DrawContext graphics, int textLeft, int top, int width, int height,
                                          int centerY, int statusRight) {
            ConfigListScreen.EntityReactionItem entityItem = (ConfigListScreen.EntityReactionItem) item;
            String entityId = entityItem.getId();

            String entityName = getEntityDisplayName(entityId);

            int maxTextWidth = width - STATUS_AREA_WIDTH - PADDING * 2;
            String displayName = truncateText(entityName, maxTextWidth);

            graphics.drawText(textrender, displayName, textLeft, centerY - 4, 0xFFFFFF, false);

            Text statusText = entityItem.getReaction().enabled
                    ? Text.translatable("gui.ezvcsurvival.enabled")
                    : Text.translatable("gui.ezvcsurvival.disabled");

            int statusColor = entityItem.getReaction().enabled ? 0x55FF55 : 0xFF5555;
            int statusWidth = textrender.getWidth(statusText);

            graphics.fill(statusRight - statusWidth - 6, centerY - 8, statusRight, centerY + 8,
                    entityItem.getReaction().enabled ? 0x2055FF55 : 0x20FF5555);

            graphics.drawText(textrender, statusText,
                    statusRight - statusWidth - 3, centerY - 4, statusColor, false);
        }

        private String truncateText(String text, int maxWidth) {
            if (maxWidth <= 0) return text;

            int textWidth = textrender.getWidth(text);
            if (textWidth <= maxWidth) {
                return text;
            }

            String ellipsis = "...";
            int ellipsisWidth = textrender.getWidth(ellipsis);
            int availableWidth = maxWidth - ellipsisWidth;

            if (availableWidth <= 0) return ellipsis;

            int left = 0;
            int right = text.length();

            while (left < right) {
                int mid = (left + right + 1) / 2;
                String truncated = text.substring(0, mid);

                if (textrender.getWidth(truncated) <= availableWidth) {
                    left = mid;
                } else {
                    right = mid - 1;
                }
            }

            return text.substring(0, left) + ellipsis;
        }

        private String getEntityDisplayName(String entityId) {
            try {
                // For Fabric 1.20.1+
                Identifier identifier = Identifier.tryParse(entityId);
                if (identifier != null) {
                    // Get EntityType from registry
                    EntityType<?> type = Registries.ENTITY_TYPE.get(identifier);
                    if (type != null) {
                        String translationKey = type.getTranslationKey();
                        return Text.translatable(translationKey).getString();
                    }
                    return identifier.getPath();
                }
                return entityId;
            } catch (Exception ignored) {
                return entityId.contains(":") ? entityId.substring(entityId.indexOf(':') + 1) : entityId;
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                openEditScreen();
                return true;
            }
            return false;
        }

        public void renderTooltip(DrawContext graphics, int mouseX, int mouseY) {
            List<Text> tooltipLines = getTooltipLines();
            if (tooltipLines.isEmpty()) {
                return;
            }

            var font = MinecraftClient.getInstance().textRenderer;
            int guiW = graphics.getScaledWindowWidth();
            int guiH = graphics.getScaledWindowHeight();

            int maxLineWidth = 0;
            for (Text c : tooltipLines) {
                int w = font.getWidth(c);
                if (w > maxLineWidth) {
                    maxLineWidth = w;
                }
            }

            int lineHeight = 10;
            int padding = 8;
            int tooltipHeight = padding + tooltipLines.size() * lineHeight;

            int tooltipX = mouseX + 12;
            int tooltipY = mouseY - 12;

            if (tooltipX + maxLineWidth + padding > guiW) {
                tooltipX = Math.max(8, mouseX - 12 - maxLineWidth - padding);
            }

            if (tooltipY + tooltipHeight > guiH) {
                tooltipY = Math.max(8, guiH - tooltipHeight - 8);
            }

            if (tooltipY < 8) {
                tooltipY = mouseY + 12;
            }

            graphics.drawTooltip(font, tooltipLines, tooltipX, tooltipY);
        }

        private List<Text> getTooltipLines() {
            List<Text> tooltip = new ArrayList<>();

            if (item instanceof ConfigListScreen.EntityConfigItem entityItem) {
                var config = entityItem.getConfig();

                tooltip.add(Text.literal("§6§l" + entityItem.getDisplayName()));
                tooltip.add(Text.literal("§7ID: §f" + entityItem.getId()));
                tooltip.add(Text.literal(""));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.configuration"));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.enabled",
                        config.enabled ? Text.translatable("gui.ezvcsurvival.enabled") : Text.translatable("gui.ezvcsurvival.disabled")));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.speed", Text.literal("§e" + config.speed)));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.range", Text.literal("§e" + config.range)));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.threshold", Text.literal("§e" + config.threshold)));

            } else if (item instanceof ConfigListScreen.SoundConfigItem soundItem) {
                var config = soundItem.getConfig();

                tooltip.add(Text.literal("§6§lSound: §f" + soundItem.getId()));
                tooltip.add(Text.literal(""));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.configuration"));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.enabled",
                        config.enabled ? Text.translatable("gui.ezvcsurvival.enabled") : Text.translatable("gui.ezvcsurvival.disabled")));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.speed_multiplier", Text.literal("§e" + config.speed_multiplier)));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.range_multiplier", Text.literal("§e" + config.range_multiplier)));

            } else if (item instanceof ConfigListScreen.EntityReactionItem entityItem) {
                var reaction = entityItem.getReaction();
                String entityName = getEntityDisplayName(entityItem.getId());

                tooltip.add(Text.literal("§6§l" + entityName));
                tooltip.add(Text.literal("§7ID: §f" + entityItem.getId()));
                tooltip.add(Text.literal(""));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.sound_reaction"));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.enabled",
                        reaction.enabled ? Text.translatable("gui.ezvcsurvival.enabled") : Text.translatable("gui.ezvcsurvival.disabled")));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.speed", Text.literal("§e" + reaction.speed)));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.range", Text.literal("§e" + reaction.range)));

            }

            return tooltip;
        }

        private void openEditScreen() {
            ConfigEditScreen.EditType editType = null;
            String elementName = "";

            if (item instanceof ConfigListScreen.EntityConfigItem) {
                editType = ConfigEditScreen.EditType.ENTITY_CONFIG;
                elementName = ((ConfigListScreen.EntityConfigItem) item).getDisplayName();
            } else if (item instanceof ConfigListScreen.SoundConfigItem) {
                editType = ConfigEditScreen.EditType.GENERAL_SOUND_CONFIG;
                elementName = ((ConfigListScreen.SoundConfigItem) item).getId();
            } else if (item instanceof ConfigListScreen.EntityReactionItem) {
                editType = ConfigEditScreen.EditType.GENERAL_SOUND_ENTITY;
                elementName = getEntityDisplayName(((ConfigListScreen.EntityReactionItem) item).getId());
            }

            if (editType != null) {
                String elementId = getElementId();
                MinecraftClient.getInstance().setScreen(new ConfigEditScreen(parent, editType, elementId, elementName));
            }
        }

        private String getElementId() {
            if (item instanceof ConfigListScreen.EntityConfigItem) {
                return ((ConfigListScreen.EntityConfigItem) item).getId();
            } else if (item instanceof ConfigListScreen.SoundConfigItem) {
                return ((ConfigListScreen.SoundConfigItem) item).getId();
            } else if (item instanceof ConfigListScreen.EntityReactionItem) {
                return ((ConfigListScreen.EntityReactionItem) item).getId();
            }
            return "";
        }

        @Override
        public Text getNarration() {
            if (item instanceof ConfigListScreen.EntityConfigItem entityItem) {
                return Text.literal(entityItem.getDisplayName());
            } else if (item instanceof ConfigListScreen.SoundConfigItem soundItem) {
                return Text.literal("Sound: " + soundItem.getId());
            } else if (item instanceof ConfigListScreen.EntityReactionItem entityItem) {
                return Text.literal(getEntityDisplayName(entityItem.getId()));
            }
            return Text.literal("Unknown Item");
        }


    }
}