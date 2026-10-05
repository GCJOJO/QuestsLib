package io.github.gcjojo.questslib.quests.rewards;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;

public class ExperienceReward extends QuestReward {
    protected int amount = 0;
    protected RewardType type;

    public ExperienceReward(JsonObject json) {
        super(json);

        type = RewardType.NONE;

        if (json.has(RewardType.POINTS.typeString)) {
            amount = json.get(RewardType.POINTS.typeString).getAsInt();
            type = RewardType.POINTS;
        } else if (json.has(RewardType.LEVELS.typeString)) {
            amount = json.get(RewardType.LEVELS.typeString).getAsInt();
            type = RewardType.LEVELS;
        }
    }

    @Override
    public void rewardPlayer(Player player) {
        switch (type) {
            case LEVELS -> player.giveExperienceLevels(amount);
            case POINTS -> player.giveExperiencePoints(amount);
        }
    }

    public enum RewardType {
        NONE("none"),
        POINTS("points"),
        LEVELS("levels");

        public final String typeString;

        RewardType(String str) {
            typeString = str;
        }
    }
}
