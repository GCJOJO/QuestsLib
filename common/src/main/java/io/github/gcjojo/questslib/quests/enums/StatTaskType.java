package io.github.gcjojo.questslib.quests.enums;

import io.github.gcjojo.questslib.Questslib;
import lombok.Getter;

public enum StatTaskType {
    None("none"),
    Item("item"),
    PlacedBlocks("placed_blocks"),
    BrokenBlocks("broken_blocks"),
    KilledMobs("killed_mobs");

    private final @Getter String typeString;

    StatTaskType(String typeString) {
        this.typeString = typeString;
    }

    public static StatTaskType fromId(String id) {
        for (StatTaskType type : values()) {
            if (type.typeString.equals(id)) return type;
        }
        Questslib.getLogger().error("StatTask Type Unknow {}", id);
        return None;
    }
}
