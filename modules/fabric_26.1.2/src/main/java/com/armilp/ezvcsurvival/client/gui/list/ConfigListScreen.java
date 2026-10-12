package com.armilp.ezvcsurvival.client.gui.list;

import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.GunfireConfig;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.packets.UpdateConfigPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.*;

public class ConfigListScreen extends Screen {

    public enum ListType {
        ENTITY_CONFIG,
        GENERAL_SOUNDS_CONFIG,
        GUNFIRE_CONFIG
    }

    private final ListType listType;
    private final Screen parent;

    private EditBox searchBox;
    private String searchQuery = "";
    private Button toggleViewButtonWidget;
    private Button toggleEnabledButtonWidget;
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
        super(Component.translatable(getTitleKey(listType)));
        this.listType = listType;
        this.parent = parent;
        this.items = new ArrayList<>();
        if (this.listType == ListType.GENERAL_SOUNDS_CONFIG || this.listType == ListType.GUNFIRE_CONFIG) {
            this.showingSounds = false;
        }
    }

    private static String getTitleKey(ListType type) {
        return switch (type) {
            case ENTITY_CONFIG -> "screen.ezvcsurvival.entity_config_list";
            case GENERAL_SOUNDS_CONFIG -> "screen.ezvcsurvival.general_sounds_config_list";
            case GUNFIRE_CONFIG ->  "screen.ezvcsurvival.gunfire_config_list";
        };
    }

    @Override
    protected void init() {
        super.init();
        int usableWidth = Math.max(this.width - 40, MIN_WIDTH);
        int leftMargin = (this.width - usableWidth) / 2;
        int topRowY = 20;
        int searchRowY = topRowY + BUTTON_HEIGHT + VERTICAL_SPACING * 2;
        int listStartY = searchRowY + SEARCH_HEIGHT + VERTICAL_SPACING * 3;

        int buttonCount = getButtonCount();
        int buttonWidth = Math.clamp(
                (usableWidth - (long) (buttonCount - 1) * HORIZONTAL_SPACING) / buttonCount, MIN_BUTTON_WIDTH, MAX_BUTTON_WIDTH);

        Button backButtonWidget = Button.builder(Component.translatable("button.ezvcsurvival.back"), b ->
                minecraft.setScreen(parent != null ? parent : new ConfigEditorScreen())
        ).bounds(leftMargin, topRowY + 5, buttonWidth, BUTTON_HEIGHT).build();
        this.addRenderableWidget(backButtonWidget);

        int currentX = leftMargin + buttonWidth + HORIZONTAL_SPACING;

        if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            toggleViewButtonWidget = Button.builder(
                    Component.translatable(showingSounds ? "button.ezvcsurvival.show_entities" : "button.ezvcsurvival.show_sounds"),
                    b -> toggleView()
            ).bounds(currentX, topRowY + 5, buttonWidth, BUTTON_HEIGHT).build();
            this.addRenderableWidget(toggleViewButtonWidget);
            currentX += buttonWidth + HORIZONTAL_SPACING;
        }

        {
            toggleEnabledButtonWidget = Button.builder(
                    getToggleEnabledMessage(),
                    b -> toggleEnabled()
            ).bounds(currentX, topRowY + 5, buttonWidth, BUTTON_HEIGHT).build();
            this.addRenderableWidget(toggleEnabledButtonWidget);
        }

        int searchWidth = Math.min(300, usableWidth - 100);
        searchBox = new EditBox(this.font, leftMargin, searchRowY + 10, searchWidth, SEARCH_HEIGHT,
                Component.translatable("textbox.ezvcsurvival.search"));
        searchBox.setResponder(this::onSearchChanged);
        searchBox.setMaxLength(50);
        this.addRenderableWidget(searchBox);

        Button clearSearchButtonWidget = Button.builder(Component.literal("✕"), b -> searchBox.setValue(""))
                .bounds(leftMargin + searchWidth + HORIZONTAL_SPACING, searchRowY + 9, 20, SEARCH_HEIGHT).build();
        this.addRenderableWidget(clearSearchButtonWidget);

        this.list = new ConfigListWidget(this, this.minecraft, this.width, this.height - 125, listStartY, 28);
        this.addRenderableWidget(this.list);

        if (items == null || items.isEmpty()) {
            loadData();
        }

        updateList();
    }

    private int getButtonCount() {
        int count = 2; // Back + Enable/Disable All
        if (listType == ListType.GENERAL_SOUNDS_CONFIG) count += 1; // view toggle
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
            case GUNFIRE_CONFIG:
                loadGunfireEntityConfigs();
                break;
        }
    }

    private void loadEntityConfigs() {
        items.clear();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            MobCategory category = type.getCategory();
            if (category == MobCategory.MISC) continue;
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            EntityVoiceConfig.EntityConfig config = EntityVoiceConfig.get(id);
            if (config == null) {
                config = EntityVoiceConfig.EntityConfig.defaultFor(type);
                EntityVoiceConfig.set(id, config);
            }
            items.add(new EntityConfigItem(id, type));
        }
        items.sort(Comparator.comparing(item -> ((EntityConfigItem) item).getDisplayName()));
    }

    private void loadGeneralSoundConfigs() {
        items.clear();
        Map<String, GeneralSoundsConfig.SoundEntry> soundConfigs = GeneralSoundsConfig.getSounds();
        for (SoundEvent sound : BuiltInRegistries.SOUND_EVENT) {
            String id = BuiltInRegistries.SOUND_EVENT.getKey(sound).toString();
            if (!soundConfigs.containsKey(id)) {
                GeneralSoundsConfig.setSoundEntry(id, false, 1.0, 1.0);
            }
            items.add(new SoundConfigItem(id, sound));
        }
        items.sort(Comparator.comparing(item -> ((SoundConfigItem) item).id()));
    }

    private void loadGeneralEntityConfigs() {
        items.clear();
        Map<String, GeneralSoundsConfig.Reaction> entityConfigs = GeneralSoundsConfig.getMobReactions();
        if (entityConfigs == null) entityConfigs = Collections.emptyMap();

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            MobCategory category = type.getCategory();
            if (category == MobCategory.MISC) continue;
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            GeneralSoundsConfig.Reaction reaction = entityConfigs.get(id);
            if (reaction == null) {
                double range = category == MobCategory.MONSTER ? 60.0 : 50.0;
                reaction = new GeneralSoundsConfig.Reaction(true, 1.0, range);
                GeneralSoundsConfig.setMobReaction(id, true, 1.0, range);
            }
            items.add(new EntityReactionItem(id, reaction));
        }
        items.sort(Comparator.comparing(item -> ((EntityReactionItem) item).id()));
    }

    private void loadGunfireEntityConfigs() {
        items.clear();
        Map<String, GunfireConfig.Reaction> entityConfigs = GunfireConfig.getMobReactions();

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            MobCategory category = type.getCategory();
            if (category == MobCategory.MISC) continue;

            String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            GunfireConfig.Reaction reaction = entityConfigs.get(id);

            if (reaction == null) {
                reaction = new GunfireConfig.Reaction(true, 1.0, category == MobCategory.MONSTER ? 50.0 : 40.0);
                GunfireConfig.setMobReaction(id, true, 1.0, category == MobCategory.MONSTER ? 50.0 : 40.0);
            }

            items.add(new GunfireEntityItem(id, reaction));
        }

        items.sort(Comparator.comparing(item -> ((GunfireEntityItem) item).id()));
    }

    private void toggleView() {
        if (listType != ListType.GENERAL_SOUNDS_CONFIG) return;
        showingSounds = !showingSounds;
        toggleViewButtonWidget.setMessage(Component.translatable(showingSounds
                ? "button.ezvcsurvival.show_entities"
                : "button.ezvcsurvival.show_sounds"));
        safeRefresh();
    }

    private void toggleEnabled() {
        // If anything is off, "Enable All" turns everything on; otherwise "Disable All" turns everything off.
        boolean state = anyDisabled();

        switch (listType) {
            case ENTITY_CONFIG -> {
                EntityVoiceConfig.ROOT.enabled = state;
                for (String id : EntityVoiceConfig.getAllEntityIds()) {
                    EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.get(id);
                    if (cfg != null) cfg.enabled = state;
                }
                EntityVoiceConfig.persist();
                EZVCNetwork.ezvcNetworkService.sendToServer(
                        new UpdateConfigPacket(UpdateConfigPacket.ConfigType.ENTITY_VOICE,
                                "global", state, 1.0, 1.0));
            }
            case GENERAL_SOUNDS_CONFIG -> {
                GeneralSoundsConfig.ROOT.enabled = state;

                Map<String, GeneralSoundsConfig.SoundEntry> sounds = GeneralSoundsConfig.getSounds();
                if (sounds != null) sounds.values().forEach(cfg -> cfg.enabled = state);

                Map<String, GeneralSoundsConfig.Reaction> reactions = GeneralSoundsConfig.getMobReactions();
                if (reactions != null) reactions.values().forEach(cfg -> cfg.enabled = state);

                GeneralSoundsConfig.persist();
                EZVCNetwork.ezvcNetworkService.sendToServer(
                        new UpdateConfigPacket(UpdateConfigPacket.ConfigType.GENERAL_SOUND,
                                "global", state, 1.0, 1.0));
            }
            case GUNFIRE_CONFIG -> {
                GunfireConfig.ROOT.enabled = state;

                Map<String, GunfireConfig.Reaction> reactions = GunfireConfig.getMobReactions();
                if (reactions != null) reactions.values().forEach(cfg -> cfg.enabled = state);

                GunfireConfig.persist();
                EZVCNetwork.ezvcNetworkService.sendToServer(
                        new UpdateConfigPacket(UpdateConfigPacket.ConfigType.GUNFIRE_SOUND,
                                "global", state, 1.0, 1.0));
            }
        }
        toggleEnabledButtonWidget.setMessage(getToggleEnabledMessage());
        updateList();
    }

    /** True if at least one entry in the current list type is individually disabled. */
    private boolean anyDisabled() {
        return switch (listType) {
            case ENTITY_CONFIG -> EntityVoiceConfig.getAllEntityIds().stream()
                    .map(EntityVoiceConfig::get)
                    .anyMatch(c -> c != null && !c.enabled);
            case GENERAL_SOUNDS_CONFIG -> {
                Map<String, GeneralSoundsConfig.SoundEntry> sounds = GeneralSoundsConfig.getSounds();
                Map<String, GeneralSoundsConfig.Reaction> reactions = GeneralSoundsConfig.getMobReactions();
                yield (sounds != null && sounds.values().stream().anyMatch(c -> !c.enabled))
                        || (reactions != null && reactions.values().stream().anyMatch(c -> !c.enabled));
            }
            case GUNFIRE_CONFIG -> {
                Map<String, GunfireConfig.Reaction> reactions = GunfireConfig.getMobReactions();
                yield reactions != null && reactions.values().stream().anyMatch(c -> !c.enabled);
            }
        };
    }

    private Component getToggleEnabledMessage() {
        return Component.translatable(anyDisabled()
                ? "button.ezvcsurvival.enable_all"
                : "button.ezvcsurvival.disable_all");
    }

    public void updateList() {
        list.clear();
        for (Object item : items) {
            if (searchQuery.isEmpty() || getSearchableText(item).toLowerCase().contains(searchQuery)) {
                list.addItem(item);
            }
        }
    }

    private String getSearchableText(Object item) {
        if (item instanceof EntityConfigItem e) return e.getDisplayName() + " " + e.id;
        if (item instanceof SoundConfigItem s) return s.id;
        if (item instanceof EntityReactionItem r) return r.id;
        if (item instanceof GunfireEntityItem g) return g.id;
        return "";
    }

    private void onSearchChanged(String query) {
        this.searchQuery = query.trim().toLowerCase();
        updateList();
    }



    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // Title
        graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);

        // Separator under title
        int titleWidth = this.font.width(this.title);
        graphics.fill(this.width / 2 - titleWidth / 2 - 10, 20,
                this.width / 2 + titleWidth / 2 + 10, 21, 0x55FFFFFF);

        // List
        this.list.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // Search box
        searchBox.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // Search placeholder
        if (searchBox.getValue().isEmpty() && !searchBox.isFocused()) {
            graphics.text(this.font,
                    searchBox.getMessage().getString(),
                    searchBox.getX() + 4,
                    searchBox.getY() + 6,
                    0xFF888888,
                    false);
        }

        // Search label
        graphics.text(this.font,
                Component.translatable("gui.ezvcsurvival.search"),
                searchBox.getX(), searchBox.getY() - 10, 0xFFCCCCCC, false);

        renderFooter(graphics);
        renderTooltips(graphics, mouseX, mouseY);
    }

    private void renderFooter(GuiGraphicsExtractor graphics) {
        int footerY = this.height - 20;
        int leftMargin = (this.width - Math.max(this.width - 40, MIN_WIDTH)) / 2;

        // Item count (bottom right)
        Component count = getCountText();
        int countWidth = this.font.width(count);
        graphics.text(this.font, count,
                this.width - countWidth - leftMargin, footerY, 0xFFAAAAAA, false);

        // Instructions (bottom left)
        Component instructions = getInstructionsText();
        graphics.text(this.font, instructions,
                leftMargin, footerY, 0xFFCCCCCC, false);
    }

    private Component getCountText() {
        if (listType == ListType.ENTITY_CONFIG) {
            return Component.translatable("gui.ezvcsurvival.entity_count", list.children().size());
        } else if (showingSounds) {
            return Component.translatable("gui.ezvcsurvival.sound_count", list.children().size());
        }
        return Component.translatable("gui.ezvcsurvival.entity_count", list.children().size());
    }

    private Component getInstructionsText() {
        if (listType == ListType.ENTITY_CONFIG) {
            return Component.translatable("gui.ezvcsurvival.click_to_edit_entity");
        } else if (showingSounds) {
            return Component.translatable("gui.ezvcsurvival.click_to_edit_sound");
        }
        return Component.translatable("gui.ezvcsurvival.click_to_edit_entity");
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (searchBox.isMouseOver(mouseX, mouseY)) return false;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.input() == 256) {
            minecraft.setScreen(null);
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public void tick() {
        super.tick();
    }

    public void onConfigUpdated() {
        loadData();
        updateList();
        switch (listType) {
            case ENTITY_CONFIG -> EntityVoiceConfig.persist();
            case GENERAL_SOUNDS_CONFIG -> GeneralSoundsConfig.persist();
            case GUNFIRE_CONFIG -> GunfireConfig.persist();
        }
    }

    public void safeRefresh() {
        loadData();
        updateList();
        if (toggleEnabledButtonWidget != null) {
            toggleEnabledButtonWidget.setMessage(getToggleEnabledMessage());
        }
    }


    private void renderTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (list == null || !list.isMouseOver(mouseX, mouseY)) return;
        int index = list.getEntryIndexAt(mouseX, mouseY);
        if (index < 0 || index >= list.children().size()) return;
        ConfigListWidget.Entry entry = list.children().get(index);
        if (entry instanceof ConfigListWidget.UniversalEntry universalEntry) {
            universalEntry.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    // ── Item types ────────────────────────────────────────────────────────────
    public record EntityConfigItem(String id, EntityType<?> type) {
        public EntityVoiceConfig.EntityConfig getConfig() {
            return EntityVoiceConfig.get(id);
        }

        public String getDisplayName() {
            return type.getDescription().getString();
        }
    }


    public record SoundConfigItem(String id, SoundEvent sound) {
        public GeneralSoundsConfig.SoundEntry getConfig() {
            return GeneralSoundsConfig.getSounds().get(id);
        }
    }

    public record EntityReactionItem(String id, GeneralSoundsConfig.Reaction reaction) {
        public GeneralSoundsConfig.Reaction getReaction() {
            return GeneralSoundsConfig.getMobReactions().get(id);
        }
    }

    public record GunfireEntityItem(String id, GunfireConfig.Reaction reaction) {
        public GunfireConfig.Reaction getReaction() {
            Map<String, GunfireConfig.Reaction> map = GunfireConfig.getMobReactions();
            return map != null ? map.get(id) : null;
        }
    }
}