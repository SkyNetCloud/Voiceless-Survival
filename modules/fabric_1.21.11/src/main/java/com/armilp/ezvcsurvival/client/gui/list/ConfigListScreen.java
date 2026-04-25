package com.armilp.ezvcsurvival.client.gui.list;

import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.packets.UpdateConfigPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class ConfigListScreen extends Screen {

    public enum ListType {
        ENTITY_CONFIG,
        GENERAL_SOUNDS_CONFIG
    }

    private final ListType listType;
    private final Screen parent;

    private TextFieldWidget searchBox;
    private String searchQuery = "";
    private ButtonWidget clearSearchButton;
    private ButtonWidget backButton;
    private ButtonWidget toggleViewButton;
    private ButtonWidget toggleEnabledButton;
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
        int topRowY = 25;
        int searchRowY = topRowY + BUTTON_HEIGHT + VERTICAL_SPACING * 2;
        int listStartY = searchRowY + SEARCH_HEIGHT + VERTICAL_SPACING * 3;

        int buttonCount = getButtonCount();
        int buttonWidth = Math.clamp(
                (usableWidth - (buttonCount - 1) * HORIZONTAL_SPACING) / buttonCount, MIN_BUTTON_WIDTH, MAX_BUTTON_WIDTH);

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

        toggleEnabledButton = ButtonWidget.builder(
                getToggleEnabledMessage(),
                b -> toggleEnabled()
        ).dimensions(currentX, topRowY, buttonWidth, BUTTON_HEIGHT).build();
        this.addDrawableChild(toggleEnabledButton);

        int searchWidth = Math.min(300, usableWidth - 100);
        searchBox = new TextFieldWidget(this.textRenderer, leftMargin, searchRowY, searchWidth, SEARCH_HEIGHT,
                Text.translatable("textbox.ezvcsurvival.search"));
        searchBox.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(searchBox);

        clearSearchButton = ButtonWidget.builder(Text.literal("✕"), b -> searchBox.setText(""))
                .dimensions(leftMargin + searchWidth + HORIZONTAL_SPACING, searchRowY, 20, SEARCH_HEIGHT).build();
        this.addDrawableChild(clearSearchButton);

        this.list = new ConfigListWidget(this, this.client, this.width, this.height, listStartY, 28);
        this.addDrawableChild(this.list);

        loadData();
        updateList();
    }

    private int getButtonCount() {
        return listType == ListType.GENERAL_SOUNDS_CONFIG ? 4 : 3;
    }

    private void loadData() {
        items = new ArrayList<>();
        if (listType == ListType.ENTITY_CONFIG) loadEntityConfigs();
        else if (showingSounds) loadGeneralSoundConfigs();
        else loadGeneralEntityConfigs();
    }

    private void loadEntityConfigs() {
        for (EntityType<?> type : Registries.ENTITY_TYPE) {
            if (type.getSpawnGroup() == SpawnGroup.MISC) continue;
            String id = Registries.ENTITY_TYPE.getId(type).toString();
            if (EntityVoiceConfig.get(id) == null) {
                EntityVoiceConfig.set(id, EntityVoiceConfig.EntityConfig.defaultFor(type));
            }
            items.add(new EntityConfigItem(id, type));
        }
    }

    private void loadGeneralSoundConfigs() {
        Map<String, GeneralSoundsConfig.SoundEntry> soundConfigs = GeneralSoundsConfig.getSounds();
        for (SoundEvent sound : Registries.SOUND_EVENT) {
            String id = Objects.requireNonNull(Registries.SOUND_EVENT.getId(sound)).toString();
            if (!soundConfigs.containsKey(id)) {
                GeneralSoundsConfig.setSoundEntry(id, false, 1.0, 1.0);
            }
            items.add(new SoundConfigItem(id, sound));
        }
    }

    private void loadGeneralEntityConfigs() {
        for (EntityType<?> type : Registries.ENTITY_TYPE) {
            if (type.getSpawnGroup() == SpawnGroup.MISC) continue;
            String id = Registries.ENTITY_TYPE.getId(type).toString();
            if (!GeneralSoundsConfig.getMobReactions().containsKey(id)) {
                GeneralSoundsConfig.setMobReaction(id, true, 1.0, 50.0);
            }
            items.add(new EntityReactionItem(id));
        }
    }

    private void toggleView() {
        showingSounds = !showingSounds;
        toggleViewButton.setMessage(Text.translatable(showingSounds ? "button.ezvcsurvival.show_entities" : "button.ezvcsurvival.show_sounds"));
        safeRefresh();
    }

    private void toggleEnabled() {
        if (listType == ListType.ENTITY_CONFIG) {
            boolean state = !EntityVoiceConfig.isEnabled();
            EntityVoiceConfig.ROOT.enabled = state;
            EntityVoiceConfig.persist();
            EZVCNetwork.ezvcNetworkService.sendToServer(
                    new UpdateConfigPacket(UpdateConfigPacket.ConfigType.ENTITY_VOICE,
                            "global", state, 1.0, 1.0));
        } else {
            boolean state = !GeneralSoundsConfig.isEnabled();
            GeneralSoundsConfig.ROOT.enabled = state;
            GeneralSoundsConfig.persist();
            EZVCNetwork.ezvcNetworkService.sendToServer(
                    new UpdateConfigPacket(UpdateConfigPacket.ConfigType.GENERAL_SOUND,
                            "global", state, 1.0, 1.0));
        }
        toggleEnabledButton.setMessage(getToggleEnabledMessage());
        safeRefresh();
    }

    private Text getToggleEnabledMessage() {
        return Text.translatable(
                (listType == ListType.ENTITY_CONFIG ? EntityVoiceConfig.isEnabled() : GeneralSoundsConfig.isEnabled())
                        ? "button.ezvcsurvival.disable_all"
                        : "button.ezvcsurvival.enable_all"
        );
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
        if (item instanceof EntityConfigItem e) return e.getDisplayName().getString()+" "+e.getId();
        if (item instanceof SoundConfigItem s) return s.getId();
        if (item instanceof EntityReactionItem r) return r.id();
        return "";
    }

    private void onSearchChanged(String query) {
        searchQuery = query.trim().toLowerCase();
        updateList();
    }

    public void safeRefresh() {
        loadData();
        updateList();
    }

    public static class EntityConfigItem {
        private final String id;
        private final EntityType<?> entry;

        public EntityConfigItem(String id, EntityType<?> entry) {
            this.id = id;
            this.entry = entry;
        }

        public String getId() { return id; }

        public EntityVoiceConfig.EntityConfig getConfig() {
            return EntityVoiceConfig.get(id);
        }

        public Text getDisplayName() { return entry.getName(); }
    }

    public static class SoundConfigItem {
        private final String id;
        private final SoundEvent sound;

        public SoundConfigItem(String id, SoundEvent sound) {
            this.id = id;
            this.sound = sound;
        }

        public String getId() { return id; }

        public GeneralSoundsConfig.SoundEntry getConfig() {
            return GeneralSoundsConfig.getSounds().get(id);
        }
    }

    public record EntityReactionItem(String id) {
        public GeneralSoundsConfig.Reaction getReaction() {
            return GeneralSoundsConfig.getMobReactions().get(id);
        }
    }
}