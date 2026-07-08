package io.github.gcjojo.questslib.quests.tasks;

import com.google.gson.JsonObject;
import io.github.gcjojo.questslib.quests.QuestTask;
import io.github.gcjojo.questslib.quests.enums.TaskType;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

// Does nothing by itself, needs a command to change it's state
public class ManualTask extends QuestTask {
    public ManualTask(JsonObject json) {
        super(json);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.Manual;
    }

    public static class ManualTaskData extends QuestTask.QuestTaskData<ManualTask> {
        public ManualTaskData(@NotNull ManualTask parentTask) {
            super(parentTask);
        }

        @Override
        public float getProgression() {
            return 0;
        }

        @Override
        public CompoundTag serialize() {
            return new CompoundTag();
        }

        @Override
        public void deserialize(CompoundTag nbt) {

        }
    }
}
