package io.github.gcjojo.questslib.quests.rewards;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;

public class ItemReward extends QuestReward {
    public ItemReward(JsonObject json) {
        super(json);
    }

    @Override
    public void rewardPlayer(Player player) {

    }
}
