package com.armilp.ezvcsurvival.client.gui.list;

import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static net.minecraft.registry.Registries.ENTITY_TYPE;
import static net.minecraft.registry.Registries.SOUND_EVENT;

public class ConfigListScreen extends Screen {

    public enum ListType {
        ENTITY_CONFIG,
        GENERAL_SOUNDS_CONFIG
    }

    private final ListType listType;
    private final Screen parent;

    private EditBoxWidget searchBox;
    private String searchQuery = "";
    private ButtonWidget clearSearchButton;
    private ButtonWidget refreshButton;
    private ButtonWidget backButton;
    private ButtonWidget toggleViewButton;
    private ButtonWidget toggleEnabledButton;
    private ConfigListWidget list;

    private List<Object> items;
    private boolean showingSounds = true;

    private static final int MIN_WIDTH = 400;
    private static final int MIN_BUTTON_WIDTH = 60;
    private static final int MAX_BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SEARCH_HEIGHT = 20;
    private static final int VERTICAL_SPACING = 6;
    private static final int HORIZONTAL_SPACING = 6;

    public ConfigListScreen(ListType listType, Screen parent) {
        super(Text.translatable(getTitleKey(listType)));
        this.listType = listType;
        this.parent = parent;
        this.items = new ArrayList<>();
        if (this.listType == ListType.GENERAL_SOUNDS_CONFIG) {
            this.showingSounds = false;
        }
    }

    private static String getTitleKey(ListType type) {
        switch (type) {
            case ENTITY_CONFIG: return "screen.ezvcsurvival.entity_config_list";
            case GENERAL_SOUNDS_CONFIG: return "screen.ezvcsurvival.general_sounds_config_list";
            default: return "screen.ezvcsurvival.config_list";
        }
    }

    @Override
    public void renderBackground(DrawContext graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void init() {
        super.init();
        int usableWidth = Math.max(this.width - 40, MIN_WIDTH);
        int leftMargin = (this.width - usableWidth) / 2;
        int rightMargin = leftMargin;
        int topRowY = 20;
        int searchRowY = topRowY + BUTTON_HEIGHT + VERTICAL_SPACING * 2;
        int listStartY = searchRowY + SEARCH_HEIGHT + VERTICAL_SPACING * 3;
        int availableButtonWidth = usableWidth;
        int buttonCount = getButtonCount();
        int buttonWidth = Math.min(MAX_BUTTON_WIDTH, Math.max(MIN_BUTTON_WIDTH,
                (availableButtonWidth - (buttonCount - 1) * HORIZONTAL_SPACING) / buttonCount));

        backButton = ButtonWidget.builder(Text.translatable("button.ezvcsurvival.back"), b ->
                MinecraftClient.getInstance().setScreen(parent != null ? parent : new ConfigEditorScreen())
        ).dimensions(leftMargin, topRowY, buttonWidth, BUTTON_HEIGHT).build();
        this.addDrawableChild(backButton);

        int currentX = leftMargin + buttonWidth + HORIZONTAL_SPACING;

        if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            toggleViewButton = ButtonWidget.builder(
                    Text.translatable(showingSounds ? "button.ezvcsurvival.show_entities" : "button.ezvcsurvival.show_sounds"),
                    b -> toggleView()
            ).dimensions(currentX, topRowY, buttonWidth, BUTTON_HEIGHT).build();
            this.addDrawableChild(toggleViewButton);
            currentX += buttonWidth + HORIZONTAL_SPACING;
        }

        if (listType == ListType.GENERAL_SOUNDS_CONFIG || listType == ListType.ENTITY_CONFIG) {
            toggleEnabledButton = ButtonWidget.builder(
                    getToggleEnabledMessage(),
                    b -> toggleEnabled()
            ).dimensions(currentX, topRowY, buttonWidth, BUTTON_HEIGHT).build();
            this.addDrawableChild(toggleEnabledButton);
            currentX += buttonWidth + HORIZONTAL_SPACING;
        }

        refreshButton = ButtonWidget.builder(Text.translatable("button.ezvcsurvival.refresh"), b -> refreshData())
                .dimensions(this.width - rightMargin - buttonWidth, topRowY, buttonWidth, BUTTON_HEIGHT).build();
        this.addDrawableChild(refreshButton);

        int searchWidth = Math.min(300, usableWidth - 100);
        searchBox = new EditBoxWidget(textRenderer, leftMargin, searchRowY, searchWidth, SEARCH_HEIGHT,
                Text.translatable("textbox.ezvcsurvival.search"), null);
        searchBox.setChangeListener(this::onSearchChanged);
        searchBox.setMaxLength(50);
        this.addDrawableChild(searchBox);

        clearSearchButton = ButtonWidget.builder(Text.literal("✕"), b -> searchBox.setText(""))
                .dimensions(leftMargin + searchWidth + HORIZONTAL_SPACING, searchRowY, 20, SEARCH_HEIGHT).build();
        this.addDrawableChild(clearSearchButton);

        this.list = new ConfigListWidget(this, this.client, this.width, this.height, listStartY, 28, 20);
        this.addDrawableChild(this.list);

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
        for (EntityType<?> type : ENTITY_TYPE) {
            SpawnGroup category = type.getSpawnGroup();
            if (category == SpawnGroup.MISC) continue;
            String id = Objects.requireNonNull(ENTITY_TYPE.getKey(type)).toString();
            EntityVoiceConfig.EntityConfig config = EntityVoiceConfig.get(id);
            if (config == null) {
                config = EntityVoiceConfig.EntityConfig.defaultFor(type);
                EntityVoiceConfig.set(id, config);
            }
            items.add(new EntityConfigItem(id, type, config));
        }
        items.sort(Comparator.comparing(item -> ((EntityConfigItem) item).getDisplayName()));
    }

    private void loadGeneralSoundConfigs() {
        items.clear();
        Map<String, GeneralSoundsConfig.SoundEntry> soundConfigs = GeneralSoundsConfig.getSounds();
        for (SoundEvent sound : SOUND_EVENT) {
            String id = Objects.requireNonNull(SOUND_EVENT.getKey(sound)).toString();
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
            entityConfigs = Collections.emptyMap();
        }
        for (EntityType<?> type : ENTITY_TYPE) {
            SpawnGroup category = type.getSpawnGroup();
            if (category == SpawnGroup.MISC) continue;
            String id = Objects.requireNonNull(ENTITY_TYPE.getKey(type)).toString();
            GeneralSoundsConfig.Reaction reaction = entityConfigs.get(id);
            if (reaction == null) {
                reaction = new GeneralSoundsConfig.Reaction(true, 1.0, category == SpawnGroup.MONSTER ? 60.0 : 50.0);
                GeneralSoundsConfig.setMobReaction(id, true, 1.0, category == SpawnGroup.MONSTER ? 60.0 : 50.0);
            }
            items.add(new EntityReactionItem(id, reaction));
        }
        items.sort(Comparator.comparing(item -> ((EntityReactionItem) item).getId()));
    }

    private void toggleView() {
        if (listType != ListType.GENERAL_SOUNDS_CONFIG) return;
        showingSounds = !showingSounds;
        toggleViewButton.setMessage(Text.translatable(showingSounds ? "button.ezvcsurvival.show_entities" : "button.ezvcsurvival.show_sounds"));
        safeRefresh();
    }

    private void toggleEnabled() {
        switch (listType) {
            case ENTITY_CONFIG:
                boolean newEntityState = !EntityVoiceConfig.isEnabled();
                EntityVoiceConfig.ROOT.enabled = newEntityState;
                EntityVoiceConfig.persist();
                PacketDistributor.sendToServer(
                        new UpdateConfigPacket(UpdateConfigPacket.ConfigType.ENTITY_VOICE,
                                "global", newEntityState, 1.0, 1.0));
                break;
            case GENERAL_SOUNDS_CONFIG:
                boolean newGeneralState = !GeneralSoundsConfig.isEnabled();
                GeneralSoundsConfig.ROOT.enabled = newGeneralState;
                GeneralSoundsConfig.persist();
                PacketDistributor.sendToServer(
                        new UpdateConfigPacket(UpdateConfigPacket.ConfigType.GENERAL_SOUND,
                                "global", newGeneralState, 1.0, 1.0));
                break;
        }
        toggleEnabledButton.setMessage(getToggleEnabledMessage());
        safeRefresh();
    }

    private Text getToggleEnabledMessage() {
        boolean isEnabled = false;
        switch (listType) {
            case ENTITY_CONFIG:
                isEnabled = EntityVoiceConfig.isEnabled();
                break;
            case GENERAL_SOUNDS_CONFIG:
                isEnabled = GeneralSoundsConfig.isEnabled();
                break;
        }
        return Text.translatable(isEnabled ? "button.ezvcsurvival.disable_all" : "button.ezvcsurvival.enable_all");
    }

    public void refreshData() {
        safeRefresh();
        PacketDistributor.sendToServer(
                new UpdateConfigPacket(UpdateConfigPacket.ConfigType.GENERAL_SOUND,
                        "refresh", true, 1.0, 1.0));
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
    public void render(DrawContext graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
        int titleWidth = textRenderer.getWidth(this.title);
        int separatorY = 20;
        graphics.fill(this.width / 2 - titleWidth / 2 - 10, separatorY,
                this.width / 2 + titleWidth / 2 + 10, separatorY + 1, 0x55FFFFFF);
        this.list.render(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        searchBox.render(graphics, mouseX, mouseY, partialTick);
        if (searchBox.getValue().isEmpty() && !searchBox.isFocused()) {
            graphics.drawString(
                    textRenderer,
                    searchBox.getMessage().getString(),
                    searchBox.getX() + 4,
                    searchBox.getY() + 6,
                    0x888888,
                    false
            );
        }
        int searchLabelY = searchBox.getY() - 12;
        graphics.drawString(textRenderer, Text.translatable("gui.ezvcsurvival.search"),
                searchBox.getX(), searchLabelY, 0xCCCCCC, false);
        renderFooter(graphics);
        renderTooltips(graphics, mouseX, mouseY);
    }

    private void renderFooter(GuiGraphics graphics) {
        int footerY = this.height - 30;
        int leftMargin = (this.width - Math.max(this.width - 40, MIN_WIDTH)) / 2;
        Text count = getCountComponent();
        int countWidth = textRenderer.width(count);
        graphics.drawString(textRenderer, count, this.width - countWidth - leftMargin, footerY, 0xAAAAAA, false);
        Text instructions = getInstructionsComponent();
        graphics.drawString(textRenderer, instructions, leftMargin, footerY, 0xCCCCCC, false);
    }

    private Text getCountComponent() {
        if (listType == ListType.ENTITY_CONFIG) {
            return Text.translatable("gui.ezvcsurvival.entity_count", list.children().size());
        } else if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            if (showingSounds) {
                return Text.translatable("gui.ezvcsurvival.sound_count", list.children().size());
            } else {
                return Text.translatable("gui.ezvcsurvival.entity_count", list.children().size());
            }
        }
        return Text.translatable("gui.ezvcsurvival.entity_count", list.children().size());
    }

    private Text getInstructionsComponent() {
        if (listType == ListType.ENTITY_CONFIG) {
            return Text.translatable("gui.ezvcsurvival.click_to_edit_entity");
        } else if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            if (showingSounds) {
                return Text.translatable("gui.ezvcsurvival.click_to_edit_sound");
            } else {
                return Text.translatable("gui.ezvcsurvival.click_to_edit_entity");
            }
        }
        return Text.translatable("gui.ezvcsurvival.click_to_edit_entity");
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (searchBox.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.minecraft.setScreen(parent != null ? parent : new ConfigEditorScreen());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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

    public static class EntityConfigItem {
        private final String id;
        private final EntityType<?> type;
        private final EntityVoiceConfig.EntityConfig config;

        public EntityConfigItem(String id, EntityType<?> type, EntityVoiceConfig.EntityConfig config) {
            this.id = id;
            this.type = type;
            this.config = config;
        }

        public String getId() { return id; }
        public EntityVoiceConfig.EntityConfig getConfig() { return config; }
        public String getDisplayName() { return type.getDescription().getString(); }
    }

    public static class SoundConfigItem {
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

        public String getId() { return id; }
        public SoundEvent getSound() { return sound; }
        public GeneralSoundsConfig.SoundEntry getConfig() { return config; }
    }

    public static class EntityReactionItem {
        private final String id;
        private final GeneralSoundsConfig.Reaction reaction;

        public EntityReactionItem(String id, GeneralSoundsConfig.Reaction reaction) {
            this.id = id;
            this.reaction = reaction;
        }

        public String getId() { return id; }
        public GeneralSoundsConfig.Reaction getReaction() { return reaction; }
    }

    private void renderTooltips(DrawContext graphics, int mouseX, int mouseY) {
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