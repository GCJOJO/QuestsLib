package io.github.gcjojo.questslib.quests.enums;

import io.github.gcjojo.questslib.Questslib;
import lombok.Getter;

public enum LocationTaskType {
    None("none"),
    Biome("biome"),
    Structure("structure");

    private final @Getter String typeString;

    LocationTaskType(String typeString) {
        this.typeString = typeString;
    }

    public static LocationTaskType fromId(String id) {
        for (LocationTaskType type : values()) {
            if (type.typeString.equals(id)) return type;
        }
        Questslib.getLogger().error("Location Task Type Unknow {}", id);
        return None;
    }

}
