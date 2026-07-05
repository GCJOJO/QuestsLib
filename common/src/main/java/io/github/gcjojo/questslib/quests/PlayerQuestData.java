package io.github.gcjojo.questslib.quests;

import io.github.gcjojo.questslib.Questslib;
import io.github.gcjojo.questslib.quests.enums.QuestCompletionState;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

@Getter
public class PlayerQuestData {
    private final ResourceLocation questId;
    private @Setter QuestCompletionState completionState;
    private int currentTaskId;
    private QuestTask.QuestTaskData<? extends QuestTask> currentTaskData;

    public PlayerQuestData(ResourceLocation questId) {
        this.questId = questId;
        this.completionState = QuestCompletionState.Started;
        this.currentTaskId = 0;
        Quest quest = QuestManager.getQuest(this.questId).orElse(null);
        QuestTask task;
        if (quest != null && (task = quest.getTask(currentTaskId)) != null)
            currentTaskData = task.getNewTaskData();
        Questslib.getLogger().warn("Feur !");
    }

    public static PlayerQuestData deserialize(ResourceLocation questId, CompoundTag nbt) {
        PlayerQuestData newData = new PlayerQuestData(questId);
        if (nbt.contains("State"))
            newData.completionState = QuestCompletionState.fromId(nbt.getString("State"));
        if (nbt.contains("CurrentTask"))
            newData.currentTaskId = nbt.getInt("CurrentTask");
        if (nbt.contains("TaskData")) {
            QuestManager.getTask(newData.questId, newData.currentTaskId).ifPresent(questTask -> {
                newData.currentTaskData = questTask.getNewTaskData();
                newData.currentTaskData.deserialize(nbt.getCompound("TaskData"));
            });
        }

        return newData;
    }

    public boolean checkTaskProgression() {
        if (currentTaskData != null)
            return currentTaskData.checkProgression();
        return false;
    }

    public void nextTask() {
        Quest quest = QuestManager.getQuest(questId).orElse(null);
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
        Quest quest = QuestManager.getQuest(questId).orElse(null);

        if (quest == null || currentTaskId >= quest.getTaskAmount()) return Optional.empty();
        return Optional.ofNullable(quest.getTask(currentTaskId));
    }

    public CompoundTag serialize() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("State", completionState.getStateString());
        nbt.putInt("CurrentTask", currentTaskId);
        nbt.put("TaskData", currentTaskData.serialize());
        return nbt;
    }
}
