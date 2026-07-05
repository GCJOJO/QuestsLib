package io.github.gcjojo.questslib.quests;

import io.github.gcjojo.questslib.quests.rewards.QuestReward;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@Getter
public class Quest {
    protected ResourceLocation questId;
    protected Component questName;
    protected Component questDescription;

    protected List<QuestTask> tasks;
    protected List<QuestReward> rewards;

    public Quest(ResourceLocation questId, Component questName, Component questDescription, List<QuestTask> tasks, List<QuestReward> rewards) {
        this.questId = questId;
        this.questName = questName;
        this.questDescription = questDescription;
        this.tasks = tasks;
        this.rewards = rewards;
    }

    @Nullable
    public QuestTask getTask(int number) {
        if (number >= tasks.size())
            return null;
        return tasks.get(number);
    }

    public Optional<QuestTask> getTask(ResourceLocation id) {
        return tasks.stream().filter(task -> task.getTaskId().equals(id)).findFirst();
    }

    public int getTaskAmount() {
        return tasks.size();
    }
}
