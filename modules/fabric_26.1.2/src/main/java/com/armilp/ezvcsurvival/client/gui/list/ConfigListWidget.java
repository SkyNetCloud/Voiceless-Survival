package com.armilp.ezvcsurvival.client.gui.list;

import com.armilp.ezvcsurvival.client.gui.edit.ConfigEditScreen;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.GunfireConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class ConfigListWidget extends ObjectSelectionList<ConfigListWidget.Entry> {

    private final ConfigListScreen parent;

    private static final int MIN_ENTRY_WIDTH = 300;
    private static final int STATUS_AREA_WIDTH = 120;
    private static final int PADDING = 10;

    public ConfigListWidget(ConfigListScreen parent, Minecraft client, int width, int height, int top, int bottom) {
        super(client, width, height, top, bottom);
        this.parent = parent;
    }

    public void addItem(Object item) {
        this.addEntry(new UniversalEntry(item));
    }

    public void clear() {
        this.clearEntries();
    }

    public int getEntryIndexAt(double mouseX, double mouseY) {
        if (!this.isMouseOver(mouseX, mouseY)) return -1;

        int rowLeft = this.getRowLeft();
        int rowRight = rowLeft + this.getRowWidth();
        if (mouseX < rowLeft || mouseX > rowRight) return -1;

        List<Entry> entries = this.children();
        for (int i = 0; i < entries.size(); i++) {
            int rowTop = this.getRowTop(i);
            int rowBottom = rowTop + entries.get(i).getHeight();
            if (mouseY >= rowTop && mouseY < rowBottom) {
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
    protected int scrollBarX() {return this.getRowRight() + 5;}

    public abstract static class Entry extends ObjectSelectionList.Entry<Entry> {}

    public class UniversalEntry extends Entry {

        private final Object item;

        public UniversalEntry(Object item) {
            this.item = item;
        }

 
        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int y = this.getY();
            int entryHeight = this.getHeight();
            int index = ConfigListWidget.this.children().indexOf(this);

            int actualLeft = ConfigListWidget.this.getRowLeft();
            int actualWidth = ConfigListWidget.this.getRowWidth();

            boolean validHover = hovered && mouseX >= actualLeft && mouseX <= actualLeft + actualWidth;

            if (validHover) {
                graphics.fill(actualLeft, y, actualLeft + actualWidth, y + entryHeight, 0x30FFFFFF);
                graphics.fill(actualLeft, y, actualLeft + actualWidth, y + 1, 0x60FFFFFF);
                graphics.fill(actualLeft, y + entryHeight - 1, actualLeft + actualWidth, y + entryHeight, 0x60FFFFFF);
            }

            if (index % 2 == 0) {
                graphics.fill(actualLeft, y, actualLeft + actualWidth, y + entryHeight, 0x10000000);
            }

            int centerY = y + entryHeight / 2;
            int textLeft = actualLeft + PADDING;
            int statusRight = actualLeft + actualWidth - PADDING;

            if (item instanceof ConfigListScreen.EntityConfigItem entity) {
                renderEntityConfig(graphics, entity, textLeft, actualWidth, centerY, statusRight);
            } else if (item instanceof ConfigListScreen.SoundConfigItem sound) {
                renderSoundConfig(graphics, sound, textLeft, actualWidth, centerY, statusRight);
            } else if (item instanceof ConfigListScreen.EntityReactionItem reaction) {
                renderEntityReaction(graphics, reaction, textLeft, actualWidth, centerY, statusRight);
            } else if (item instanceof ConfigListScreen.GunfireEntityItem gun) {
                renderGunfireEntity(graphics, gun, textLeft, centerY, statusRight);
            }
        }

        private void renderEntityConfig(GuiGraphicsExtractor context, ConfigListScreen.EntityConfigItem entity,
                                        int textLeft, int width, int centerY, int statusRight) {
            EntityVoiceConfig.EntityConfig cfg = entity.getConfig();
            boolean enabled = cfg != null && cfg.enabled;
            int nameColor = enabled ? 0xFFFFFFFF : 0xFF777777;
            int maxTextWidth = width - STATUS_AREA_WIDTH - PADDING * 2;

            context.text(minecraft.font, entity.getDisplayName(),
                    textLeft, centerY - 4, nameColor, false);

            renderStatusBadge(context, enabled, centerY, statusRight);
        }

        private void renderSoundConfig(GuiGraphicsExtractor context, ConfigListScreen.SoundConfigItem sound,
                                       int textLeft, int width, int centerY, int statusRight) {
            GeneralSoundsConfig.SoundEntry cfg = sound.getConfig();
            boolean enabled = cfg != null && cfg.enabled;
            int nameColor = enabled ? 0xFFFFFFFF : 0xFF777777;
            int maxTextWidth = width - STATUS_AREA_WIDTH - PADDING * 2;

            context.text(minecraft.font,
                    truncateText(cleanElementId(sound.id()), maxTextWidth),
                    textLeft, centerY - 4, nameColor, false);

            renderStatusBadge(context, enabled, centerY, statusRight);
        }

        private void renderEntityReaction(GuiGraphicsExtractor context, ConfigListScreen.EntityReactionItem reaction,
                                          int textLeft, int width, int centerY, int statusRight) {
            GeneralSoundsConfig.Reaction r = reaction.getReaction();
            boolean enabled = r != null && r.enabled;
            int nameColor = enabled ? 0xFFFFFFFF : 0xFF777777;

            context.text(minecraft.font,
                    getEntityDisplayName(cleanElementId(reaction.id())),
                    textLeft, centerY - 4, nameColor, false);

            renderStatusBadge(context, enabled, centerY, statusRight);
        }

        private void renderGunfireEntity(GuiGraphicsExtractor context, ConfigListScreen.GunfireEntityItem gun,
                                         int textLeft, int centerY, int statusRight) {
            GunfireConfig.Reaction r = gun.getReaction();
            boolean enabled = r != null && r.enabled;
            int nameColor = enabled ? 0xFFFFFFFF : 0xFF777777;

            context.text(minecraft.font,
                    getEntityDisplayName(cleanElementId(gun.id())),
                    textLeft, centerY - 4, nameColor, false);

            renderStatusBadge(context, enabled, centerY, statusRight);
        }

        private void renderStatusBadge(GuiGraphicsExtractor context, boolean enabled, int centerY, int statusRight) {
            Component statusText = enabled
                    ? Component.translatable("gui.ezvcsurvival.enabled")
                    : Component.translatable("gui.ezvcsurvival.disabled");
            int statusColor = enabled ? 0xFF55FF55 : 0xFFFF5555;
            int bgColor = enabled ? 0x2055FF55 : 0x20FF5555;
            int statusWidth = minecraft.font.width(statusText);

            context.fill(statusRight - statusWidth - 6, centerY - 8,
                    statusRight, centerY + 8, bgColor);
            context.text(minecraft.font, statusText,
                    statusRight - statusWidth - 3, centerY - 4, statusColor, false);
        }

        private String truncateText(String text, int maxWidth) {
            if (minecraft.font.width(text) <= maxWidth) return text;
            String ellipsis = "...";
            int available = maxWidth - minecraft.font.width(ellipsis);
            for (int i = text.length(); i > 0; i--) {
                String sub = text.substring(0, i);
                if (minecraft.font.width(sub) <= available) return sub + ellipsis;
            }
            return ellipsis;
        }

        private String getEntityDisplayName(String id) {
            Identifier identifier = Identifier.tryParse(id);
            if (identifier == null) return id;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(identifier);
            if (type == null) return identifier.getPath();
            return Component.translatable(type.getDescriptionId()).getString();
        }

        public void renderTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
            List<String> tooltipLines = getTooltipLines();
            if (tooltipLines.isEmpty()) return;

            Font font = minecraft.font;
            int maxLineWidth = tooltipLines.stream().mapToInt(font::width).max().orElse(0);

            int lineHeight = 10;
            int padding = 8;
            int tooltipHeight = padding + tooltipLines.size() * lineHeight;

            int screenW = minecraft.getWindow().getGuiScaledWidth();
            int screenH = minecraft.getWindow().getGuiScaledHeight();

            int tooltipX = mouseX + 12;
            int tooltipY = mouseY - 12;

            // right edge: flip to the left of the cursor
            if (tooltipX + maxLineWidth + padding > screenW) {
                tooltipX = Math.max(8, mouseX - 12 - maxLineWidth - padding);
            }
            // bottom edge: slide up just enough to fit, so it keeps tracking the mouse
            if (tooltipY + tooltipHeight + 5 > screenH) {
                tooltipY = screenH - tooltipHeight - 5;
            }
            // top edge
            if (tooltipY < 8) tooltipY = 8;

            // Background
            context.fill(tooltipX - 3, tooltipY - 4,
                    tooltipX + maxLineWidth + padding, tooltipY + tooltipHeight, 0xF0100010);
            // Border
            context.fill(tooltipX - 4, tooltipY - 5,
                    tooltipX + maxLineWidth + padding + 1, tooltipY - 4, 0x505000FF);
            context.fill(tooltipX - 4, tooltipY + tooltipHeight,
                    tooltipX + maxLineWidth + padding + 1, tooltipY + tooltipHeight + 1, 0x505000FF);
            context.fill(tooltipX - 4, tooltipY - 4,
                    tooltipX - 3, tooltipY + tooltipHeight, 0x505000FF);
            context.fill(tooltipX + maxLineWidth + padding, tooltipY - 4,
                    tooltipX + maxLineWidth + padding + 1, tooltipY + tooltipHeight, 0x505000FF);

            for (int i = 0; i < tooltipLines.size(); i++) {
                context.text(font, tooltipLines.get(i),
                        tooltipX, tooltipY + i * lineHeight, 0xFFFFFFFF, false);
            }
        }

        private List<String> getTooltipLines() {
            List<String> tooltip = new ArrayList<>();

            if (item instanceof ConfigListScreen.EntityConfigItem entityItem) {
                EntityVoiceConfig.EntityConfig config = entityItem.getConfig();
                if (config == null) return tooltip;

                tooltip.add("§6§l" + entityItem.getDisplayName());
                tooltip.add("§7ID: §f" + entityItem.id());
                tooltip.add("");
                tooltip.add(I18n.get("tooltip.ezvcsurvival.configuration"));
                tooltip.add(I18n.get("tooltip.ezvcsurvival.enabled",
                        config.enabled ? I18n.get("gui.ezvcsurvival.enabled") : I18n.get("gui.ezvcsurvival.disabled")));
                tooltip.add(I18n.get("tooltip.ezvcsurvival.speed", "§e" + config.speed));
                tooltip.add(I18n.get("tooltip.ezvcsurvival.range", "§e" + config.range));
                tooltip.add(I18n.get("tooltip.ezvcsurvival.threshold", "§e" + config.threshold));

            } else if (item instanceof ConfigListScreen.SoundConfigItem soundItem) {
                GeneralSoundsConfig.SoundEntry config = soundItem.getConfig();
                if (config == null) return tooltip;

                tooltip.add("§6§lSound: §f" + soundItem.id());
                tooltip.add("");
                tooltip.add(I18n.get("tooltip.ezvcsurvival.configuration"));
                tooltip.add(I18n.get("tooltip.ezvcsurvival.enabled",
                        config.enabled ? I18n.get("gui.ezvcsurvival.enabled") : I18n.get("gui.ezvcsurvival.disabled")));
                tooltip.add(I18n.get("tooltip.ezvcsurvival.speed_multiplier", "§e" + config.speed_multiplier));
                tooltip.add(I18n.get("tooltip.ezvcsurvival.range_multiplier", "§e" + config.range_multiplier));

            } else if (item instanceof ConfigListScreen.EntityReactionItem entityItem) {
                GeneralSoundsConfig.Reaction reaction = entityItem.getReaction();
                if (reaction == null) return tooltip;
                String entityName = getEntityDisplayName(entityItem.id());

                tooltip.add("§6§l" + entityName);
                tooltip.add("§7ID: §f" + entityItem.id());
                tooltip.add("");
                tooltip.add(I18n.get("tooltip.ezvcsurvival.sound_reaction"));
                tooltip.add(I18n.get("tooltip.ezvcsurvival.enabled",
                        reaction.enabled ? I18n.get("gui.ezvcsurvival.enabled") : I18n.get("gui.ezvcsurvival.disabled")));
                tooltip.add(I18n.get("tooltip.ezvcsurvival.speed", "§e" + reaction.speed));
                tooltip.add(I18n.get("tooltip.ezvcsurvival.range", "§e" + reaction.range));

            } else if (item instanceof ConfigListScreen.GunfireEntityItem gunItem) {
                GunfireConfig.Reaction reaction = gunItem.getReaction();
                if (reaction == null) return tooltip;

                tooltip.add("§6§l" + getEntityDisplayName(gunItem.id()));
                tooltip.add("§7ID: §f" + gunItem.id());
                tooltip.add("");
                tooltip.add(I18n.get("tooltip.ezvcsurvival.gunfire_reaction"));
                tooltip.add(I18n.get("tooltip.ezvcsurvival.enabled",
                        reaction.enabled ? I18n.get("gui.ezvcsurvival.enabled") : I18n.get("gui.ezvcsurvival.disabled")));
                tooltip.add(I18n.get("tooltip.ezvcsurvival.speed", "§e" + reaction.speed));
                tooltip.add(I18n.get("tooltip.ezvcsurvival.range", "§e" + reaction.range));
            }

            return tooltip;
        }


        @Override
        public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
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
                elementName = entity.getDisplayName();
            } else if (item instanceof ConfigListScreen.SoundConfigItem sound) {
                editType = ConfigEditScreen.EditType.GENERAL_SOUND_CONFIG;
                elementName = sound.id();
            } else if (item instanceof ConfigListScreen.EntityReactionItem reaction) {
                editType = ConfigEditScreen.EditType.GENERAL_SOUND_ENTITY;
                elementName = getEntityDisplayName(reaction.id());
            } else if (item instanceof ConfigListScreen.GunfireEntityItem gun) {
                editType = ConfigEditScreen.EditType.GUNFIRE_ENTITY;
                elementName = getEntityDisplayName(gun.id());
            }

            if (editType != null) {
                minecraft.setScreen(new ConfigEditScreen(parent, editType, getElementId(), elementName));
            }
        }

        private String getElementId() {
            if (item instanceof ConfigListScreen.EntityConfigItem e) return e.id();
            if (item instanceof ConfigListScreen.SoundConfigItem s) return s.id();
            if (item instanceof ConfigListScreen.EntityReactionItem r) return r.id();
            if (item instanceof ConfigListScreen.GunfireEntityItem g) return g.id();
            return "";
        }

        public static String cleanElementId(String rawId) {
            if (rawId == null) return "";
            if (rawId.contains("Optional[ResourceKey[")) {
                int slashIndex = rawId.indexOf('/');
                if (slashIndex > 0) {
                    String idPart = rawId.substring(slashIndex + 1);
                    int bracketIndex = idPart.indexOf(']');
                    if (bracketIndex > 0) return idPart.substring(0, bracketIndex).trim();
                }
            }
            return rawId;
        }

        @Override
        public Component getNarration() {
            return Component.literal("Config Entry");
        }


        @Override
        public void updateNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, getNarration());
        }

 
    }
}