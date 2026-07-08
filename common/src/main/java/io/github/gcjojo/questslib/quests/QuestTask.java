package io.github.gcjojo.questslib.quests;

import com.google.gson.JsonObject;
import io.github.gcjojo.questslib.quests.enums.TaskType;
import io.github.gcjojo.questslib.quests.factory.QuestTaskDataRegistry;
import io.github.gcjojo.questslib.quests.rewards.QuestReward;
import lombok.Getter;
import lombok.NonNull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class QuestTask {
    protected ResourceLocation taskId;
    protected Component taskName;
    protected Component taskDescription;
    protected List<QuestReward> rewards = new ArrayList<>();
    protected Class<? extends QuestTaskData<? extends QuestTask>> taskDataClass = EmptyQuestTaskData.class;

    public QuestTask(ResourceLocation taskId, Component taskName, Component taskDescription) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskDescription = taskDescription;
    }

    public QuestTask(JsonObject json) {
        if (json.has("id"))
            this.taskId = ResourceLocation.tryParse(json.get("id").getAsString());

        if (json.has("name"))
            this.taskName = Component.translatable(json.get("name").getAsString());
        else
            this.taskName = Component.empty();

        if (json.has("description"))
            this.taskDescription = Component.translatable(json.get("description").getAsString());
        else
            this.taskDescription = Component.empty();

        if (json.has("rewards")) {
            json.get("rewards").getAsJsonArray().forEach(rewardJson -> {
                JsonObject rewardData = rewardJson.getAsJsonObject();
                if (!rewardData.has("reward")) return;

                String rewardIdString = rewardData.get("reward").getAsString();
                ResourceLocation rewardId = ResourceLocation.tryParse(rewardIdString);
                QuestLoader.constructReward(rewardId, rewardData).ifPresent(rewards::add);
            });
        }
    }

    public abstract TaskType getTaskType();

    public QuestTaskData<? extends QuestTask> getNewTaskData() {
        return QuestTaskDataRegistry.create(this);
    }

    public void rewardPlayer(Player player) {
        rewards.forEach(reward -> reward.rewardPlayer(player));
    }

    public static abstract class QuestTaskData<T extends QuestTask> {
        @NonNull
        protected T task;

        public QuestTaskData(@NotNull T parentTask) {
            this.task = parentTask;
        }

        public boolean checkProgression() {
            return getProgression() >= 1.0f;
        }

        public abstract float getProgression();

        public abstract CompoundTag serialize();

        public abstract void deserialize(CompoundTag nbt);
    }

    private static class EmptyQuestTaskData extends QuestTaskData<QuestTask> {
        public EmptyQuestTaskData(@NotNull QuestTask parentTask) {
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
