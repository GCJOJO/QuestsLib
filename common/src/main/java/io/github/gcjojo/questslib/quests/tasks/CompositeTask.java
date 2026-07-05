package io.github.gcjojo.questslib.quests.tasks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.gcjojo.questslib.quests.QuestLoader;
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
import java.util.concurrent.atomic.AtomicReference;

@Getter
public abstract class CompositeTask extends QuestTask {

    // False means subtask is optional
    private Map<QuestTask, Boolean> subtasks = new HashMap<>();

    public CompositeTask(ResourceLocation taskId, Component taskName, Component taskDescription, Map<QuestTask, Boolean> subtasks) {
        super(taskId, taskName, taskDescription);

        this.subtasks = subtasks;

        //this.taskDataClass = CompositeTaskData.class;
    }

    public CompositeTask(JsonObject json) {
        super(json);

        if (json.has("subtasks")) {
            JsonArray subtaskArray = json.getAsJsonArray("subtasks");
            if (subtaskArray != null) {
                subtaskArray.forEach(subtaskJson -> {
                    boolean required;
                    JsonObject subtaskObject = subtaskJson.getAsJsonObject();
                    if (subtaskObject.has("required"))
                        required = subtaskObject.get("required").getAsBoolean();
                    else
                        required = true;

                    QuestLoader.constructTask(subtaskJson.getAsJsonObject()).ifPresent(subtask -> subtasks.putIfAbsent(subtask, required));
                });
            }
        }

        //this.taskDataClass = CompositeTaskData.class;
    }

    public Map.Entry<Optional<QuestTask>, Boolean> getSubtask(ResourceLocation subtaskId) {
        AtomicReference<Optional<QuestTask>> task = new AtomicReference<>(Optional.empty());
        AtomicReference<Boolean> subtaskRequired = new AtomicReference<>(true);
        subtasks.forEach((subtask, required) ->
        {
            if (subtaskId.equals(subtask.getTaskId())) {
                task.set(Optional.ofNullable(subtask));
                subtaskRequired.set(required);
            }
        });

        return Map.entry(task.get(), subtaskRequired.get());
    }

    public abstract TaskType getTaskType();

    public static abstract class CompositeTaskData<T extends CompositeTask> extends QuestTaskData<T> {
        protected Map<ResourceLocation, QuestTaskData<? extends QuestTask>> subtasksData = new HashMap<>();

        public CompositeTaskData(@NotNull T parentTask) {
            super(parentTask);
            parentTask.getSubtasks().forEach((subtask, required) -> subtasksData.putIfAbsent(subtask.getTaskId(), subtask.getNewTaskData()));
        }

        public Optional<QuestTaskData<? extends QuestTask>> getSubtaskData(ResourceLocation taskId) {
            if (subtasksData.containsKey(taskId)) return Optional.ofNullable(subtasksData.getOrDefault(taskId, null));
            return Optional.empty();
        }

        public void setSubtasksData(ResourceLocation taskId, QuestTaskData<? extends QuestTask> data) {
            subtasksData.put(taskId, data);
        }

        @Override
        public CompoundTag serialize() {
            CompoundTag nbt = new CompoundTag();
            subtasksData.forEach((subtaskId, subtaskData) -> nbt.put(subtaskId.toString(), subtaskData.serialize()));
            return nbt;
        }

        @Override
        public void deserialize(CompoundTag nbt) {
            nbt.getAllKeys().forEach(subtaskIdString -> {
                ResourceLocation subtaskId = ResourceLocation.tryParse(subtaskIdString);
                CompoundTag subtaskNbt = nbt.getCompound(subtaskIdString);
                task.getSubtask(subtaskId).getKey().ifPresent(subtask -> {
                    QuestTaskData<? extends QuestTask> subtaskData = subtask.getNewTaskData();
                    subtaskData.deserialize(subtaskNbt);
                    subtasksData.putIfAbsent(subtaskId, subtaskData);
                });
            });
        }
    }
}
