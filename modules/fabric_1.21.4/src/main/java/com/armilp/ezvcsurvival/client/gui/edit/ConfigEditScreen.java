package com.armilp.ezvcsurvival.client.gui.edit;

import net.minecraft.client.gui.EditBox;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfigEditScreen extends Screen {

    public enum EditType {
        ENTITY_CONFIG,
        GENERAL_SOUND_CONFIG,
        GENERAL_SOUND_ENTITY,
        GUNFIRE_SOUND_CONFIG,
        GUNFIRE_ENTITY
    }

    private final Screen parent;
    private final EditType editType;
    private final String elementId;

    private ButtonWidget enabledButton;
    private EditBox speedBox;
    private EditBox rangeBox;
    private EditBox thresholdBox;
    private ButtonWidget priorityButton;
    private ButtonWidget saveButton;

    private boolean enabled;
    private double speed;
    private double range;
    private double threshold;
    private boolean priority;

    private boolean originalEnabled;
    private double originalSpeed;
    private double originalRange;
    private double originalThreshold;
    private boolean originalPriority;

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

        loadCurrentValues();
    }

    private static String getTitleKey(EditType editType) {
        switch (editType) {
            case ENTITY_CONFIG: return "screen.ezvcsurvival.entity_config_edit";
            case GENERAL_SOUND_CONFIG: return "screen.ezvcsurvival.general_sound_config_edit";
            case GENERAL_SOUND_ENTITY: return "screen.ezvcsurvival.general_sound_entity_edit";
            case GUNFIRE_SOUND_CONFIG: return "screen.ezvcsurvival.gunfire_sound_config_edit";
            case GUNFIRE_ENTITY: return "screen.ezvcsurvival.gunfire_entity_edit";
            default: return "screen.ezvcsurvival.config_edit";
        }
    }

    private void loadCurrentValues() {
        switch (editType) {
            case ENTITY_CONFIG:
                EntityVoiceConfig.EntityConfig entityConfig = EntityVoiceConfig.get(elementId);
                if (entityConfig != null) {
                    this.enabled = entityConfig.enabled;
                    this.speed = entityConfig.speed;
                    this.range = entityConfig.range;
                    this.threshold = entityConfig.threshold;

                    this.originalEnabled = entityConfig.enabled;
                    this.originalSpeed = entityConfig.speed;
                    this.originalRange = entityConfig.range;
                    this.originalThreshold = entityConfig.threshold;
                }
                break;

            case GENERAL_SOUND_CONFIG:
                GeneralSoundsConfig.SoundEntry soundConfig = GeneralSoundsConfig.getSounds().get(elementId);
                if (soundConfig != null) {
                    this.enabled = soundConfig.enabled;
                    this.speed = soundConfig.speed_multiplier;
                    this.range = soundConfig.range_multiplier;

                    this.originalEnabled = soundConfig.enabled;
                    this.originalSpeed = soundConfig.speed_multiplier;
                    this.originalRange = soundConfig.range_multiplier;
                }
                break;

            case GENERAL_SOUND_ENTITY:
                java.util.Map<String, GeneralSoundsConfig.Reaction> reactions = GeneralSoundsConfig.getMobReactions();
                GeneralSoundsConfig.Reaction generalReaction = reactions != null ? reactions.get(elementId) : null;
                if (generalReaction != null) {
                    this.enabled = generalReaction.enabled;
                    this.speed = generalReaction.speed;
                    this.range = generalReaction.range;

                    this.originalEnabled = generalReaction.enabled;
                    this.originalSpeed = generalReaction.speed;
                    this.originalRange = generalReaction.range;
                } else {
                    // Valores por defecto por si la configuración aún no existe
                    this.enabled = true;
                    this.speed = 1.0;
                    this.range = 1.0;

                    this.originalEnabled = this.enabled;
                    this.originalSpeed = this.speed;
                    this.originalRange = this.range;
                }
                break;

            case GUNFIRE_SOUND_CONFIG:
                Boolean prioritySound = GunfireConfig.getGunPrioritySounds().get(elementId);
                this.priority = prioritySound != null ? prioritySound : false;

                this.originalPriority = this.priority;
                break;

            case GUNFIRE_ENTITY:
                GunfireConfig.Reaction gunfireReaction = GunfireConfig.getMobReactions().get(elementId);
                if (gunfireReaction != null) {
                    this.enabled = gunfireReaction.enabled;
                    this.speed = gunfireReaction.speed;
                    this.range = gunfireReaction.range;

                    this.originalEnabled = gunfireReaction.enabled;
                    this.originalSpeed = gunfireReaction.speed;
                    this.originalRange = gunfireReaction.range;
                }
                break;
        }
    }

}
