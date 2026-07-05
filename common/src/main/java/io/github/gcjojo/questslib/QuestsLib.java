package io.github.gcjojo.questslib;

import com.mojang.logging.LogUtils;
import io.github.gcjojo.liblib.factory.PlayerDataRegistry;
import io.github.gcjojo.questslib.events.QuestsEvents;
import io.github.gcjojo.questslib.quests.QuestLoader;
import io.github.gcjojo.questslib.quests.QuestManager;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.util.Arrays;

public final class QuestsLib {
    public static final String MOD_ID = "questslib";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void printException(String message, Throwable e) {
        QuestsLib.getLogger().error("{}\nError : {}", message, e.toString());
        Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> QuestsLib.getLogger().error(stackTraceElement.toString()));
    }

    public static void init() {
        QuestManager.initEvent();
        PlayerDataRegistry.register(QuestPlayerSaveData.class, QuestPlayerSaveData::new);

        QuestLoader.registerDefaultTaskClasses();
        QuestLoader.registerDefaultRewards();

        QuestsEvents.TASK_PROGRESSION.register((player, questId, taskId) -> {
            float progression = QuestManager.getPlayerProgression(player, questId);
            player.sendSystemMessage(Component.literal(String.format("Progression on task %s is %.2f", taskId, progression)));
        });
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}
