package io.github.gcjojo.questslib.quests.tasks;

import com.google.gson.JsonObject;
import io.github.gcjojo.questslib.quests.QuestTask;
import io.github.gcjojo.questslib.quests.enums.TaskType;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class DialogueTask extends QuestTask {
    protected ResourceLocation dialogueId;

    public DialogueTask(JsonObject json) {
        super(json);
        if (json.has("dialogue"))
            this.dialogueId = ResourceLocation.tryParse(json.get("dialogue").getAsString());
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.CompleteDialogue;
    }

    @Getter
    @Setter
    public static class DialogueTaskData extends QuestTaskData<DialogueTask> {
        protected boolean hasReadDialogue = false;

        public DialogueTaskData(@NotNull DialogueTask parentTask) {
            super(parentTask);
        }

        @Override
        public float getProgression() {
            return hasReadDialogue ? 1.0f : 0.0f;
        }

        @Override
        public CompoundTag serialize() {
            CompoundTag nbt = new CompoundTag();
            nbt.putBoolean("HasReadDialogue", hasReadDialogue);
            return nbt;
        }

        @Override
        public void deserialize(CompoundTag nbt) {
            if (nbt.contains("HasReadDialogue"))
                this.hasReadDialogue = nbt.getBoolean("HasReadDialogue");
        }
    }
}
