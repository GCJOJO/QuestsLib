package io.github.gcjojo.questslib.quests;

import io.github.gcjojo.questslib.quests.rewards.QuestReward;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;

@Getter
public class Quest {
    protected ResourceLocation questId;
    protected Component questName;
    protected Component questDescription;

    protected Optional<? extends QuestTask> trigger;
    protected List<QuestTask> tasks;
    protected List<QuestReward> rewards;

    public Quest(ResourceLocation questId, Component questName, Component questDescription, Optional<? extends QuestTask> trigger, List<QuestTask> tasks, List<QuestReward> rewards) {
        this.questId = questId;
        this.questName = questName;
        this.questDescription = questDescription;

        this.trigger = trigger;
        this.tasks = tasks;
        this.rewards = rewards;
    }

    public Optional<QuestTask> getTask(int index) {
        if (index >= tasks.size())
            return Optional.empty();
        return Optional.ofNullable(tasks.get(index));
    }

    public Optional<QuestTask> getTask(ResourceLocation id) {
        return tasks.stream().filter(task -> task.getTaskId().equals(id)).findFirst();
    }

    public int getTaskIndex(ResourceLocation id) {
        for (int taskIndex = 0; taskIndex <= getTaskAmount() - 1; taskIndex++) {
            Optional<QuestTask> taskOpt = getTask(taskIndex);
            if (taskOpt.isPresent() && taskOpt.get().taskId.equals(id))
                return taskIndex;
        }
        return -1;
    }

    public int getTaskAmount() {
        return tasks.size();
    }

    public void rewardPlayer(Player player) {
        rewards.forEach(reward -> reward.rewardPlayer(player));
    }
}
