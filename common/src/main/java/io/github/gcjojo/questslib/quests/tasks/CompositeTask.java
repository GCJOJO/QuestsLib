package io.github.gcjojo.questslib.quests.tasks;

import io.github.gcjojo.questslib.quests.QuestTask;
import io.github.gcjojo.questslib.quests.TaskType;
import io.github.gcjojo.questslib.utils.MathUtils;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public abstract class CompositeTask extends QuestTask {

    // False means task is optional (only works for AllTask)
    private Map<QuestTask, Boolean> subtasks;

    public CompositeTask(ResourceLocation taskId, Component taskName, Component taskDescription) {
        super(taskId, taskName, taskDescription);
    }

    public abstract TaskType getTaskType();

    public static class CompositeTaskData extends QuestTaskData<CompositeTask> {
        private Map<ResourceLocation, QuestTaskData<? extends QuestTask>> subtasksData = new HashMap<>();

        public CompositeTaskData(@NotNull CompositeTask parentTask) {
            super(parentTask);
        }

        @Override
        public float getProgression() {
            AtomicInteger requiredSubtasks = new AtomicInteger(0);
            AtomicInteger completedSubtasks = new AtomicInteger(0);
            task.subtasks.forEach((subtask, required) -> {
                if (!required) return;
                requiredSubtasks.set(requiredSubtasks.get() + 1);
                if (subtasksData.containsKey(subtask.getTaskId())) {
                    QuestTaskData<? extends QuestTask> subtaskData = subtasksData.get(subtask.getTaskId());
                    if (subtaskData.checkProgression()) completedSubtasks.set(completedSubtasks.get() + 1);
                }
            });

            return MathUtils.clamp((float) completedSubtasks.get() / (float) requiredSubtasks.get(), 0.0f, 1.0f);
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
