package io.github.gcjojo.questslib.api;

import io.github.gcjojo.liblib.api.QuestsLibAPI;
import io.github.gcjojo.questslib.quests.QuestManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class QuestsLibAPIImpl extends QuestsLibAPI {
    @Override
    public void startQuest(Player player, ResourceLocation questId) {
        QuestManager.startQuest(player, questId);
    }
}
