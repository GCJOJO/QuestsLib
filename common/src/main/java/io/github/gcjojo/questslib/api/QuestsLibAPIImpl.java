package io.github.gcjojo.questslib.api;

import io.github.gcjojo.liblib.api.QuestsLibAPI;
import io.github.gcjojo.questslib.events.QuestsEvents;
import io.github.gcjojo.questslib.quests.GameEventsListener;
import io.github.gcjojo.questslib.quests.PlayerQuestData;
import io.github.gcjojo.questslib.quests.QuestsManager;
import io.github.gcjojo.questslib.quests.enums.QuestCompletionState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class QuestsLibAPIImpl extends QuestsLibAPI {
    @Override
    public boolean startQuest(Player player, ResourceLocation questId) {
        QuestsManager.startQuest(player, questId);
        Optional<PlayerQuestData> playerQuestData = QuestsManager.getPlayerQuestData(player, questId);
        return playerQuestData.isPresent() && playerQuestData.get().getCompletionState() == QuestCompletionState.Started;
    }

    @Override
    public boolean setQuestCompletionState(Player player, ResourceLocation questId, String newState) {
        QuestCompletionState state = QuestCompletionState.fromId(newState);

        AtomicBoolean hasSetState = new AtomicBoolean(false);
        QuestsManager.getPlayerQuestData(player, questId).ifPresent(questData -> {
            questData.setCompletionState(state);
            hasSetState.set(true);
            if (state == QuestCompletionState.Completed)
                QuestsEvents.QUEST_COMPLETED.invoker().questCompleted(player, questId);
        });

        return hasSetState.get();
    }

    @Override
    public boolean setTaskCompletionState(Player player, ResourceLocation questId, ResourceLocation taskId, String newState) {
        QuestCompletionState state = QuestCompletionState.fromId(newState);


        AtomicBoolean hasSetState = new AtomicBoolean(false);
        QuestsManager.getPlayerQuestData(player, questId).ifPresent(questData -> {
            questData.setCurrentTask(taskId);
            questData.setCompletionState(state);
            hasSetState.set(true);
            GameEventsListener.onTaskUpdate(player, questId, taskId, questData);
        });

        return hasSetState.get();
    }

    @Override
    public boolean isQuestCompleted(Player player, ResourceLocation questId) {
        AtomicBoolean questCompleted = new AtomicBoolean(false);
        QuestsManager.getPlayerQuestData(player, questId).ifPresent(questData ->
                questCompleted.set(questData.getCompletionState() == QuestCompletionState.Completed));
        return questCompleted.get();
    }

    @Override
    public float getQuestCompletion(Player player, ResourceLocation questId) {
        return QuestsManager.getPlayerProgression(player, questId);
    }

    @Override
    public Optional<ResourceLocation> getCurrentTask(Player player, ResourceLocation questId) {
        AtomicReference<Optional<ResourceLocation>> taskId = new AtomicReference<>(Optional.empty());
        QuestsManager.getPlayerQuestData(player, questId)
                .flatMap(PlayerQuestData::getCurrentTask)
                .ifPresent(task -> taskId.set(Optional.of(task.getTaskId())));
        return taskId.get();
    }
}
