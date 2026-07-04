package io.github.gcjojo.questslib.quests.tasks;

import io.github.gcjojo.questslib.quests.QuestTask;
import io.github.gcjojo.questslib.quests.enums.TaskType;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
public abstract class CompositeTask extends QuestTask {

    // False means task is optional
    private Map<QuestTask, Boolean> subtasks;

    public CompositeTask(ResourceLocation taskId, Component taskName, Component taskDescription) {
        super(taskId, taskName, taskDescription);
    }

    public abstract TaskType getTaskType();

    public static abstract class CompositeTaskData extends QuestTaskData<CompositeTask> {
        protected Map<ResourceLocation, QuestTaskData<? extends QuestTask>> subtasksData = new HashMap<>();

        public CompositeTaskData(@NotNull CompositeTask parentTask) {
            super(parentTask);
        }

        public Optional<QuestTaskData<? extends QuestTask>> getSubtaskData(ResourceLocation taskId) {
            if (subtasksData.containsKey(taskId)) return Optional.ofNullable(subtasksData.getOrDefault(taskId, null));
            return Optional.empty();
        }

        public void setSubtasksData(ResourceLocation taskId, QuestTaskData<? extends QuestTask> data) {
            subtasksData.put(taskId, data);
        }

        @Override
        public CompoundTag saveData() {
            return null;
        }

        @Override
        public void loadData(CompoundTag data) {

        }
    }
}
