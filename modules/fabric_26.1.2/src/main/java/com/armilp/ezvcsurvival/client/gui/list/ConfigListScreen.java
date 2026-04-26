package com.armilp.ezvcsurvival.client.gui.list;

import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
import com.armilp.ezvcsurvival.client.gui.list.interfaces.RenderableConfigItem;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.UpdateConfigPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnGroupData;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class ConfigListScreen extends Screen {

    public enum ListType {
        ENTITY_CONFIG,
        GENERAL_SOUNDS_CONFIG
    }

    private final ListType listType;
    private final Screen parent;

    private EditBox searchBox;
    private String searchQuery = "";
    private Button clearSearchButton;
    private Button backButton;
    private Button toggleViewButton;
    private Button toggleEnabledButton;
    private ConfigListWidget list;

    private java.util.List<Object> items;
    private boolean showingSounds = true;

    private static final int MIN_WIDTH = 400;
    private static final int MIN_BUTTON_WIDTH = 60;
    private static final int MAX_BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SEARCH_HEIGHT = 15;
    private static final int VERTICAL_SPACING = 14;
    private static final int HORIZONTAL_SPACING = 6;

    public ConfigListScreen(ListType listType, Screen parent) {
        super(Component.translatable(getTitleKey(listType)));
        this.listType = listType;
        this.parent = parent;
        this.items = new ArrayList<>();
        if (this.listType == ListType.GENERAL_SOUNDS_CONFIG) {
            this.showingSounds = false;
        }
    }

    private static String getTitleKey(ListType type) {
        switch (type) {
            case ENTITY_CONFIG:
                return "screen.ezvcsurvival.entity_config_list";
            case GENERAL_SOUNDS_CONFIG:
                return "screen.ezvcsurvival.general_sounds_config_list";
            default:
                return "screen.ezvcsurvival.config_list";
        }
    }

    @Override
    protected void init() {
        super.init();
        int usableWidth = Math.max(this.width - 40, MIN_WIDTH);
        int leftMargin = (this.width - usableWidth) / 2;
        int rightMargin = leftMargin;
        int topRowY = 25;
        int searchRowY = topRowY + BUTTON_HEIGHT + VERTICAL_SPACING * 2;
        int listStartY = searchRowY + SEARCH_HEIGHT + VERTICAL_SPACING * 3;
        int buttonCount = getButtonCount();
        int buttonWidth = Math.clamp(
                (usableWidth - (long) (buttonCount - 1) * HORIZONTAL_SPACING) / buttonCount, MIN_BUTTON_WIDTH, MAX_BUTTON_WIDTH);

        backButton = Button.builder(Component.translatable("button.ezvcsurvival.back"), b ->
                Minecraft.getInstance().setScreen(parent != null ? parent : new ConfigEditorScreen())
        ).bounds(leftMargin, topRowY, buttonWidth, BUTTON_HEIGHT).build();
        this.addRenderableWidget(backButton);

        int currentX = leftMargin + buttonWidth + HORIZONTAL_SPACING;

        if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            toggleViewButton = Button.builder(
                    Component.translatable(showingSounds ? "button.ezvcsurvival.show_entities" : "button.ezvcsurvival.show_sounds"),
                    b -> toggleView()
            ).bounds(currentX, topRowY, buttonWidth, BUTTON_HEIGHT).build();
            this.addRenderableWidget(toggleViewButton);
            currentX += buttonWidth + HORIZONTAL_SPACING;
        }

        if (listType == ListType.GENERAL_SOUNDS_CONFIG || listType == ListType.ENTITY_CONFIG) {
            toggleEnabledButton = Button.builder(
                    getToggleEnabledMessage(),
                    b -> toggleEnabled()
            ).bounds(currentX, topRowY, buttonWidth, BUTTON_HEIGHT).build();
            this.addRenderableWidget(toggleEnabledButton);
            currentX += buttonWidth + HORIZONTAL_SPACING;
        }

        int searchWidth = Math.min(300, usableWidth - 100);
        searchBox = new EditBox(this.font, leftMargin, searchRowY, searchWidth, SEARCH_HEIGHT,
                Component.translatable("textbox.ezvcsurvival.search"));
        searchBox.setResponder(this::onSearchChanged);
        searchBox.setMaxLength(50);
        this.addRenderableWidget(searchBox);

        clearSearchButton = Button.builder(Component.literal("✕"), b -> searchBox.setValue(""))
                .bounds(leftMargin + searchWidth + HORIZONTAL_SPACING, searchRowY, 20, SEARCH_HEIGHT).build();
        this.addRenderableWidget(clearSearchButton);

        this.list = new ConfigListWidget(this, this.minecraft, this.width, this.height, listStartY, 28, 20);
        this.addRenderableWidget(this.list);

        if (items == null || items.isEmpty()) {
            loadData();
        }

        updateList();
    }

    private int getButtonCount() {
        int count = 2;
        if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            count += 2;
        } else if (listType == ListType.ENTITY_CONFIG) {
            count += 1;
        }
        return count;
    }

    private void loadData() {
        items = new ArrayList<>();
        switch (listType) {
            case ENTITY_CONFIG:
                loadEntityConfigs();
                break;
            case GENERAL_SOUNDS_CONFIG:
                if (showingSounds) {
                    loadGeneralSoundConfigs();
                } else {
                    loadGeneralEntityConfigs();
                }
                break;
        }
    }

    private void loadEntityConfigs() {
        items.clear();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            MobCategory category = type.getCategory();
            if (category == MobCategory.MISC) continue;
            String id = Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(type)).toString();
            EntityVoiceConfig.EntityConfig config = EntityVoiceConfig.get(id);
            if (config == null) {
                config = EntityVoiceConfig.EntityConfig.defaultFor(type);
                EntityVoiceConfig.set(id, config);
            }
            items.add(new EntityConfigItem(id, type, config));
        }
        items.sort(Comparator.comparing(item -> ((EntityConfigItem) item).getDisplayName().getString()));
    }

    private void loadGeneralSoundConfigs() {
        items.clear();
        Map<String, GeneralSoundsConfig.SoundEntry> soundConfigs = GeneralSoundsConfig.getSounds();
        for (SoundEvent sound : BuiltInRegistries.SOUND_EVENT) {
            String id = Objects.requireNonNull(BuiltInRegistries.SOUND_EVENT.getKey(sound)).toString();
            GeneralSoundsConfig.SoundEntry config = soundConfigs.get(id);
            if (config == null) {
                config = new GeneralSoundsConfig.SoundEntry(false, 1.0, 1.0);
                GeneralSoundsConfig.setSoundEntry(id, false, 1.0, 1.0);
            }
            items.add(new SoundConfigItem(id, sound, config, false));
        }
        items.sort(Comparator.comparing(item -> ((SoundConfigItem) item).getId()));
    }

    private void loadGeneralEntityConfigs() {
        items.clear();
        Map<String, GeneralSoundsConfig.Reaction> entityConfigs = GeneralSoundsConfig.getMobReactions();
        if (entityConfigs == null) {
            entityConfigs = java.util.Collections.emptyMap();
        }
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            MobCategory category = type.getCategory();
            if (category == MobCategory.MISC) continue;
            String id = Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(type)).toString();
            GeneralSoundsConfig.Reaction reaction = entityConfigs.get(id);
            if (reaction == null) {
                reaction = new GeneralSoundsConfig.Reaction(true, 1.0, category == MobCategory.MONSTER ? 60.0 : 50.0);
                GeneralSoundsConfig.setMobReaction(id, true, 1.0, category == MobCategory.MONSTER ? 60.0 : 50.0);
            }
            items.add(new EntityReactionItem(id, reaction));
        }
        items.sort(Comparator.comparing(item -> ((EntityReactionItem) item).getId()));
    }

    private void toggleView() {
        if (listType != ListType.GENERAL_SOUNDS_CONFIG) return;
        showingSounds = !showingSounds;
        toggleViewButton.setMessage(Component.translatable(showingSounds ? "button.ezvcsurvival.show_entities" : "button.ezvcsurvival.show_sounds"));
        safeRefresh();
    }

    private void toggleEnabled() {
        switch (listType) {
            case ENTITY_CONFIG:
                boolean newEntityState = !EntityVoiceConfig.isEnabled();
                EntityVoiceConfig.ROOT.enabled = newEntityState;
                EntityVoiceConfig.persist();

                EZVCNetwork.sendConfigUpdateToServer(
                        UpdateConfigPayload.ConfigType.ENTITY_VOICE,
                        "global",
                        newEntityState,
                        1.0,
                        1.0,
                        0.0,
                        false
                );
                break;

            case GENERAL_SOUNDS_CONFIG:
                boolean newGeneralState = !GeneralSoundsConfig.isEnabled();
                GeneralSoundsConfig.ROOT.enabled = newGeneralState;
                GeneralSoundsConfig.persist();

                EZVCNetwork.sendConfigUpdateToServer(
                        UpdateConfigPayload.ConfigType.GENERAL_SOUND,
                        "global",
                        newGeneralState,
                        1.0,
                        1.0,
                        0.0,
                        false
                );
                break;
        }
        toggleEnabledButton.setMessage(getToggleEnabledMessage());
        safeRefresh();
    }

    private Component getToggleEnabledMessage() {
        boolean isEnabled = false;
        switch (listType) {
            case ENTITY_CONFIG:
                isEnabled = EntityVoiceConfig.isEnabled();
                break;
            case GENERAL_SOUNDS_CONFIG:
                isEnabled = GeneralSoundsConfig.isEnabled();
                break;
        }
        return Component.translatable(isEnabled ? "button.ezvcsurvival.disable_all" : "button.ezvcsurvival.enable_all");
    }

    public void refreshData() {
        safeRefresh();

        String configType = switch (listType) {
            case ENTITY_CONFIG -> "ENTITY_VOICE";
            case GENERAL_SOUNDS_CONFIG -> "GENERAL_SOUND";
            default -> "GENERAL_SOUND_ENTITY";
        };

        EZVCNetwork.sendRefreshRequest(configType);
    }

    public void updateList() {
        list.clear();
        List<Object> filtered = new ArrayList<>();
        for (Object item : items) {
            String searchableText = getSearchableText(item).toLowerCase();
            if (searchQuery.isEmpty() || searchableText.contains(searchQuery)) {
                filtered.add(item);
            }
        }
        for (Object item : filtered) {
            list.addItem(item);
        }
    }

    private String getSearchableText(Object item) {
        if (item instanceof EntityConfigItem) {
            return ((EntityConfigItem) item).getDisplayName() + " " + ((EntityConfigItem) item).getId();
        } else if (item instanceof SoundConfigItem) {
            return ((SoundConfigItem) item).getId();
        } else if (item instanceof EntityReactionItem) {
            return ((EntityReactionItem) item).getId();
        }
        return "";
    }

    private void onSearchChanged(String query) {
        this.searchQuery = query.trim().toLowerCase();
        updateList();
    }



    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.text(this.font, this.title, this.width / 2, 5, 0xFFFFFFFF);

        // Render count and instructions at the TOP (right after title)
        renderTopInfo(graphics);

        int titleWidth = this.font.width(this.title);
        int separatorY = 20;
        graphics.fill(this.width / 2 - titleWidth / 2 - 10, separatorY, this.width / 2 + titleWidth / 2 + 10, separatorY + 1, 0x55FFFFFF);
        this.list.extractRenderState(graphics, mouseX, mouseY, partialTick);
        searchBox.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (searchBox.getValue().isEmpty() && !searchBox.isFocused()) {
            graphics.text(
                    this.font,
                    searchBox.getMessage().getString(),
                    searchBox.getX() + 4,
                    searchBox.getY() + 6,
                    0xFF888888,
                    false
            );
        }
        int searchLabelY = searchBox.getY() - 10;
        graphics.text(this.font, Component.translatable("gui.ezvcsurvival.search"),
                searchBox.getX(), searchLabelY, 0xFFCCCCCC, false);

        renderTooltips(graphics, mouseX, mouseY);
    }

    private void renderTopInfo(GuiGraphicsExtractor graphics) {
        int topInfoY = 100; // Position right below the separator line
        int leftMargin = (this.width - Math.max(this.width - 40, MIN_WIDTH)) / 2;

        // Get count and instructions components
        Component count = getCountComponent();
        Component instructions = getInstructionsComponent();

        // Draw count on the right side
        int countWidth = this.font.width(count);
        int countX = this.width - countWidth - leftMargin;
        graphics.text(this.font, count, countX, topInfoY, 0xFFAAAAAA, false);

        // Draw instructions on the left side
        graphics.text(this.font, instructions, leftMargin, topInfoY, 0xFFCCCCCC, false);
    }

    private Component getCountComponent() {
        if (listType == ListType.ENTITY_CONFIG) {
            return Component.translatable("gui.ezvcsurvival.entity_count", list.children().size());
        } else if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            if (showingSounds) {
                return Component.translatable("gui.ezvcsurvival.sound_count", list.children().size());
            } else {
                return Component.translatable("gui.ezvcsurvival.entity_count", list.children().size());
            }
        }
        return Component.translatable("gui.ezvcsurvival.entity_count", list.children().size());
    }

    private Component getInstructionsComponent() {
        if (listType == ListType.ENTITY_CONFIG) {
            return Component.translatable("gui.ezvcsurvival.click_to_edit_entity");
        } else if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            if (showingSounds) {
                return Component.translatable("gui.ezvcsurvival.click_to_edit_sound");
            } else {
                return Component.translatable("gui.ezvcsurvival.click_to_edit_entity");
            }
        }
        return Component.translatable("gui.ezvcsurvival.click_to_edit_entity");
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (searchBox.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }



    @Override
    public boolean keyPressed(@NonNull KeyEvent keyCode) {
        if (keyCode.key() == 256) {
            this.minecraft.setScreen(parent != null ? parent : new ConfigEditorScreen());
            return true;
        }
        return super.keyPressed(keyCode);
    }

    @Override
    public void tick() {
        super.tick();
    }

    public void onConfigUpdated() {
        loadData();
        updateList();
        switch (listType) {
            case ENTITY_CONFIG:
                EntityVoiceConfig.persist();
                break;
            case GENERAL_SOUNDS_CONFIG:
                GeneralSoundsConfig.persist();
                break;
        }
    }

    public void safeRefresh() {
        if (this.minecraft != null && this.minecraft.screen == this) {
            loadData();
            updateList();
        }
    }

    public static class EntityConfigItem implements RenderableConfigItem {
        private final String id;
        private final EntityType<?> entry;
        private final EntityVoiceConfig.EntityConfig config;

        public EntityConfigItem(String id, EntityType<?> entry, EntityVoiceConfig.EntityConfig config) {
            this.id = id;
            this.entry = entry;
            this.config = config;
        }

        public String getId() {
            return id;
        }

        public EntityVoiceConfig.EntityConfig getConfig() {
            return config;
        }

        public Component  getDisplayName() {
            return entry.getDescription();
        }

        @Override
        public boolean isEnabled() {
            return config.enabled;
        }

        @Override
        public boolean shouldTruncate() {
            return false;
        }
    }



    public static class SoundConfigItem implements RenderableConfigItem {
        private final String id;
        private final SoundEvent sound;
        private final GeneralSoundsConfig.SoundEntry config;
        private final boolean isPriority;

        public SoundConfigItem(String id, SoundEvent sound, GeneralSoundsConfig.SoundEntry config, boolean isPriority) {
            this.id = id;
            this.sound = sound;
            this.config = config;
            this.isPriority = isPriority;
        }

        public String getId() {
            return id;
        }

        public SoundEvent getSound() {
            return sound;
        }

        public GeneralSoundsConfig.SoundEntry getConfig() {
            return config;
        }

        @Override
        public Component getDisplayName() {
            return Component.literal(id);
        }

        @Override
        public boolean isEnabled() {
            return config.enabled;
        }

        @Override
        public boolean shouldTruncate() {
            return false;
        }
    }



    public static class EntityReactionItem implements RenderableConfigItem {
        private final String id;
        private final GeneralSoundsConfig.Reaction reaction;

        public EntityReactionItem(String id, GeneralSoundsConfig.Reaction reaction) {
            this.id = id;
            this.reaction = reaction;
        }

        public String getId() { return id; }
        public GeneralSoundsConfig.Reaction getReaction() { return reaction; }

        @Override
        public Component getDisplayName() {
            return Component.literal(id);
        }

        @Override
        public boolean isEnabled() {
            return reaction.enabled;
        }

        @Override
        public boolean shouldTruncate() {
            return true;
        }
    }




    private void renderTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (list == null) return;
        if (!list.isMouseOver(mouseX, mouseY)) {
            return;
        }
        int index = list.getEntryIndexAt(mouseX, mouseY);
        if (index < 0 || index >= list.children().size()) {
            return;
        }
        ConfigListWidget.Entry entry = list.children().get(index);
        if (entry instanceof ConfigListWidget.UniversalEntry universalEntry) {
            universalEntry.renderTooltip(graphics, mouseX, mouseY);
        }
    }

}