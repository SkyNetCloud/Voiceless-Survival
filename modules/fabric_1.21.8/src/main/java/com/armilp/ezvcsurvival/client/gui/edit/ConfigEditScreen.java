package com.armilp.ezvcsurvival.client.gui.edit;


import com.armilp.ezvcsurvival.client.gui.list.ConfigListScreen;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ConfigEditScreen extends Screen {

    public static final Identifier MENU_BACKGROUND_TEXTURE = Identifier.ofVanilla("textures/gui/menu_background.png");

    public enum EditType {
        ENTITY_CONFIG,
        GENERAL_SOUND_CONFIG,
        GENERAL_SOUND_ENTITY
    }

    private final Screen parent;
    private final EditType editType;
    private final String elementId;
    private final String elementName;
    private final String cleanElementId;

    private ButtonWidget enabledButton;
    private TextFieldWidget speedBox;
    private TextFieldWidget rangeBox;
    private TextFieldWidget thresholdBox;
    private ButtonWidget priorityButton;
    private ButtonWidget saveButton;
    private ButtonWidget soundFiltersButton;

    private boolean enabled;
    private double speed;
    private double range;
    private double threshold;
    private boolean isPriority;

    private boolean originalEnabled;
    private double originalSpeed;
    private double originalRange;
    private double originalThreshold;
    private boolean originalIsPriority;

    private static final int TOP_MARGIN = 30;
    private static final int CENTER_X_OFFSET = 80;
    private static final int FIELD_WIDTH = 160;
    private static final int FIELD_HEIGHT = 20;
    private static final int FIELD_SPACING = 30;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 10;

    public ConfigEditScreen(Screen parent, EditType editType, String elementId, String elementName) {
        super(Text.translatable(getTitleKey(editType), elementName));
        this.parent = parent;
        this.editType = editType;
        this.elementId = elementId;
        this.elementName = elementName;
        this.cleanElementId = cleanElementId(elementId);

        loadCurrentValues();
    }



    private static String cleanElementId(String rawId) {
        if (rawId == null || rawId.isEmpty()) {
            return rawId;
        }

        // Handle Optional[ResourceKey[minecraft:entity_type / minecraft:allay]]
        if (rawId.contains("Optional[ResourceKey[")) {
            // Find the part after the slash
            int slashIndex = rawId.indexOf('/');
            if (slashIndex > 0) {
                // Extract from after slash to before the closing bracket
                String idPart = rawId.substring(slashIndex + 1);
                int bracketIndex = idPart.indexOf(']');
                if (bracketIndex > 0) {
                    return idPart.substring(0, bracketIndex).trim();
                }
            }
        }

        return rawId;
    }

    private static String getTitleKey(EditType editType) {
        return switch (editType) {
            case ENTITY_CONFIG -> "screen.ezvcsurvival.entity_config_edit";
            case GENERAL_SOUND_CONFIG -> "screen.ezvcsurvival.general_sound_config_edit";
            case GENERAL_SOUND_ENTITY -> "screen.ezvcsurvival.general_sound_entity_edit";
            default -> "screen.ezvcsurvival.config_edit";
        };
    }

    private void loadCurrentValues() {
        switch (editType) {
            case ENTITY_CONFIG -> {
                EntityVoiceConfig.EntityConfig entityConfig = EntityVoiceConfig.get(cleanElementId);
                if (entityConfig != null) {
                    this.enabled = entityConfig.enabled;
                    this.speed = entityConfig.speed;
                    this.range = entityConfig.range;
                    this.threshold = entityConfig.threshold;

                    this.originalEnabled = entityConfig.enabled;
                    this.originalSpeed = entityConfig.speed;
                    this.originalRange = entityConfig.range;
                    this.originalThreshold = entityConfig.threshold;
                } else {
                    this.enabled = true;
                    this.speed = 1.0;
                    this.range = 50.0;
                    this.threshold = 0.0;

                    this.originalEnabled = this.enabled;
                    this.originalSpeed = this.speed;
                    this.originalRange = this.range;
                    this.originalThreshold = this.threshold;
                }
            }
            case GENERAL_SOUND_CONFIG -> {
                GeneralSoundsConfig.SoundEntry soundConfig = GeneralSoundsConfig.getSounds().get(cleanElementId);
                if (soundConfig != null) {
                    this.enabled = soundConfig.enabled;
                    this.speed = soundConfig.speed_multiplier;
                    this.range = soundConfig.range_multiplier;
                    this.isPriority = soundConfig.is_priority;

                    this.originalEnabled = soundConfig.enabled;
                    this.originalSpeed = soundConfig.speed_multiplier;
                    this.originalRange = soundConfig.range_multiplier;
                    this.originalIsPriority = soundConfig.is_priority;
                } else {
                    this.enabled = false;
                    this.speed = 1.0;
                    this.range = 1.0;
                    this.isPriority = false;

                    this.originalEnabled = this.enabled;
                    this.originalSpeed = this.speed;
                    this.originalRange = this.range;
                    this.originalIsPriority = this.isPriority;
                }
            }
            case GENERAL_SOUND_ENTITY -> {
                var reactions = GeneralSoundsConfig.getMobReactions();
                GeneralSoundsConfig.Reaction generalReaction = reactions != null ? reactions.get(cleanElementId) : null;
                if (generalReaction != null) {
                    this.enabled = generalReaction.enabled;
                    this.speed = generalReaction.speed;
                    this.range = generalReaction.range;

                    this.originalEnabled = generalReaction.enabled;
                    this.originalSpeed = generalReaction.speed;
                    this.originalRange = generalReaction.range;
                } else {
                    this.enabled = true;
                    this.speed = 1.0;
                    this.range = 50.0;

                    this.originalEnabled = this.enabled;
                    this.originalSpeed = this.speed;
                    this.originalRange = this.range;
                }
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = TOP_MARGIN + 40;

        clearChildren();

        initFields(centerX, startY);
        initActionButtons(centerX, startY);
        updateSaveButtonState();
    }

    private void initFields(int centerX, int startY) {
        int currentY = startY;

        enabledButton = ButtonWidget.builder(
                Text.translatable(enabled ? "button.ezvcsurvival.enabled" : "button.ezvcsurvival.disabled"),
                b -> toggleEnabled()
        ).dimensions(centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT).build();
        this.addDrawableChild(enabledButton);
        currentY += FIELD_SPACING + 20;

        speedBox = new TextFieldWidget(this.textRenderer, centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT, Text.literal(""));
        speedBox.setText(String.valueOf(speed));
        speedBox.setMaxLength(20);
        speedBox.setChangedListener(s -> {
            try {
                speed = Double.parseDouble(s);
                updateSaveButtonState();
            } catch (NumberFormatException ignored) {
            }
        });
        this.addDrawableChild(speedBox);
        currentY += FIELD_SPACING + 20;

        rangeBox = new TextFieldWidget(this.textRenderer, centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT, Text.literal(""));
        rangeBox.setText(String.valueOf(range));
        rangeBox.setMaxLength(20);
        rangeBox.setChangedListener(s -> {
            try {
                range = Double.parseDouble(s);
                updateSaveButtonState();
            } catch (NumberFormatException ignored) {
            }
        });
        this.addDrawableChild(rangeBox);
        currentY += FIELD_SPACING + 20;

        if (editType == EditType.ENTITY_CONFIG) {
            thresholdBox = new TextFieldWidget(this.textRenderer, centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT, Text.literal(""));
            thresholdBox.setText(String.valueOf(threshold));
            thresholdBox.setMaxLength(20);
            thresholdBox.setChangedListener(s -> {
                try {
                    threshold = Double.parseDouble(s);
                    updateSaveButtonState();
                } catch (NumberFormatException ignored) {
                }
            });
            this.addDrawableChild(thresholdBox);
            currentY += FIELD_SPACING + 20;
        }

        if (editType == EditType.GENERAL_SOUND_CONFIG) {
            priorityButton = ButtonWidget.builder(
                    Text.translatable(isPriority ? "button.ezvcsurvival.priority_on" : "button.ezvcsurvival.priority_off"),
                    b -> togglePriority()
            ).dimensions(centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT).build();
            this.addDrawableChild(priorityButton);
            currentY += FIELD_SPACING + 20;
        }

        if (editType == EditType.GENERAL_SOUND_ENTITY) {
            soundFiltersButton = ButtonWidget.builder(
                    Text.literal("Sound Filters..."),
                    b -> MinecraftClient.getInstance().setScreen(new SoundFilterEditScreen(this, elementId, elementName))
            ).dimensions(centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT).build();
            this.addDrawableChild(soundFiltersButton);
        }
    }

    private void initActionButtons(int centerX, int startY) {
        int fieldCount = getFieldCount();
        int buttonY = startY + (fieldCount * FIELD_SPACING) + 30;

        this.saveButton = ButtonWidget.builder(
                Text.translatable("button.ezvcsurvival.save"),
                b -> saveConfig()
        ).dimensions(centerX - CENTER_X_OFFSET - BUTTON_WIDTH - BUTTON_SPACING, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addDrawableChild(this.saveButton);

        ButtonWidget cancelButton = ButtonWidget.builder(
                Text.translatable("button.ezvcsurvival.cancel"),
                b -> MinecraftClient.getInstance().setScreen(parent)
        ).dimensions(centerX + CENTER_X_OFFSET, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addDrawableChild(cancelButton);

        ButtonWidget resetButton = ButtonWidget.builder(
                Text.translatable("button.ezvcsurvival.reset"),
                b -> resetToDefaults()
        ).dimensions(centerX - BUTTON_WIDTH / 2, buttonY + BUTTON_SPACING + BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addDrawableChild(resetButton);
    }

    private int getFieldCount() {
        return switch (editType) {
            case ENTITY_CONFIG, GENERAL_SOUND_CONFIG, GENERAL_SOUND_ENTITY -> 4;
            default -> 3;
        };
    }

    private void toggleEnabled() {
        enabled = !enabled;
        enabledButton.setMessage(Text.translatable(enabled ? "button.ezvcsurvival.enabled" : "button.ezvcsurvival.disabled"));
        updateSaveButtonState();
    }

    private void togglePriority() {
        isPriority = !isPriority;
        priorityButton.setMessage(Text.translatable(isPriority ? "button.ezvcsurvival.priority_on" : "button.ezvcsurvival.priority_off"));
        updateSaveButtonState();
    }

    private void saveConfig() {
        try {
            try {
                speed = Double.parseDouble(speedBox.getText());
                range = Double.parseDouble(rangeBox.getText());
                if (thresholdBox != null) threshold = Double.parseDouble(thresholdBox.getText());
            } catch (NumberFormatException e) {
                showError();
                return;
            }

            if (editType == EditType.ENTITY_CONFIG && thresholdBox != null) {
                threshold = Math.max(-100.0, Math.min(100.0, threshold));
            }

            boolean localSuccess = switch (editType) {
                case ENTITY_CONFIG -> {
                    EntityVoiceConfig.set(elementId, new EntityVoiceConfig.EntityConfig(enabled, speed, range, threshold));
                    EntityVoiceConfig.persist();
                    yield true;
                }
                case GENERAL_SOUND_CONFIG -> {
                    GeneralSoundsConfig.setSoundEntry(elementId, enabled, speed, range, isPriority);
                    GeneralSoundsConfig.persist();

                    GeneralSoundsConfig.SoundEntry updated = GeneralSoundsConfig.getSounds().get(elementId);
                    yield (updated != null &&
                            updated.enabled == enabled &&
                            Math.abs(updated.speed_multiplier - speed) < 0.001 &&
                            Math.abs(updated.range_multiplier - range) < 0.001 &&
                            updated.is_priority == isPriority);
                }
                case GENERAL_SOUND_ENTITY -> {
                    GeneralSoundsConfig.setMobReaction(elementId, enabled, speed, range);
                    GeneralSoundsConfig.persist();
                    yield true;
                }
            };

            if (localSuccess) {
                try {
                   SoundConfig.loadConfigs();
                } catch (Exception e) {
                    // Ignore reload errors
                }

                sendUpdate();
                updateOriginalValues();

                if (parent instanceof ConfigListScreen configList) {
                    configList.onConfigUpdated();
                }

                MinecraftClient.getInstance().setScreen(parent);
            } else {
                showError();
            }

        } catch (Exception e) {
            showError();
        }
    }

    private void resetToDefaults() {
        switch (editType) {
            case ENTITY_CONFIG -> {
                EntityVoiceConfig.EntityConfig defaultConfig = EntityVoiceConfig.EntityConfig.defaultFor(null);
                enabled = defaultConfig.enabled;
                speed = defaultConfig.speed;
                range = defaultConfig.range;
                threshold = defaultConfig.threshold;
                if (thresholdBox != null) thresholdBox.setText(String.valueOf(threshold));
            }
            case GENERAL_SOUND_CONFIG -> {
                enabled = false;
                speed = 1.0;
                range = 1.0;
                isPriority = false;
                if (priorityButton != null) {
                    priorityButton.setMessage(Text.translatable(isPriority ? "button.ezvcsurvival.priority_on" : "button.ezvcsurvival.priority_off"));
                }
            }
            case GENERAL_SOUND_ENTITY -> {
                enabled = true;
                speed = 1.0;
                range = 50.0;
            }
        }

        if (enabledButton != null) {
            enabledButton.setMessage(Text.translatable(enabled ? "button.ezvcsurvival.enabled" : "button.ezvcsurvival.disabled"));
        }
        if (speedBox != null) speedBox.setText(String.valueOf(speed));
        if (rangeBox != null) rangeBox.setText(String.valueOf(range));

        updateSaveButtonState();
    }

    @Override
    public void render(DrawContext graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int titleY = TOP_MARGIN;
        graphics.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, titleY, 0xFFFFFFFF);



        String elementInfo = "ID: " + cleanElementId;
        int elementInfoY = titleY + 15;
        graphics.drawCenteredTextWithShadow(this.textRenderer, elementInfo, this.width / 2, elementInfoY, 0xFFAAAAAA);

        int lineY = elementInfoY + 10;
        int lineWidth = Math.min(180, this.width - 100);
        graphics.fill(this.width / 2 - lineWidth / 2, lineY, this.width / 2 + lineWidth / 2, lineY + 1, 0x44FFFFFF);

        renderFieldLabels(graphics);
    }

    private void renderFieldLabels(DrawContext graphics) {
        int centerX = this.width / 2;
        int startY = TOP_MARGIN + 40;
        int currentY = startY;

        graphics.drawTextWithShadow(this.textRenderer, "Enabled", centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFFFF);
        currentY += FIELD_SPACING + 20;

        graphics.drawTextWithShadow(this.textRenderer, "Speed", centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFFFF);
        currentY += FIELD_SPACING + 20;

        graphics.drawTextWithShadow(this.textRenderer, "Range", centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFFFF);
        currentY += FIELD_SPACING + 20;

        if (editType == EditType.ENTITY_CONFIG) {
            graphics.drawTextWithShadow(this.textRenderer, "Threshold", centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFFFF);
            currentY += FIELD_SPACING + 20;
        }

        if (editType == EditType.GENERAL_SOUND_CONFIG) {
            graphics.drawTextWithShadow(this.textRenderer, "Priority Sound", centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFFFF);
        }

        if (editType == EditType.GENERAL_SOUND_ENTITY) {
            graphics.drawTextWithShadow(this.textRenderer, "Configure Filters", centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC key
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void sendUpdate() {
        try {
            switch (editType) {
                case ENTITY_CONFIG:
                    EZVCNetwork.sendEntityConfigUpdate(
                            elementId,
                            enabled,
                            speed,
                            range,
                            threshold
                    );
                    break;

                case GENERAL_SOUND_CONFIG:
                    EZVCNetwork.sendGeneralSoundConfigUpdate(
                            elementId,
                            enabled,
                            speed,
                            range,
                            isPriority
                    );
                    break;

                case GENERAL_SOUND_ENTITY:
                    EZVCNetwork.sendGeneralSoundEntityUpdate(
                            elementId,
                            enabled,
                            speed,
                            range
                    );
                    break;
            }
        } catch (Exception e) {
            System.err.println("[EZVCSurvival] Error sending configuration: " + e.getMessage());
        }
    }


    private boolean hasChanges() {
        return switch (editType) {
            case ENTITY_CONFIG -> enabled != originalEnabled ||
                    speed != originalSpeed ||
                    range != originalRange ||
                    threshold != originalThreshold;
            case GENERAL_SOUND_CONFIG -> enabled != originalEnabled ||
                    speed != originalSpeed ||
                    range != originalRange ||
                    isPriority != originalIsPriority;
            case GENERAL_SOUND_ENTITY -> enabled != originalEnabled ||
                    speed != originalSpeed ||
                    range != originalRange;
        };
    }

    private void updateSaveButtonState() {
        if (saveButton != null) {
            boolean hasChanges = hasChanges();
            saveButton.active = hasChanges;

            if (hasChanges) {
                saveButton.setMessage(Text.translatable("button.ezvcsurvival.save"));
            } else {
                saveButton.setMessage(Text.translatable("button.ezvcsurvival.no_changes"));
            }
        }
    }

    private void updateOriginalValues() {
        switch (editType) {
            case ENTITY_CONFIG -> {
                originalEnabled = enabled;
                originalSpeed = speed;
                originalRange = range;
                originalThreshold = threshold;
            }
            case GENERAL_SOUND_CONFIG -> {
                originalEnabled = enabled;
                originalSpeed = speed;
                originalRange = range;
                originalIsPriority = isPriority;
            }
            case GENERAL_SOUND_ENTITY -> {
                originalEnabled = enabled;
                originalSpeed = speed;
                originalRange = range;
            }
        }
    }

    private void showError() {
        System.err.println("[EZVCSurvival] Error: Values must be valid numbers.");
    }
}