package io.github.gcjojo.questslib.quests.tasks;

import com.google.gson.JsonObject;
import io.github.gcjojo.liblib.utils.MathUtils;
import io.github.gcjojo.questslib.quests.QuestTask;
import io.github.gcjojo.questslib.quests.enums.TaskType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AnyTask extends CompositeTask {
    public AnyTask(ResourceLocation taskId, Component taskName, Component taskDescription, Map<QuestTask, Boolean> subtasks) {
        super(taskId, taskName, taskDescription, subtasks);

        this.taskDataClass = AnyTaskData.class;
    }

    public AnyTask(JsonObject json) {
        super(json);
    }

    @Override
    public TaskType getTaskType() {
        return null;
    }

    public static class AnyTaskData extends CompositeTaskData<AnyTask> {
        public AnyTaskData(@NotNull AnyTask parentTask) {
            super(parentTask);
        }

        @Override
        public float getProgression() {
            AtomicInteger completedSubtasks = new AtomicInteger(0);
            task.getSubtasks().forEach((subtask, required) -> {
                if (!required) return;
                if (subtasksData.containsKey(subtask.getTaskId())) {
                    QuestTaskData<? extends QuestTask> subtaskData = subtasksData.get(subtask.getTaskId());
                    if (subtaskData.checkProgression()) completedSubtasks.set(completedSubtasks.get() + 1);
                }
            });

            return MathUtils.clamp(completedSubtasks.get(), 0.0f, 1.0f);
        }
    }
}
