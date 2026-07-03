package io.github.gcjojo.questslib.quests.tasks;

import io.github.gcjojo.questslib.quests.QuestTask;
import io.github.gcjojo.questslib.quests.TaskType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class AllTask extends QuestTask {
    public AllTask(ResourceLocation taskId, Component taskName, Component taskDescription) {
        super(taskId, taskName, taskDescription);
    }

    @Override
    public TaskType getTaskType() {
        return null;
    }
}
