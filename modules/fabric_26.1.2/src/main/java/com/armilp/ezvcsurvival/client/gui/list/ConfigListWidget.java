package com.armilp.ezvcsurvival.client.gui.list;

import com.armilp.ezvcsurvival.client.gui.edit.ConfigEditScreen;
import com.armilp.ezvcsurvival.client.gui.list.interfaces.RenderableConfigItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigListWidget extends ObjectSelectionList<ConfigListWidget.Entry> {

    private final ConfigListScreen parent;
    private static final int MIN_ENTRY_WIDTH = 300;
    private static final int STATUS_AREA_WIDTH = 120;
    private static final int PADDING = 10;

    public ConfigListWidget(ConfigListScreen parent, Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
        super(mc, width, height, top, itemHeight);
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
            int rowBottom = rowTop + this.height;

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
    protected int scrollBarX() {
        return this.getRowLeft() + this.getRowWidth() + 5;
    }

    public abstract static class Entry extends ObjectSelectionList.Entry<Entry> {
    }

    public class UniversalEntry extends Entry {
        private final Object item;

        public UniversalEntry(Object item) {
            this.item = item;
        }


        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int left = ConfigListWidget.this.getRowLeft();
            int width = ConfigListWidget.this.getRowWidth();

            int top = getY();
            int height = getHeight();

            int right = left + width;
            int bottom = top + height;

            boolean validHover = hovered && isMouseOver(mouseX, mouseY);

            if (((int) a % 2) == 0) {
                graphics.fill(left, top, right, bottom, 0x10000000);
            }

            int centerY = top + height / 2;

            int fontHeight = Minecraft.getInstance().font.lineHeight;
            int boxTop = centerY - fontHeight / 2 - 2;
            int boxBottom = centerY + fontHeight / 2 + 2;

            if (validHover) {
                graphics.fill(left, boxTop, right, boxBottom, 0x30FFFFFF);
                graphics.fill(left, boxTop, right, boxTop + 1, 0x60FFFFFF);
                graphics.fill(left, boxBottom - 1, right, boxBottom, 0x60FFFFFF);
            }

            int textLeft = left + PADDING;
            int statusRight = right - PADDING;

            if (item instanceof ConfigListScreen.EntityConfigItem s) {
                renderSystem(graphics, s, textLeft, centerY, width, height, centerY, statusRight);
            } else if (item instanceof ConfigListScreen.SoundConfigItem s) {
                renderSystem(graphics, s, textLeft, centerY, width, height, centerY, statusRight);
            } else if (item instanceof ConfigListScreen.EntityReactionItem s) {
                renderSystem(graphics, s, textLeft, centerY, width, height, centerY, statusRight);
            }
        }
        public void renderSystem(GuiGraphicsExtractor graphics, RenderableConfigItem item, int textLeft, int top, int  width, int height, int centerY, int statusRight) {
            var font = Minecraft.getInstance().font;
            int maxTextWidth = width - STATUS_AREA_WIDTH - PADDING * 2;

            Component name = item.getDisplayName();
            if (item.shouldTruncate()) {
                name = Component.nullToEmpty(truncateText(name, maxTextWidth));
            }



            graphics.text(font, name, textLeft, centerY - 4, 0xFFFFFFFF, false);

            Component statusText = item.isEnabled()
                    ? Component.translatable("gui.ezvcsurvival.enabled")
                    : Component.translatable("gui.ezvcsurvival.disabled");

            int statusColor = item.isEnabled() ? 0xFF55FF55 : 0xFFFF5555;
            int statusWidth = font.width(statusText);


            graphics.fill(statusRight - statusWidth - 6, centerY - 8, statusRight, centerY + 8, item.isEnabled() ? 0x2055FF55 : 0x20FF5555);

            int statusX = Math.max(textLeft, statusRight - statusWidth - 3);

            graphics.text(font, statusText, statusX, centerY - 4, statusColor, false);
        }


        private String truncateText(Component text, int maxWidth) {
            if (maxWidth <= 0) return text.getString();

            int textWidth = Minecraft.getInstance().font.width(text);
            if (textWidth <= maxWidth) {
                return text.getString();
            }

            String ellipsis = "...";
            int ellipsisWidth = Minecraft.getInstance().font.width(ellipsis);
            int availableWidth = maxWidth - ellipsisWidth;

            if (availableWidth <= 0) return ellipsis;

            int left = 0;
            int right = text.getString().length();

            while (left < right) {
                int mid = (left + right + 1) / 2;
                String truncated = text.getString().substring(0, mid);

                if (Minecraft.getInstance().font.width(truncated) <= availableWidth) {
                    left = mid;
                } else {
                    right = mid - 1;
                }
            }

            return text.getString().substring(0, left) + ellipsis;
        }

        private String getEntityDisplayName(String entityId) {
            try {
                Identifier key = Identifier.parse(entityId);
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(key);
                if (type != null) {
                    String translationKey = type.getDescriptionId();
                    return Component.translatable(translationKey).getString();
                }
                return key.getPath();
            } catch (Exception ignored) {
                return entityId.contains(":") ? entityId.substring(entityId.indexOf(':') + 1) : entityId;
            }
        }


        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == 0) {
                openEditScreen();
                return true;
            }
            return false;
        }

        public void renderTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
            List<Component> tooltipLines = getTooltipLines();
            if (tooltipLines.isEmpty()) {
                return;
            }

            var font = Minecraft.getInstance().font;
            int guiW = graphics.guiWidth();
            int guiH = graphics.guiHeight();

            int maxLineWidth = 0;
            for (Component c : tooltipLines) {
                int w = font.width(c);
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

            graphics.setComponentTooltipForNextFrame(font, tooltipLines, tooltipX, tooltipY);
        }

        private List<Component> getTooltipLines() {
            List<Component> tooltip = new ArrayList<>();

            if (item instanceof ConfigListScreen.EntityConfigItem entityItem) {
                var config = entityItem.getConfig();

                tooltip.add(Component.literal("§6§l" + entityItem.getDisplayName()));
                tooltip.add(Component.literal("§7ID: §f" + entityItem.getId()));
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.configuration"));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.enabled",
                        config.enabled ? Component.translatable("gui.ezvcsurvival.enabled") : Component.translatable("gui.ezvcsurvival.disabled")));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.speed", Component.literal("§e" + config.speed)));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.range", Component.literal("§e" + config.range)));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.threshold", Component.literal("§e" + config.threshold)));

            } else if (item instanceof ConfigListScreen.SoundConfigItem soundItem) {
                var config = soundItem.getConfig();

                tooltip.add(Component.literal("§6§lSound: §f" + soundItem.getId()));
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.configuration"));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.enabled",
                        config.enabled ? Component.translatable("gui.ezvcsurvival.enabled") : Component.translatable("gui.ezvcsurvival.disabled")));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.speed_multiplier", Component.literal("§e" + config.speed_multiplier)));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.range_multiplier", Component.literal("§e" + config.range_multiplier)));

            } else if (item instanceof ConfigListScreen.EntityReactionItem entityItem) {
                var getReaction = entityItem.getReaction();
                String entityName = getEntityDisplayName(entityItem.getId());

                tooltip.add(Component.literal("§6§l" + entityName));
                tooltip.add(Component.literal("§7ID: §f" + entityItem.getId()));
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.sound_reaction"));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.enabled",
                        getReaction.enabled ? Component.translatable("gui.ezvcsurvival.enabled") : Component.translatable("gui.ezvcsurvival.disabled")));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.speed", Component.literal("§e" + getReaction.speed)));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.range", Component.literal("§e" + getReaction.range)));
            }

            return tooltip;
        }

        private void openEditScreen() {
            ConfigEditScreen.EditType editType = null;
            String elementName = "";

            if (item instanceof ConfigListScreen.EntityConfigItem) {
                editType = ConfigEditScreen.EditType.ENTITY_CONFIG;
                elementName = ((ConfigListScreen.EntityConfigItem) item).getDisplayName().getString();
            } else if (item instanceof ConfigListScreen.SoundConfigItem) {
                editType = ConfigEditScreen.EditType.GENERAL_SOUND_CONFIG;
                elementName = ((ConfigListScreen.SoundConfigItem) item).getId();
            } else if (item instanceof ConfigListScreen.EntityReactionItem) {
                editType = ConfigEditScreen.EditType.GENERAL_SOUND_ENTITY;
                elementName = getEntityDisplayName(((ConfigListScreen.EntityReactionItem) item).getId());
            }

            if (editType != null) {
                String elementId = getElementId();
                Minecraft.getInstance().setScreen(new ConfigEditScreen(parent, editType, elementId, elementName));
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
        public @NotNull Component getNarration() {
            if (item instanceof ConfigListScreen.EntityConfigItem entityItem) {
                return Component.literal(entityItem.getDisplayName().getString());
            } else if (item instanceof ConfigListScreen.SoundConfigItem soundItem) {
                return Component.literal("Sound: " + soundItem.getId());
            } else if (item instanceof ConfigListScreen.EntityReactionItem entityItem) {
                return Component.literal(getEntityDisplayName(entityItem.getId()));
            }
            return Component.literal("Unknown Item");
        }


    }
}