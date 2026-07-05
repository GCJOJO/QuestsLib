package io.github.gcjojo.questslib.quests.rewards;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;

public abstract class QuestReward {
    public QuestReward(JsonObject json) {
    }

    public abstract void rewardPlayer(Player player);
}
