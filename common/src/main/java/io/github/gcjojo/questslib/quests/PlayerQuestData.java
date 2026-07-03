package io.github.gcjojo.questslib.quests;

import io.github.gcjojo.questslib.Questslib;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public class PlayerQuestData {
    private @Getter ResourceLocation questId;
    private @Getter QuestCompletionState completionState;
    private @Getter int currentTaskId;
    private @Getter QuestTask.QuestTaskData<? extends QuestTask> currentTaskData;

    public PlayerQuestData(ResourceLocation questId) {
        this.questId = questId;
        this.completionState = QuestCompletionState.Started;
        this.currentTaskId = 0;
        Quest quest = QuestManager.getQuest(this.questId);
        QuestTask task;
        if (quest != null && (task = quest.getTask(currentTaskId)) != null)
            currentTaskData = task.getNewTaskData();
        Questslib.getLogger().warn("Feur !");
    }

    public boolean checkTaskProgression() {
        if (currentTaskData != null)
            return currentTaskData.checkProgression();
        return false;
    }

    public void nextTask() {
        Quest quest = QuestManager.getQuest(questId);
        if (quest == null) return;

        if (++currentTaskId >= quest.getTaskAmount()) {
            completionState = QuestCompletionState.Completed;
            return;
        }

        QuestTask newTask = quest.getTask(currentTaskId);
        assert newTask != null;
        currentTaskData = newTask.getNewTaskData();
    }

    public Optional<QuestTask> getCurrentTask() {
        Quest quest = QuestManager.getQuest(questId);

        if (quest == null || currentTaskId >= quest.getTaskAmount()) return Optional.empty();
        return Optional.ofNullable(quest.getTask(currentTaskId));
    }
}
