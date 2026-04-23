package com.armilp.ezvcsurvival.utils;

public enum ConfigType {
    ENTITY_VOICE(0),
    GENERAL_SOUND(1),
    GENERAL_SOUND_ENTITY(4),
    SOUND_PRIORITY(5),
    GUNFIRE_SOUND(6);

    private final int id;

    ConfigType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static ConfigType fromId(int id) {
        for (ConfigType type : values()) {
            if (type.id == id) return type;
        }
        return ENTITY_VOICE;
    }
}