package io.github.gcjojo.questslib.quests.tasks;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.JsonObject;
import io.github.gcjojo.liblib.utils.MathUtils;
import io.github.gcjojo.questslib.quests.QuestTask;
import io.github.gcjojo.questslib.quests.enums.TaskType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AllTask extends CompositeTask {
    public AllTask(ResourceLocation taskId, Component taskName, Component taskDescription, Map<QuestTask, Boolean> subtasks) {
        super(taskId, taskName, taskDescription, subtasks);

        this.taskDataClass = AllTaskData.class;
    }

    public AllTask(JsonObject json) {
        super(json);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.All;
    }

    public static class AllTaskData extends CompositeTaskData<AllTask> {

        public AllTaskData(@NotNull AllTask parentTask) {
            super(parentTask);
        }

        @Override
        public float getProgression() {
            AtomicInteger requiredSubtasks = new AtomicInteger(0);
            AtomicDouble completedSubtasks = new AtomicDouble(0.0d);
            task.getSubtasks().forEach((subtask, required) -> {
                if (!required) return;
                requiredSubtasks.set(requiredSubtasks.get() + 1);
                if (subtasksData.containsKey(subtask.getTaskId())) {
                    QuestTaskData<? extends QuestTask> subtaskData = subtasksData.get(subtask.getTaskId());
                    completedSubtasks.set(completedSubtasks.get() + subtaskData.getProgression());
                }
            });

            return MathUtils.clamp((float) completedSubtasks.get() / (float) requiredSubtasks.get(), 0.0f, 1.0f);
        }
    }
}
