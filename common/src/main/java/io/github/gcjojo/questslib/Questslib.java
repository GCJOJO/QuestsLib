package io.github.gcjojo.questslib;

import com.mojang.logging.LogUtils;
import io.github.gcjojo.factory.PlayerDataRegistry;
import io.github.gcjojo.questslib.events.QuestsEvents;
import io.github.gcjojo.questslib.quests.QuestLoader;
import io.github.gcjojo.questslib.quests.QuestManager;
import io.github.gcjojo.questslib.quests.factory.QuestTaskDataRegistry;
import io.github.gcjojo.questslib.quests.tasks.AllTask;
import io.github.gcjojo.questslib.quests.tasks.AnyTask;
import io.github.gcjojo.questslib.quests.tasks.StatTask;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.util.Arrays;

public final class Questslib {
    public static final String MOD_ID = "questslib";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void printException(String message, Throwable e) {
        Questslib.getLogger().error("{}\nError : {}", message, e.toString());
        Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> Questslib.getLogger().error(stackTraceElement.toString()));
    }

    public static void init() {
        QuestManager.initEvent();
        PlayerDataRegistry.register(QuestPlayerSaveData.class, QuestPlayerSaveData::new);

        QuestLoader.registerDefaultTaskClasses();
        QuestTaskDataRegistry.register(StatTask.class, StatTask.StatTaskData::new);
        QuestTaskDataRegistry.register(AnyTask.class, AnyTask.AnyTaskData::new);
        QuestTaskDataRegistry.register(AllTask.class, AllTask.AllTaskData::new);

        QuestsEvents.TASK_PROGRESSION.register((player, questId, taskId) -> {
            float progression = QuestManager.getPlayerProgression(player, questId);
            player.sendSystemMessage(Component.literal(String.format("Progression on task %s is %.2f", taskId, progression)));
        });
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}
