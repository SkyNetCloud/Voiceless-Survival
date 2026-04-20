package com.armilp.ezvcsurvival.client.gui.list;

import com.armilp.ezvcsurvival.client.gui.edit.ConfigEditScreen;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.resource.language.I18n;
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

    public ConfigListWidget(ConfigListScreen parent, MinecraftClient client, int width, int height, int top, int bottom) {
        super(client, width, height, top, bottom); // FIXED
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

    public abstract static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {}

    public class UniversalEntry extends Entry {

        private final Object item;

        public UniversalEntry(Object item) {
            this.item = item;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float delta) {

            int y = this.getY();
            int entryHeight = this.getHeight();
            int index = ConfigListWidget.this.children().indexOf(this);

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

            if (item instanceof ConfigListScreen.EntityConfigItem entity) {
                renderEntityConfig(context, entity, textLeft, actualWidth, centerY, statusRight);
            }
            else if (item instanceof ConfigListScreen.SoundConfigItem sound) {
                renderSoundConfig(context, sound, textLeft, actualWidth, centerY, statusRight);
            }
            else if (item instanceof ConfigListScreen.EntityReactionItem reaction) {
                renderEntityReaction(context, reaction, textLeft, actualWidth, centerY, statusRight);
            }
        }

        private void renderEntityConfig(DrawContext context, ConfigListScreen.EntityConfigItem entity,
                                        int textLeft, int width, int centerY, int statusRight) {

            boolean enabled = entity.getConfig().enabled && EntityVoiceConfig.isEnabled();

            int nameColor = enabled ? 0xFFFFFFFF : 0xFF777777;

            String entityName = entity.getDisplayName().getString();
            int maxTextWidth = width - STATUS_AREA_WIDTH - PADDING * 2;

            context.drawText(client.textRenderer,
                    truncateText(entityName, maxTextWidth),
                    textLeft,
                    centerY - 4,
                    nameColor,
                    false);

            Text statusText = enabled
                    ? Text.translatable("gui.ezvcsurvival.enabled")
                    : Text.translatable("gui.ezvcsurvival.disabled");

            int statusColor = enabled ? 0xFF55FF55 : 0xFFFF5555;
            int statusWidth = client.textRenderer.getWidth(statusText);

            context.fill(statusRight - statusWidth - 6, centerY - 8, statusRight, centerY + 8,
                    enabled ? 0x2055FF55 : 0x20FF5555);

            context.drawText(client.textRenderer,
                    statusText,
                    statusRight - statusWidth - 3,
                    centerY - 4,
                    statusColor,
                    false);
        }

        private void renderSoundConfig(DrawContext context, ConfigListScreen.SoundConfigItem sound,
                                       int textLeft, int width, int centerY, int statusRight) {

            boolean enabled = sound.getConfig().enabled && GeneralSoundsConfig.isEnabled();

            int nameColor = enabled ? 0xFFFFFFFF : 0xFF777777;

            String name = cleanElementId(sound.getId());

            int maxTextWidth = width - STATUS_AREA_WIDTH - PADDING * 2;

            context.drawText(client.textRenderer,
                    truncateText(name, maxTextWidth),
                    textLeft,
                    centerY - 4,
                    nameColor,
                    false);

            Text statusText = enabled
                    ? Text.translatable("gui.ezvcsurvival.enabled")
                    : Text.translatable("gui.ezvcsurvival.disabled");

            int statusColor = enabled ? 0xFF55FF55 : 0xFFFF5555;
            int statusWidth = client.textRenderer.getWidth(statusText);

            context.fill(statusRight - statusWidth - 6, centerY - 8, statusRight, centerY + 8,
                    enabled ? 0x2055FF55 : 0x20FF5555);

            context.drawText(client.textRenderer,
                    statusText,
                    statusRight - statusWidth - 3,
                    centerY - 4,
                    statusColor,
                    false);
        }

        private void renderEntityReaction(DrawContext context, ConfigListScreen.EntityReactionItem reaction,
                                          int textLeft, int width, int centerY, int statusRight) {

            boolean enabled = reaction.reaction().enabled && GeneralSoundsConfig.isEnabled();

            int nameColor = enabled ? 0xFFFFFFFF : 0xFF777777;

            String entityName = getEntityDisplayName(cleanElementId(reaction.id()));

            context.drawText(client.textRenderer,
                    entityName,
                    textLeft,
                    centerY - 4,
                    nameColor,
                    false);

            Text statusText = enabled
                    ? Text.translatable("gui.ezvcsurvival.enabled")
                    : Text.translatable("gui.ezvcsurvival.disabled");

            int statusColor = enabled ? 0xFF55FF55 : 0xFFFF5555;
            int statusWidth = client.textRenderer.getWidth(statusText);

            context.fill(statusRight - statusWidth - 6, centerY - 8, statusRight, centerY + 8,
                    enabled ? 0x2055FF55 : 0x20FF5555);

            context.drawText(client.textRenderer,
                    statusText,
                    statusRight - statusWidth - 3,
                    centerY - 4,
                    statusColor,
                    false);
        }

        private String truncateText(String text, int maxWidth) {
            if (client.textRenderer.getWidth(text) <= maxWidth) {
                return text;
            }

            String ellipsis = "...";
            int ellipsisWidth = client.textRenderer.getWidth(ellipsis);
            int available = maxWidth - ellipsisWidth;

            for (int i = text.length(); i > 0; i--) {
                String sub = text.substring(0, i);
                if (client.textRenderer.getWidth(sub) <= available) {
                    return sub + ellipsis;
                }
            }

            return ellipsis;
        }

        private String getEntityDisplayName(String id) {
            Identifier identifier = Identifier.tryParse(id);
            if (identifier == null) return id;

            EntityType<?> type = Registries.ENTITY_TYPE.get(identifier);
            if (type == null) return identifier.getPath();

            return Text.translatable(type.getTranslationKey()).getString();
        }

        public void renderTooltip(DrawContext context, int mouseX, int mouseY) {
            List<String> tooltipLines = getTooltipLines();
            if (tooltipLines.isEmpty()) {
                return;
            }

            TextRenderer font = MinecraftClient.getInstance().textRenderer;

            int maxLineWidth = 0;
            for (String line : tooltipLines) {
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

            if (tooltipX + maxLineWidth + padding > ConfigListWidget.this.width) {
                tooltipX = Math.max(8, mouseX - 12 - maxLineWidth - padding);
            }

            if (tooltipY + tooltipHeight > ConfigListWidget.this.height) {
                tooltipY = Math.max(8, ConfigListWidget.this.height - tooltipHeight - 8);
            }

            if (tooltipY < 8) {
                tooltipY = mouseY + 12;
            }


            context.fill(tooltipX - 3, tooltipY - 4, tooltipX + maxLineWidth + padding, tooltipY + tooltipHeight, 0xF0100010);
            context.fill(tooltipX - 4, tooltipY - 5, tooltipX + maxLineWidth + padding + 1, tooltipY - 4, 0x505000FF);
            context.fill(tooltipX - 4, tooltipY + tooltipHeight, tooltipX + maxLineWidth + padding + 1, tooltipY + tooltipHeight + 1, 0x505000FF);
            context.fill(tooltipX - 4, tooltipY - 4, tooltipX - 3, tooltipY + tooltipHeight, 0x505000FF);
            context.fill(tooltipX + maxLineWidth + padding, tooltipY - 4, tooltipX + maxLineWidth + padding + 1, tooltipY + tooltipHeight, 0x505000FF);

            for (int i = 0; i < tooltipLines.size(); i++) {
                context.drawText(
                        font,
                        tooltipLines.get(i),
                        tooltipX,
                        tooltipY + i * lineHeight,
                        0xFFFFFF,
                        false
                );
            }
        }

        private List<String> getTooltipLines() {
            List<String> tooltip = new ArrayList<String>();

            if (item instanceof ConfigListScreen.EntityConfigItem) {
                ConfigListScreen.EntityConfigItem entityItem = (ConfigListScreen.EntityConfigItem) item;
                EntityVoiceConfig.EntityConfig config = entityItem.getConfig();

                tooltip.add("§6§l" + entityItem.getDisplayName());
                tooltip.add("§7ID: §f" + entityItem.getId());
                tooltip.add("");
                tooltip.add(I18n.translate("tooltip.ezvcsurvival.configuration"));
                tooltip.add(I18n.translate("tooltip.ezvcsurvival.enabled",
                        config.enabled ? I18n.translate("gui.ezvcsurvival.enabled") : I18n.translate("gui.ezvcsurvival.disabled")));
                tooltip.add(I18n.translate("tooltip.ezvcsurvival.speed", "§e" + config.speed));
                tooltip.add(I18n.translate("tooltip.ezvcsurvival.range", "§e" + config.range));
                tooltip.add(I18n.translate("tooltip.ezvcsurvival.threshold", "§e" + config.threshold));

            } else if (item instanceof ConfigListScreen.SoundConfigItem) {
                ConfigListScreen.SoundConfigItem soundItem = (ConfigListScreen.SoundConfigItem) item;
                GeneralSoundsConfig.SoundEntry config = soundItem.getConfig();

                tooltip.add("§6§lSound: §f" + soundItem.getId());
                tooltip.add("");
                tooltip.add(I18n.translate("tooltip.ezvcsurvival.configuration"));
                tooltip.add(I18n.translate("tooltip.ezvcsurvival.enabled",
                        config.enabled ? I18n.translate("gui.ezvcsurvival.enabled") : I18n.translate("gui.ezvcsurvival.disabled")));
                tooltip.add(I18n.translate("tooltip.ezvcsurvival.speed_multiplier", "§e" + config.speed_multiplier));
                tooltip.add(I18n.translate("tooltip.ezvcsurvival.range_multiplier", "§e" + config.range_multiplier));

            } else if (item instanceof ConfigListScreen.EntityReactionItem) {
                ConfigListScreen.EntityReactionItem entityItem = (ConfigListScreen.EntityReactionItem) item;
                GeneralSoundsConfig.Reaction reaction = entityItem.reaction();
                String entityName = getEntityDisplayName(entityItem.id());

                tooltip.add("§6§l" + entityName);
                tooltip.add("§7ID: §f" + entityItem.id());
                tooltip.add("");
                tooltip.add(I18n.translate("tooltip.ezvcsurvival.sound_reaction"));
                tooltip.add(I18n.translate("tooltip.ezvcsurvival.enabled",
                        reaction.enabled ? I18n.translate("gui.ezvcsurvival.enabled") : I18n.translate("gui.ezvcsurvival.disabled")));
                tooltip.add(I18n.translate("tooltip.ezvcsurvival.speed", "§e" + reaction.speed));
                tooltip.add(I18n.translate("tooltip.ezvcsurvival.range", "§e" + reaction.range));
            }

            return tooltip;
        }

        @Override
        public boolean mouseClicked(Click click, boolean doubled) {
            if (click.button() == 0) {
                openEditScreen();
                return true;
            }
            return false;
        }

        private void openEditScreen() {

            ConfigEditScreen.EditType editType = null;
            String elementName = "";

            if (item instanceof ConfigListScreen.EntityConfigItem entity) {
                editType = ConfigEditScreen.EditType.ENTITY_CONFIG;
                elementName = entity.getDisplayName().getString();
            }
            else if (item instanceof ConfigListScreen.SoundConfigItem sound) {
                editType = ConfigEditScreen.EditType.GENERAL_SOUND_CONFIG;
                elementName = sound.getId();
            }
            else if (item instanceof ConfigListScreen.EntityReactionItem reaction) {
                editType = ConfigEditScreen.EditType.GENERAL_SOUND_ENTITY;
                elementName = getEntityDisplayName(reaction.id());
            }

            if (editType != null) {
                client.setScreen(new ConfigEditScreen(parent, editType, getElementId(), elementName));
            }
        }

        private String getElementId() {
            if (item instanceof ConfigListScreen.EntityConfigItem e) return e.getId();
            if (item instanceof ConfigListScreen.SoundConfigItem s) return s.getId();
            if (item instanceof ConfigListScreen.EntityReactionItem r) return r.id();
            return "";
        }

        public static String cleanElementId(String rawId) {
            if (rawId == null) return "";

            if (rawId.contains("Optional[ResourceKey[")) {
                int slashIndex = rawId.indexOf('/');
                if (slashIndex > 0) {
                    String idPart = rawId.substring(slashIndex + 1);
                    int bracketIndex = idPart.indexOf(']');
                    if (bracketIndex > 0) {
                        return idPart.substring(0, bracketIndex).trim();
                    }
                }
            }

            return rawId;
        }

        @Override
        public Text getNarration() {
            return Text.literal("Config Entry");
        }

        @Override
        public void appendNarrations(NarrationMessageBuilder builder) {
            builder.put(NarrationPart.TITLE, getNarration());
        }
    }
}