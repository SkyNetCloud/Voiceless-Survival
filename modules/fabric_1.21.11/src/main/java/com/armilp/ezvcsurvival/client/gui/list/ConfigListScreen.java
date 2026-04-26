package com.armilp.ezvcsurvival.client.gui.list;

import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.packets.UpdateConfigPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;

import java.util.*;

public class ConfigListScreen extends Screen {

    public enum ListType {
        ENTITY_CONFIG,
        GENERAL_SOUNDS_CONFIG
    }

    private final ListType listType;
    private final Screen parent;

    private TextFieldWidget searchBox;
    private String searchQuery = "";
    private ButtonWidget clearSearchButtonWidget;
    private ButtonWidget backButtonWidget;
    private ButtonWidget toggleViewButtonWidget;
    private ButtonWidget toggleEnabledButtonWidget;
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
        return switch (type) {
            case ENTITY_CONFIG -> "screen.ezvcsurvival.entity_config_list";
            case GENERAL_SOUNDS_CONFIG -> "screen.ezvcsurvival.general_sounds_config_list";
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

        backButtonWidget = ButtonWidget.builder(Text.translatable("button.ezvcsurvival.back"), b ->
                MinecraftClient.getInstance().setScreen(parent != null ? parent : new ConfigEditorScreen())
        ).dimensions(leftMargin, topRowY + 5, buttonWidth, BUTTON_HEIGHT).build();
        this.addDrawableChild(backButtonWidget);

        int currentX = leftMargin + buttonWidth + HORIZONTAL_SPACING;

        if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            toggleViewButtonWidget = ButtonWidget.builder(
                    Text.translatable(showingSounds ? "button.ezvcsurvival.show_entities" : "button.ezvcsurvival.show_sounds"),
                    b -> toggleView()
            ).dimensions(currentX, topRowY + 5, buttonWidth, BUTTON_HEIGHT).build();
            this.addDrawableChild(toggleViewButtonWidget);
            currentX += buttonWidth + HORIZONTAL_SPACING;
        }

        if (listType == ListType.GENERAL_SOUNDS_CONFIG || listType == ListType.ENTITY_CONFIG) {
            toggleEnabledButtonWidget = ButtonWidget.builder(
                    getToggleEnabledMessage(),
                    b -> toggleEnabled()
            ).dimensions(currentX, topRowY + 5, buttonWidth, BUTTON_HEIGHT).build();
            this.addDrawableChild(toggleEnabledButtonWidget);
        }

        int searchWidth = Math.min(300, usableWidth - 100);
        searchBox = new TextFieldWidget(this.textRenderer, leftMargin, searchRowY + 10, searchWidth, SEARCH_HEIGHT,
                Text.translatable("textbox.ezvcsurvival.search"));
        searchBox.setChangedListener(this::onSearchChanged);
        searchBox.setMaxLength(50);
        this.addDrawableChild(searchBox);

        clearSearchButtonWidget = ButtonWidget.builder(Text.literal("✕"), b -> searchBox.setText(""))
                .dimensions(leftMargin + searchWidth + HORIZONTAL_SPACING, searchRowY + 9, 20, SEARCH_HEIGHT).build();
        this.addDrawableChild(clearSearchButtonWidget);

        this.list = new ConfigListWidget(this, this.client, this.width, this.height - 125, listStartY, 28);
        this.addDrawableChild(this.list);

        if (items == null || items.isEmpty()) {
            loadData();
        }

        updateList();
    }

    private int getButtonCount() {
        int count = 2;
        if (listType == ListType.GENERAL_SOUNDS_CONFIG) count += 2;
        else if (listType == ListType.ENTITY_CONFIG) count += 1;
        return count;
    }

    private void loadData() {
        items = new ArrayList<>();
        switch (listType) {
            case ENTITY_CONFIG -> loadEntityConfigs();
            case GENERAL_SOUNDS_CONFIG -> {
                if (showingSounds) loadGeneralSoundConfigs();
                else loadGeneralEntityConfigs();
            }
        }
    }

    private void loadEntityConfigs() {
        items.clear();
        for (EntityType<?> type : Registries.ENTITY_TYPE) {
            SpawnGroup category = type.getSpawnGroup();
            if (category == SpawnGroup.MISC) continue;
            String id = Registries.ENTITY_TYPE.getId(type).toString();
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
        for (SoundEvent sound : Registries.SOUND_EVENT) {
            String id = Registries.SOUND_EVENT.getId(sound).toString();
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
        if (entityConfigs == null) entityConfigs = Collections.emptyMap();

        for (EntityType<?> type : Registries.ENTITY_TYPE) {
            SpawnGroup category = type.getSpawnGroup();
            if (category == SpawnGroup.MISC) continue;
            String id = Registries.ENTITY_TYPE.getId(type).toString();
            GeneralSoundsConfig.Reaction reaction = entityConfigs.get(id);
            if (reaction == null) {
                double range = category == SpawnGroup.MONSTER ? 60.0 : 50.0;
                reaction = new GeneralSoundsConfig.Reaction(true, 1.0, range);
                GeneralSoundsConfig.setMobReaction(id, true, 1.0, range);
            }
            items.add(new EntityReactionItem(id, reaction));
        }
        items.sort(Comparator.comparing(item -> ((EntityReactionItem) item).id()));
    }

    private void toggleView() {
        if (listType != ListType.GENERAL_SOUNDS_CONFIG) return;
        showingSounds = !showingSounds;
        toggleViewButtonWidget.setMessage(Text.translatable(showingSounds
                ? "button.ezvcsurvival.show_entities"
                : "button.ezvcsurvival.show_sounds"));
        safeRefresh();
    }

    private void toggleEnabled() {
        switch (listType) {
            case ENTITY_CONFIG -> {
                boolean state = !EntityVoiceConfig.isEnabled();
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
                boolean state = !GeneralSoundsConfig.isEnabled();
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
        }
        toggleEnabledButtonWidget.setMessage(getToggleEnabledMessage());
        updateList();
    }

    private Text getToggleEnabledMessage() {
        boolean isEnabled = switch (listType) {
            case ENTITY_CONFIG -> EntityVoiceConfig.isEnabled();
            case GENERAL_SOUNDS_CONFIG -> GeneralSoundsConfig.isEnabled();
        };
        return Text.translatable(isEnabled
                ? "button.ezvcsurvival.disable_all"
                : "button.ezvcsurvival.enable_all");
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
        if (item instanceof EntityConfigItem e) return e.getDisplayName() + " " + e.getId();
        if (item instanceof SoundConfigItem s) return s.getId();
        if (item instanceof EntityReactionItem r) return r.id();
        return "";
    }

    private void onSearchChanged(String query) {
        this.searchQuery = query.trim().toLowerCase();
        updateList();
    }

    @Override
    public void render(DrawContext graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Title
        graphics.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFFFF);

        // Separator under title
        int titleWidth = this.textRenderer.getWidth(this.title);
        graphics.fill(this.width / 2 - titleWidth / 2 - 10, 20,
                this.width / 2 + titleWidth / 2 + 10, 21, 0x55FFFFFF);

        // List
        this.list.render(graphics, mouseX, mouseY, partialTick);

        // Search box
        searchBox.render(graphics, mouseX, mouseY, partialTick);

        // Search placeholder
        if (searchBox.getText().isEmpty() && !searchBox.isFocused()) {
            graphics.drawText(this.textRenderer,
                    searchBox.getMessage().getString(),
                    searchBox.getX() + 4,
                    searchBox.getY() + 6,
                    0xFF888888,
                    false);
        }

        // Search label
        graphics.drawText(this.textRenderer,
                Text.translatable("gui.ezvcsurvival.search"),
                searchBox.getX(), searchBox.getY() - 10, 0xFFCCCCCC, false);

        renderFooter(graphics);
        renderTooltips(graphics, mouseX, mouseY);
    }

    private void renderFooter(DrawContext graphics) {
        int footerY = this.height - 20;
        int leftMargin = (this.width - Math.max(this.width - 40, MIN_WIDTH)) / 2;

        // Item count (bottom right)
        Text count = getCountText();
        int countWidth = this.textRenderer.getWidth(count);
        graphics.drawText(this.textRenderer, count,
                this.width - countWidth - leftMargin, footerY, 0xFFAAAAAA, false);

        // Instructions (bottom left)
        Text instructions = getInstructionsText();
        graphics.drawText(this.textRenderer, instructions,
                leftMargin, footerY, 0xFFCCCCCC, false);
    }

    private Text getCountText() {
        if (listType == ListType.ENTITY_CONFIG) {
            return Text.translatable("gui.ezvcsurvival.entity_count", list.children().size());
        } else if (showingSounds) {
            return Text.translatable("gui.ezvcsurvival.sound_count", list.children().size());
        }
        return Text.translatable("gui.ezvcsurvival.entity_count", list.children().size());
    }

    private Text getInstructionsText() {
        if (listType == ListType.ENTITY_CONFIG) {
            return Text.translatable("gui.ezvcsurvival.click_to_edit_entity");
        } else if (showingSounds) {
            return Text.translatable("gui.ezvcsurvival.click_to_edit_sound");
        }
        return Text.translatable("gui.ezvcsurvival.click_to_edit_entity");
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (searchBox.isMouseOver(mouseX, mouseY)) return false;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyInput keyEvent) {
        if (keyEvent.getKeycode() == 256) {
            MinecraftClient.getInstance().setScreen(null);
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
        }
    }

    public void safeRefresh() {
        loadData();
        updateList();
    }


    private void renderTooltips(DrawContext graphics, int mouseX, int mouseY) {
        if (list == null || !list.isMouseOver(mouseX, mouseY)) return;
        int index = list.getEntryIndexAt(mouseX, mouseY);
        if (index < 0 || index >= list.children().size()) return;
        ConfigListWidget.Entry entry = list.children().get(index);
        if (entry instanceof ConfigListWidget.UniversalEntry universalEntry) {
            universalEntry.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    // ── Item types ────────────────────────────────────────────────────────────

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

        // Always read live from config so toggle reflects immediately
        public EntityVoiceConfig.EntityConfig getConfig() { return EntityVoiceConfig.get(id); }

        public String getDisplayName() { return type.getName().getString(); }
    }

    public static class SoundConfigItem {
        private final String id;
        private final SoundEvent sound;
        private final boolean isPriority;

        public SoundConfigItem(String id, SoundEvent sound, GeneralSoundsConfig.SoundEntry config, boolean isPriority) {
            this.id = id;
            this.sound = sound;
            this.isPriority = isPriority;
        }

        public String getId() { return id; }
        public SoundEvent getSound() { return sound; }

        // Always read live from config
        public GeneralSoundsConfig.SoundEntry getConfig() { return GeneralSoundsConfig.getSounds().get(id); }
    }

    public record EntityReactionItem(String id, GeneralSoundsConfig.Reaction reaction) {
        // Always read live from config
        public GeneralSoundsConfig.Reaction getReaction() {
            return GeneralSoundsConfig.getMobReactions().get(id);
        }
    }
}
