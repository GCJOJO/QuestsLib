package io.github.gcjojo.questslib.quests.factory;

import com.google.gson.JsonObject;
import io.github.gcjojo.questslib.quests.rewards.QuestReward;

public interface QuestRewardFactory<T extends QuestReward> {
    T create(JsonObject json);
}
