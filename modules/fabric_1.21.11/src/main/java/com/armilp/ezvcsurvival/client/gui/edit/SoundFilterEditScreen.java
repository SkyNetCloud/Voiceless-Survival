package com.armilp.ezvcsurvival.client.gui.edit;


import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SoundFilterEditScreen extends Screen {
    private final Screen parent;
    private final String entityId;
    private final String entityName;

    private TextFieldWidget searchBox;
    private ButtonWidget saveButton;
    private ButtonWidget backButton;
    private ButtonWidget clearButton;

    private List<String> blockedSounds;
    private List<SuggestionEntry> suggestions;
    private int scrollOffset = 0;
    private boolean isDraggingScrollbar = false;
    private int dragStartY = 0;
    private int dragStartOffset = 0;
    private boolean showOnlyBlocked = false;
    private ButtonWidget toggleFilterButton;

    private static final int MAX_VISIBLE_SUGGESTIONS = 15;
    private static final int FIELD_WIDTH = 500;
    private static final int FIELD_HEIGHT = 20;
    private static final int SUGGESTION_HEIGHT = 20;
    private static final int SCROLLBAR_WIDTH = 10;


    public SoundFilterEditScreen(Screen parent, String entityId, String entityName) {
        super(Text.literal("Sound Filters: " + SoundFilterEditScreen.getEntityDisplayName(entityId)));
        this.parent = parent;
        this.entityId = entityId;
        this.entityName = entityName;
        loadCurrentFilters();
        loadSuggestions();
    }


    public static String getEntityDisplayName(String entityId) {
        try {
            Identifier identifier = Identifier.tryParse(entityId);
            if (identifier == null) {
                return entityId;
            }

            EntityType<?> type = Registries.ENTITY_TYPE.get(identifier);
            if (type == null) {
                return identifier.getPath();
            }

            String translationKey = type.getTranslationKey();
            return translationKey != null ? Text.translatable(translationKey).getString() : identifier.getPath() ;

        } catch (Exception e) {
            return entityId.contains(":") ? entityId.substring(entityId.indexOf(':') + 1) : entityId;
        }
    }


    private void loadCurrentFilters() {
        GeneralSoundsConfig.Reaction reaction = GeneralSoundsConfig.getMobReactions().get(entityId);
        if (reaction != null && reaction.blocked_sounds != null) {
            this.blockedSounds = new ArrayList<>(reaction.blocked_sounds);
        } else {
            this.blockedSounds = new ArrayList<>();
        }
    }


    private void loadSuggestions() {
        suggestions = new ArrayList<>();

        Registries.SOUND_EVENT.forEach(sound -> {
            Identifier soundId = Registries.SOUND_EVENT.getId(sound);
            if (soundId != null) {
                String soundIdString = soundId.toString();
                String category = categorizeSound(soundIdString);
                suggestions.add(new SuggestionEntry(soundIdString, category));
            }
        });

        suggestions.sort((a, b) -> {
            int catCompare = a.category.compareTo(b.category);
            return catCompare != 0 ? catCompare : a.soundId.compareTo(b.soundId);
        });
    }

    private String categorizeSound(String soundId) {
        String lower = soundId.toLowerCase();

        if (lower.contains("explosion") || lower.contains("explode")) return "Explosions";
        if (lower.contains("gun") || lower.contains("shoot") || lower.contains("fire")) return "Gunfire";
        if (lower.contains("place") || lower.contains("break") || lower.contains("hit")) return "Blocks";
        if (lower.contains("step") || lower.contains("walk")) return "Movement";
        if (lower.contains("ambient") || lower.contains("idle")) return "Ambient";
        if (lower.contains("hurt") || lower.contains("death")) return "Entity Sounds";
        if (lower.contains("music")) return "Music";
        if (lower.contains("weather") || lower.contains("rain") || lower.contains("thunder")) return "Weather";

        if (soundId.startsWith("minecraft:")) return "Minecraft";
        if (soundId.contains(":")) return soundId.split(":")[0].toUpperCase();

        return "Other";
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = 40;

        searchBox = new TextFieldWidget(this.textRenderer, centerX - FIELD_WIDTH / 2, startY, FIELD_WIDTH - 110, FIELD_HEIGHT,
                Text.literal("Search Sounds"));
        searchBox.setPlaceholder(Text.literal("Search sounds...").withColor(0x888888));
        searchBox.setChangedListener(this::onSearchChanged);
        searchBox.setMaxLength(100);
        this.addDrawableChild(searchBox);

        toggleFilterButton = ButtonWidget.builder(Text.literal("Show Blocked"), b -> toggleShowBlocked())
                .dimensions(centerX + FIELD_WIDTH / 2 - 100, startY, 100, FIELD_HEIGHT).build();
        this.addDrawableChild(toggleFilterButton);

        int buttonY = this.height - 30;

        clearButton = ButtonWidget.builder(Text.literal("Clear All"), b -> clearFilters())
                .dimensions(centerX - 210, buttonY, 100, 20).build();
        this.addDrawableChild(clearButton);

        saveButton = ButtonWidget.builder(Text.literal("Save"), b -> saveFilters())
                .dimensions(centerX - 100, buttonY, 100, 20).build();
        this.addDrawableChild(saveButton);

        backButton = ButtonWidget.builder(Text.literal("Cancel"), b -> MinecraftClient.getInstance().setScreen(parent))
                .dimensions(centerX + 10, buttonY, 100, 20).build();
        this.addDrawableChild(backButton);
    }

    private void onSearchChanged(String query) {
        scrollOffset = 0;
    }

    private void toggleShowBlocked() {
        showOnlyBlocked = !showOnlyBlocked;
        scrollOffset = 0;
        toggleFilterButton.setMessage(Text.literal(showOnlyBlocked ? "Show All" : "Show Blocked"));
    }

    private void clearFilters() {
        blockedSounds.clear();
    }

    private void saveFilters() {
        GeneralSoundsConfig.Reaction reaction = GeneralSoundsConfig.getMobReactions().get(entityId);
        if (reaction != null) {
            reaction.blocked_sounds = new ArrayList<>(blockedSounds);
            GeneralSoundsConfig.persist();
        }

        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void render(DrawContext graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFFFF);

        graphics.drawTextWithShadow(this.textRenderer, "Search and click to block/unblock sounds:",
                this.width / 2 - FIELD_WIDTH / 2, 25, 0xFFFFFFFF);

        renderSuggestions(graphics, mouseX, mouseY);

        graphics.drawCenteredTextWithShadow(this.textRenderer, "§7Blocked: " + blockedSounds.size() + " sounds",
                this.width / 2, this.height - 50, 0xFFAAAAAA);

        renderSuggestionTooltip(graphics, mouseX, mouseY);
    }

    private void renderSuggestions(DrawContext graphics, int mouseX, int mouseY) {
        String query = searchBox.getText().toLowerCase().trim();

        List<SuggestionEntry> filtered = suggestions.stream()
                .filter(s -> {
                    if (showOnlyBlocked && !blockedSounds.contains(s.soundId)) {
                        return false;
                    }
                    return query.isEmpty() ||
                            s.soundId.toLowerCase().contains(query) ||
                            s.category.toLowerCase().contains(query);
                })
                .collect(Collectors.toList());

        int startX = this.width / 2 - FIELD_WIDTH / 2;
        int startY = searchBox.getY() + FIELD_HEIGHT + 10;
        int endY = this.height - 70;
        int boxHeight = endY - startY;
        int contentWidth = FIELD_WIDTH - SCROLLBAR_WIDTH - 2;

        graphics.fill(startX, startY, startX + FIELD_WIDTH, endY, 0xDD000000);
        graphics.fill(startX, startY, startX + FIELD_WIDTH, startY + 1, 0xFF555555);
        graphics.fill(startX, endY - 1, startX + FIELD_WIDTH, endY, 0xFF555555);

        if (filtered.isEmpty()) {
            String noResults = query.isEmpty() ? "Start typing to search..." : "No sounds found";
            graphics.drawTextWithShadow(this.textRenderer, noResults,
                    startX + contentWidth / 2, startY + boxHeight / 2 - 4, 0xFF888888);
        } else {
            int maxVisible = boxHeight / SUGGESTION_HEIGHT;
            int maxScroll = Math.max(0, filtered.size() - maxVisible);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

            String lastCategory = null;
            int renderY = startY;
            int visibleCount = 0;

            for (int i = scrollOffset; i < filtered.size() && visibleCount < maxVisible; i++) {
                SuggestionEntry entry = filtered.get(i);

                if (renderY + SUGGESTION_HEIGHT > endY) break;

                boolean isHovered = mouseX >= startX && mouseX <= startX + contentWidth &&
                        mouseY >= renderY && mouseY <= renderY + SUGGESTION_HEIGHT;

                if (i % 2 == 0) {
                    graphics.fill(startX, renderY, startX + contentWidth, renderY + SUGGESTION_HEIGHT, 0x20FFFFFF);
                }
                if (isHovered) {
                    graphics.fill(startX, renderY, startX + contentWidth, renderY + SUGGESTION_HEIGHT, 0x40FFFFFF);
                }

                if (!entry.category.equals(lastCategory)) {
                    graphics.fill(startX, renderY, startX + 4, renderY + SUGGESTION_HEIGHT, getCategoryColor(entry.category));
                    lastCategory = entry.category;
                }

                String displayText = entry.soundId;
                int maxTextWidth = contentWidth - 50;
                if (this.textRenderer.getWidth(displayText) > maxTextWidth) {
                    displayText = truncateText(displayText, maxTextWidth);
                }

                boolean isBlocked = blockedSounds.contains(entry.soundId);
                int textColor = isBlocked ? 0xFFFF5555 : 0xFFFFFFFF;
                graphics.drawTextWithShadow(this.textRenderer, displayText, startX + 10, renderY + 6, textColor);

                if (isBlocked) {
                    graphics.fill(startX + contentWidth - 25, renderY + 5, startX + contentWidth - 5, renderY + SUGGESTION_HEIGHT - 5, 0xFF555555);
                    graphics.drawTextWithShadow(this.textRenderer, "✓", startX + contentWidth - 20, renderY + 6, 0xFFFFFFFF);
                }

                renderY += SUGGESTION_HEIGHT;
                visibleCount++;
            }

            if (filtered.size() > maxVisible) {
                int scrollbarX = startX + FIELD_WIDTH - SCROLLBAR_WIDTH;
                int scrollbarTrackHeight = boxHeight;
                int scrollbarThumbHeight = Math.max(30, (maxVisible * scrollbarTrackHeight) / filtered.size());
                int scrollbarThumbY = startY + (scrollOffset * (scrollbarTrackHeight - scrollbarThumbHeight)) /
                        Math.max(1, filtered.size() - maxVisible);

                graphics.fill(scrollbarX, startY, scrollbarX + SCROLLBAR_WIDTH, endY, 0xFF222222);

                boolean isScrollbarHovered = mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH &&
                        mouseY >= scrollbarThumbY && mouseY <= scrollbarThumbY + scrollbarThumbHeight;

                int thumbColor = isDraggingScrollbar ? 0xFFCCCCCC : (isScrollbarHovered ? 0xFFBBBBBB : 0xFFAAAAAA);
                graphics.fill(scrollbarX + 1, scrollbarThumbY, scrollbarX + SCROLLBAR_WIDTH - 1,
                        scrollbarThumbY + scrollbarThumbHeight, thumbColor);
            }
        }
    }

    private void renderSuggestionTooltip(DrawContext graphics, int mouseX, int mouseY) {
        String query = searchBox.getText().toLowerCase().trim();
        List<SuggestionEntry> filtered = suggestions.stream()
                .filter(s -> {
                    if (showOnlyBlocked && !blockedSounds.contains(s.soundId)) {
                        return false;
                    }
                    return query.isEmpty() ||
                            s.soundId.toLowerCase().contains(query) ||
                            s.category.toLowerCase().contains(query);
                })
                .collect(Collectors.toList());

        int startX = this.width / 2 - FIELD_WIDTH / 2;
        int startY = searchBox.getY() + FIELD_HEIGHT + 10;
        int endY = this.height - 70;
        int boxHeight = endY - startY;
        int contentWidth = FIELD_WIDTH - SCROLLBAR_WIDTH - 2;

        if (filtered.isEmpty()) return;

        if (mouseX >= startX && mouseX <= startX + contentWidth &&
                mouseY >= startY && mouseY < endY) {

            int relativeY = mouseY - startY;
            int index = (relativeY / SUGGESTION_HEIGHT) + scrollOffset;

            if (index >= 0 && index < filtered.size()) {
                SuggestionEntry entry = filtered.get(index);
                List<Text> tooltip = new ArrayList<>();
                tooltip.add(Text.literal("§6" + entry.category));

                if (blockedSounds.contains(entry.soundId)) {
                    tooltip.add(Text.literal("§cClick to unblock"));
                } else {
                    tooltip.add(Text.literal("§aClick to block"));
                }

                //graphics.renderTooltip();
            }
        }
    }

    private int getCategoryColor(String category) {
        return switch (category) {
            case "Explosions" -> 0xFFFF5555;
            case "Gunfire" -> 0xFFFFAA00;
            case "Blocks" -> 0xFF55FF55;
            case "Movement" -> 0xFF5555FF;
            case "Entity Sounds" -> 0xFFFF55FF;
            case "Music" -> 0xFF55FFFF;
            default -> 0xFFAAAAAA;
        };
    }

    private String truncateText(String text, int maxWidth) {
        if (this.textRenderer.getWidth(text) <= maxWidth) return text;

        String ellipsis = "...";
        StringBuilder truncated = new StringBuilder();
        for (char c : text.toCharArray()) {
            truncated.append(c);
            if (this.textRenderer.getWidth(truncated + ellipsis) >= maxWidth) {
                truncated.setLength(truncated.length() - 1);
                break;
            }
        }

        return truncated + ellipsis;
    }




    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            String query = searchBox.getText().toLowerCase().trim();
            List<SuggestionEntry> filtered = suggestions.stream()
                    .filter(s -> {
                        if (showOnlyBlocked && !blockedSounds.contains(s.soundId)) {
                            return false;
                        }
                        return query.isEmpty() ||
                                s.soundId.toLowerCase().contains(query) ||
                                s.category.toLowerCase().contains(query);
                    })
                    .collect(Collectors.toList());

            int startX = this.width / 2 - FIELD_WIDTH / 2;
            int startY = searchBox.getY() + FIELD_HEIGHT + 10;
            int endY = this.height - 70;
            int boxHeight = endY - startY;
            int contentWidth = FIELD_WIDTH - SCROLLBAR_WIDTH - 2;

            int maxVisible = boxHeight / SUGGESTION_HEIGHT;

            if (filtered.size() > maxVisible) {
                int scrollbarX = startX + FIELD_WIDTH - SCROLLBAR_WIDTH;
                int scrollbarTrackHeight = boxHeight;
                int scrollbarThumbHeight = Math.max(30, (maxVisible * scrollbarTrackHeight) / filtered.size());
                int scrollbarThumbY = startY + (scrollOffset * (scrollbarTrackHeight - scrollbarThumbHeight)) /
                        Math.max(1, filtered.size() - maxVisible);

                if (click.x() >= scrollbarX && click.y() <= scrollbarX + SCROLLBAR_WIDTH &&
                        click.y() >= scrollbarThumbY && click.y() <= scrollbarThumbY + scrollbarThumbHeight) {
                    isDraggingScrollbar = true;
                    dragStartY = (int) click.y();
                    dragStartOffset = scrollOffset;
                    return true;
                }
            }

            if (click.x() >= startX && click.x() <= startX + contentWidth &&
                    click.y() >= startY && click.y() < endY) {

                int relativeY = (int) click.y() - startY;
                int index = (relativeY / SUGGESTION_HEIGHT) + scrollOffset;

                if (index >= 0 && index < filtered.size()) {
                    SuggestionEntry entry = filtered.get(index);
                    toggleBlockedSound(entry.soundId);
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }


    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0 && isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (isDraggingScrollbar) {
            String query = searchBox.getText().toLowerCase().trim();
            long filteredCount = suggestions.stream()
                    .filter(s -> {
                        if (showOnlyBlocked && !blockedSounds.contains(s.soundId)) {
                            return false;
                        }
                        return query.isEmpty() ||
                                s.soundId.toLowerCase().contains(query) ||
                                s.category.toLowerCase().contains(query);
                    })
                    .count();

            int startY = searchBox.getY() + FIELD_HEIGHT + 10;
            int endY = this.height - 70;
            int boxHeight = endY - startY;
            int maxVisible = boxHeight / SUGGESTION_HEIGHT;

            int scrollbarTrackHeight = boxHeight;
            int scrollbarThumbHeight = Math.max(30, (maxVisible * scrollbarTrackHeight) / (int) filteredCount);
            int availableTrackHeight = scrollbarTrackHeight - scrollbarThumbHeight;

            int deltaY = (int) offsetY - dragStartY;
            int maxScroll = Math.max(0, (int) filteredCount - maxVisible);

            if (availableTrackHeight > 0) {
                int deltaScroll = (deltaY * maxScroll) / availableTrackHeight;
                scrollOffset = Math.max(0, Math.min(dragStartOffset + deltaScroll, maxScroll));
            }

            return true;
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    private void toggleBlockedSound(String soundId) {
        if (blockedSounds.contains(soundId)) {
            blockedSounds.remove(soundId);
        } else {
            blockedSounds.add(soundId);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int startX = this.width / 2 - FIELD_WIDTH / 2;
        int startY = searchBox.getY() + FIELD_HEIGHT + 10;
        int endY = this.height - 70;

        if (mouseX >= startX && mouseX <= startX + FIELD_WIDTH &&
                mouseY >= startY && mouseY < endY) {

            scrollOffset -= (int) scrollY;

            String query = searchBox.getText().toLowerCase().trim();
            long filteredCount = suggestions.stream()
                    .filter(s -> {
                        if (showOnlyBlocked && !blockedSounds.contains(s.soundId)) {
                            return false;
                        }
                        return query.isEmpty() ||
                                s.soundId.toLowerCase().contains(query) ||
                                s.category.toLowerCase().contains(query);
                    })
                    .count();

            int boxHeight = endY - startY;
            int maxVisible = boxHeight / SUGGESTION_HEIGHT;
            int maxScroll = Math.max(0, (int) filteredCount - maxVisible);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == 256) { // ESC key
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }
        return super.keyPressed(input);
    }

    private record SuggestionEntry(String soundId, String category) {
    }
}