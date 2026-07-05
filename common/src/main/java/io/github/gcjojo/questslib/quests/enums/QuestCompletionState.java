package io.github.gcjojo.questslib.quests.enums;

import io.github.gcjojo.questslib.QuestsLib;
import lombok.Getter;

public enum QuestCompletionState {
    None("none"),
    Started("started"),
    Completed("completed");

    private final @Getter String stateString;

    QuestCompletionState(String stateString) {
        this.stateString = stateString;
    }

    public static QuestCompletionState fromId(String id) {
        for (QuestCompletionState state : values()) {
            if (state.stateString.equals(id)) return state;
        }
        QuestsLib.getLogger().error("StatTask Type Unknow {}", id);
        return None;
    }
}
