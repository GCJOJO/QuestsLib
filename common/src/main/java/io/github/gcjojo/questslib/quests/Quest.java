package io.github.gcjojo.questslib.quests;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
public class Quest {
    protected ResourceLocation questId;
    protected Component questName;
    protected Component questDescription;

    protected List<QuestTask> tasks;

    public Quest(ResourceLocation questId, Component questName, Component questDescription, List<QuestTask> tasks) {
        this.questId = questId;
        this.questName = questName;
        this.questDescription = questDescription;
        this.tasks = tasks;
    }

    @Nullable
    public QuestTask getTask(int id) {
        if (id >= tasks.size())
            return null;
        return tasks.get(id);
    }

    public int getTaskAmount() {
        return tasks.size();
    }
}
