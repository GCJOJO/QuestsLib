package io.github.gcjojo.questslib.quests.tasks;

import io.github.gcjojo.questslib.quests.QuestTask;
import io.github.gcjojo.questslib.quests.enums.TaskType;
import io.github.gcjojo.questslib.utils.MathUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public class AllTask extends CompositeTask {
    public AllTask(ResourceLocation taskId, Component taskName, Component taskDescription) {
        super(taskId, taskName, taskDescription);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.All;
    }

    public static class AllTaskData extends CompositeTaskData {

        public AllTaskData(@NotNull CompositeTask parentTask) {
            super(parentTask);
        }

        @Override
        public float getProgression() {
            AtomicInteger requiredSubtasks = new AtomicInteger(0);
            AtomicInteger completedSubtasks = new AtomicInteger(0);
            task.getSubtasks().forEach((subtask, required) -> {
                if (!required) return;
                requiredSubtasks.set(requiredSubtasks.get() + 1);
                if (subtasksData.containsKey(subtask.getTaskId())) {
                    QuestTaskData<? extends QuestTask> subtaskData = subtasksData.get(subtask.getTaskId());
                    if (subtaskData.checkProgression()) completedSubtasks.set(completedSubtasks.get() + 1);
                }
            });

            return MathUtils.clamp((float) completedSubtasks.get() / (float) requiredSubtasks.get(), 0.0f, 1.0f);
        }
    }
}
