package com.armilp.ezvcsurvival.client.gui.list;

import com.armilp.ezvcsurvival.client.gui.edit.ConfigEditScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ConfigListWidget extends AlwaysSelectedEntryListWidget<ConfigListWidget.Entry> {

    private final ConfigListScreen parent;
    private static final int MIN_ENTRY_WIDTH = 300;
    private static final int STATUS_AREA_WIDTH = 120;
    private static final int PADDING = 10;

    public ConfigListWidget(ConfigListScreen parent, MinecraftClient client, int width, int height, int top, int bottom) {
        super(client, width, height, top, bottom, 20); // itemHeight = 20
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

    public abstract static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
    }

    public class UniversalEntry extends Entry {
        private final Object item;

        public UniversalEntry(Object item) {
            this.item = item;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {

            int actualLeft = ConfigListWidget.this.getRowLeft();
            int actualWidth = ConfigListWidget.this.getRowWidth();

            boolean validHover = hovered && mouseX >= actualLeft && mouseX <= actualLeft + actualWidth;

            if (validHover) {
                context.fill(actualLeft, y, actualLeft + actualWidth, y + entryHeight, 0x30FFFFFF);
                context.fill(actualLeft, y, actualLeft + actualWidth, y + 1, 0x60FFFFFF);
                context.fill(actualLeft, y + entryHeight - 1, actualLeft + actualWidth, y + entryHeight, 0x60FFFFFF);
            }

            if (index % 2 == 0) {
                context.fill(actualLeft, y, actualLeft + actualWidth, y + entryHeight, 0x10000000);
            }

            int centerY = y + entryHeight / 2;
            int textLeft = actualLeft + PADDING;
            int statusRight = actualLeft + actualWidth - PADDING;

            if (item instanceof ConfigListScreen.EntityConfigItem) {
                renderEntityConfig(context, textLeft, y, actualWidth, entryHeight, centerY, statusRight);
            } else if (item instanceof ConfigListScreen.SoundConfigItem) {
                renderSoundConfig(context, textLeft, y, actualWidth, entryHeight, centerY, statusRight);
            } else if (item instanceof ConfigListScreen.EntityReactionItem) {
                renderEntityReaction(context, textLeft, y, actualWidth, entryHeight, centerY, statusRight);
            }
        }

        private void renderEntityConfig(DrawContext context, int textLeft, int top, int width, int height,
                                        int centerY, int statusRight) {
            ConfigListScreen.EntityConfigItem entityItem = (ConfigListScreen.EntityConfigItem) item;
            String entityName = entityItem.getDisplayName();

            int maxTextWidth = width - STATUS_AREA_WIDTH - PADDING * 2;
            String displayName = truncateText(entityName, maxTextWidth);

            context.drawText(client.textRenderer, displayName, textLeft, centerY - 4, 0xFFFFFFFF, false);

            Text statusText = entityItem.getConfig().enabled
                    ? Text.translatable("gui.ezvcsurvival.enabled")
                    : Text.translatable("gui.ezvcsurvival.disabled");

            int statusColor = entityItem.getConfig().enabled ? 0xFF55FF55 : 0xFFFF5555;
            int statusWidth = client.textRenderer.getWidth(statusText);

            context.fill(statusRight - statusWidth - 6, centerY - 8, statusRight, centerY + 8,
                    entityItem.getConfig().enabled ? 0x2055FF55 : 0x20FF5555);

            context.drawText(client.textRenderer, statusText,
                    statusRight - statusWidth - 3, centerY - 4, statusColor, false);
        }

        private void renderSoundConfig(DrawContext context, int textLeft, int top, int width, int height,
                                       int centerY, int statusRight) {
            ConfigListScreen.SoundConfigItem soundItem = (ConfigListScreen.SoundConfigItem) item;
            String soundName = soundItem.getId();

            int maxTextWidth = width - STATUS_AREA_WIDTH - PADDING * 2;
            String displayName = truncateText(soundName, maxTextWidth);

            context.drawText(client.textRenderer, displayName, textLeft, centerY - 4, 0xFFFFFFFF, false);

            Text statusText = soundItem.getConfig().enabled
                    ? Text.translatable("gui.ezvcsurvival.enabled")
                    : Text.translatable("gui.ezvcsurvival.disabled");

            int statusColor = soundItem.getConfig().enabled ? 0xFF55FF55 : 0xFFFF5555;
            int statusWidth = client.textRenderer.getWidth(statusText);

            context.fill(statusRight - statusWidth - 6, centerY - 8, statusRight, centerY + 8,
                    soundItem.getConfig().enabled ? 0x2055FF55 : 0x20FF5555);

            context.drawText(client.textRenderer, statusText,
                    statusRight - statusWidth - 3, centerY - 4, statusColor, false);
        }

        private void renderEntityReaction(DrawContext context, int textLeft, int top, int width, int height,
                                          int centerY, int statusRight) {
            ConfigListScreen.EntityReactionItem entityItem = (ConfigListScreen.EntityReactionItem) item;
            String entityId = entityItem.id();

            String entityName = getEntityDisplayName(entityId);

            int maxTextWidth = width - STATUS_AREA_WIDTH - PADDING * 2;
            String displayName = truncateText(entityName, maxTextWidth);

            context.drawText(client.textRenderer, displayName, textLeft, centerY - 4, 0xFFFFFFFF, false);

            Text statusText = entityItem.reaction().enabled
                    ? Text.translatable("gui.ezvcsurvival.enabled")
                    : Text.translatable("gui.ezvcsurvival.disabled");

            int statusColor = entityItem.reaction().enabled ? 0xFF55FF55 : 0xFFFF5555;
            int statusWidth = client.textRenderer.getWidth(statusText);

            context.fill(statusRight - statusWidth - 6, centerY - 8, statusRight, centerY + 8,
                    entityItem.reaction().enabled ? 0x2055FF55 : 0x20FF5555);

            context.drawText(client.textRenderer, statusText,
                    statusRight - statusWidth - 3, centerY - 4, statusColor, false);
        }

        private String truncateText(String text, int maxWidth) {
            if (maxWidth <= 0) return text;

            int textWidth = client.textRenderer.getWidth(text);
            if (textWidth <= maxWidth) {
                return text;
            }

            String ellipsis = "...";
            int ellipsisWidth = client.textRenderer.getWidth(ellipsis);
            int availableWidth = maxWidth - ellipsisWidth;

            if (availableWidth <= 0) return ellipsis;

            int left = 0;
            int right = text.length();

            while (left < right) {
                int mid = (left + right + 1) / 2;
                String truncated = text.substring(0, mid);

                if (client.textRenderer.getWidth(truncated) <= availableWidth) {
                    left = mid;
                } else {
                    right = mid - 1;
                }
            }

            return text.substring(0, left) + ellipsis;
        }

        private String getEntityDisplayName(String entityId) {
            try {
                Identifier key = Identifier.of(entityId);
                EntityType<?> type = Registries.ENTITY_TYPE.get(key);
                if (type != null) {
                    String translationKey = type.getTranslationKey();
                    return Text.translatable(translationKey).getString();
                }
                return key.getPath();
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

        public void renderTooltip(DrawContext context, int mouseX, int mouseY) {
            List<Text> tooltipLines = getTooltipLines();
            if (tooltipLines.isEmpty()) {
                return;
            }

            var font = client.textRenderer;

            // Calculate max line width
            int maxLineWidth = 0;
            for (Text line : tooltipLines) {
                int w = font.getWidth(line);
                if (w > maxLineWidth) {
                    maxLineWidth = w;
                }
            }

            int lineHeight = 10;
            int padding = 8;
            int tooltipHeight = padding + tooltipLines.size() * lineHeight;

            int tooltipX = mouseX + 12;
            int tooltipY = mouseY - 12;

            // Adjust position if out of bounds
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();

            if (tooltipX + maxLineWidth + padding > screenWidth) {
                tooltipX = Math.max(8, mouseX - 12 - maxLineWidth - padding);
            }

            if (tooltipY + tooltipHeight > screenHeight) {
                tooltipY = Math.max(8, screenHeight - tooltipHeight - 8);
            }

            if (tooltipY < 8) {
                tooltipY = mouseY + 12;
            }

            // Render tooltip
            context.drawTooltip(font, tooltipLines, mouseX, mouseY);
        }

        private List<Text> getTooltipLines() {
            List<Text> tooltip = new ArrayList<>();

            if (item instanceof ConfigListScreen.EntityConfigItem entityItem) {
                var config = entityItem.getConfig();

                tooltip.add(Text.literal(entityItem.getDisplayName()).formatted(Formatting.GOLD, Formatting.BOLD));
                tooltip.add(Text.literal("ID: " + entityItem.getId()).formatted(Formatting.GRAY));
                tooltip.add(Text.literal(""));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.configuration"));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.enabled",
                        config.enabled ? Text.translatable("gui.ezvcsurvival.enabled").formatted(Formatting.GREEN) : Text.translatable("gui.ezvcsurvival.disabled").formatted(Formatting.RED)));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.speed", Text.literal(String.valueOf(config.speed)).formatted(Formatting.YELLOW)));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.range",
                        Text.literal(String.valueOf(config.range)).formatted(Formatting.YELLOW)));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.threshold",
                        Text.literal(String.valueOf(config.threshold)).formatted(Formatting.YELLOW)));

            } else if (item instanceof ConfigListScreen.SoundConfigItem soundItem) {
                var config = soundItem.getConfig();

                tooltip.add(Text.literal("Sound: " + soundItem.getId()).formatted(Formatting.GOLD, Formatting.BOLD));
                tooltip.add(Text.literal(""));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.configuration"));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.enabled",
                        config.enabled ?
                                Text.translatable("gui.ezvcsurvival.enabled").formatted(Formatting.GREEN) :
                                Text.translatable("gui.ezvcsurvival.disabled").formatted(Formatting.RED)));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.speed_multiplier",
                        Text.literal(String.valueOf(config.speed_multiplier)).formatted(Formatting.YELLOW)));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.range_multiplier",
                        Text.literal(String.valueOf(config.range_multiplier)).formatted(Formatting.YELLOW)));

            } else if (item instanceof ConfigListScreen.EntityReactionItem) {
                ConfigListScreen.EntityReactionItem reactionItem = (ConfigListScreen.EntityReactionItem) item;
                var reaction = reactionItem.reaction();
                String entityName = getEntityDisplayName(reactionItem.id());

                tooltip.add(Text.literal(entityName).formatted(Formatting.GOLD, Formatting.BOLD));
                tooltip.add(Text.literal("ID: " + reactionItem.id()).formatted(Formatting.GRAY));
                tooltip.add(Text.literal(""));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.sound_reaction"));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.enabled",
                        reaction.enabled ?
                                Text.translatable("gui.ezvcsurvival.enabled").formatted(Formatting.GREEN) :
                                Text.translatable("gui.ezvcsurvival.disabled").formatted(Formatting.RED)));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.speed",
                        Text.literal(String.valueOf(reaction.speed)).formatted(Formatting.YELLOW)));
                tooltip.add(Text.translatable("tooltip.ezvcsurvival.range",
                        Text.literal(String.valueOf(reaction.range)).formatted(Formatting.YELLOW)));
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
                elementName = getEntityDisplayName(((ConfigListScreen.EntityReactionItem) item).id());
            }

            if (editType != null) {
                String elementId = getElementId();
                client.setScreen(new ConfigEditScreen(parent, editType, elementId, elementName));
            }
        }

        private String getElementId() {
            if (item instanceof ConfigListScreen.EntityConfigItem) {
                return ((ConfigListScreen.EntityConfigItem) item).getId();
            } else if (item instanceof ConfigListScreen.SoundConfigItem) {
                return ((ConfigListScreen.SoundConfigItem) item).getId();
            } else if (item instanceof ConfigListScreen.EntityReactionItem) {
                return ((ConfigListScreen.EntityReactionItem) item).id();
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
                return Text.literal(getEntityDisplayName(entityItem.id()));
            }
            return Text.literal("Unknown Item");
        }

        @Override
        public void appendNarrations(NarrationMessageBuilder builder) {
            builder.put(NarrationPart.TITLE, getNarration());
        }
    }
}