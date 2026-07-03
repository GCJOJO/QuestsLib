package io.github.gcjojo.questslib;

import com.mojang.logging.LogUtils;
import io.github.gcjojo.questslib.events.QuestsEvents;
import io.github.gcjojo.questslib.quests.QuestManager;
import io.github.gcjojo.questslib.quests.QuestTaskDataRegistry;
import io.github.gcjojo.questslib.quests.tasks.StatTask;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public final class Questslib {
    public static final String MOD_ID = "questslib";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        QuestManager.initEvent();
        QuestManager.loadQuests();

        QuestTaskDataRegistry.register(StatTask.class, StatTask.StatTaskData::new);

        QuestsEvents.TASK_PROGRESSION.register((player, questId, taskId) -> {
            float progression = QuestManager.getPlayerProgression(player, questId);
            player.sendSystemMessage(Component.literal(String.format("Progression on task %s is %.2f", taskId, progression)));
        });
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}
