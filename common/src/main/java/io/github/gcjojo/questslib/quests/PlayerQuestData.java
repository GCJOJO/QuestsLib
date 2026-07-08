package io.github.gcjojo.questslib.quests;

import io.github.gcjojo.questslib.quests.enums.QuestCompletionState;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class PlayerQuestData {
    public static final FriendlyByteBuf.Reader<PlayerQuestData> READER = buf -> {
        ResourceLocation questId = buf.readResourceLocation();
        PlayerQuestData questData = new PlayerQuestData(questId);
        questData.completionState = QuestCompletionState.fromId(buf.readUtf());
        questData.currentTaskIndex = buf.readInt();
        questData.getCurrentTask().ifPresent(task -> {
            questData.currentTaskData = task.getNewTaskData();
            questData.currentTaskData.deserialize(buf.readNbt());
        });

        return questData;
    };
    public static final FriendlyByteBuf.Writer<PlayerQuestData> WRITER = (buf, questData) -> {
        buf.writeResourceLocation(questData.getQuestId());
        buf.writeUtf(questData.getCompletionState().getStateString());
        buf.writeInt(questData.getCurrentTaskIndex());
        buf.writeNbt(questData.currentTaskData.serialize());
    };

    private final ResourceLocation questId;
    private @Setter QuestCompletionState completionState;
    private @Setter int currentTaskIndex;
    private QuestTask.QuestTaskData<? extends QuestTask> currentTaskData;

    public PlayerQuestData(ResourceLocation questId) {
        this.questId = questId;
        this.completionState = QuestCompletionState.None;
        this.currentTaskIndex = 0;
        QuestsManager.getQuest(this.questId).flatMap(quest -> quest.getTask(currentTaskIndex)).ifPresent(task -> currentTaskData = task.getNewTaskData());
    }

    public static PlayerQuestData deserialize(ResourceLocation questId, CompoundTag nbt) {
        PlayerQuestData newData = new PlayerQuestData(questId);
        if (nbt.contains("State"))
            newData.completionState = QuestCompletionState.fromId(nbt.getString("State"));
        if (nbt.contains("CurrentTask"))
            newData.currentTaskIndex = nbt.getInt("CurrentTask");
        if (nbt.contains("TaskData")) {
            QuestsManager.getQuest(newData.questId).flatMap(quest -> quest.getTask(newData.currentTaskIndex)).ifPresent(questTask -> {
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
        Quest quest = QuestsManager.getQuest(questId).orElse(null);
        if (quest == null) return;

        if (currentTaskIndex + 1 >= quest.getTaskAmount()) {
            completionState = QuestCompletionState.Completed;
            return;
        }

        currentTaskIndex++;
        quest.getTask(currentTaskIndex).ifPresent(newTask -> currentTaskData = newTask.getNewTaskData());
    }

    public Optional<QuestTask> getCurrentTask() {
        Quest quest = QuestsManager.getQuest(questId).orElse(null);

        if (quest == null || currentTaskIndex >= quest.getTaskAmount()) return Optional.empty();
        return quest.getTask(currentTaskIndex);
    }

    public void setCurrentTask(ResourceLocation taskId) {
        AtomicInteger newTaskIndex = new AtomicInteger(-1);
        QuestsManager.getQuest(questId).ifPresent(quest -> {
            newTaskIndex.set(quest.getTaskIndex(taskId));
            if (newTaskIndex.get() >= 0) {
                currentTaskIndex = newTaskIndex.get();
                quest.getTask(currentTaskIndex).ifPresent(newTask -> currentTaskData = newTask.getNewTaskData());
            }
        });
    }

    public CompoundTag serialize() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("State", completionState.getStateString());
        nbt.putInt("CurrentTask", currentTaskIndex);
        nbt.put("TaskData", currentTaskData.serialize());
        return nbt;
    }
}
