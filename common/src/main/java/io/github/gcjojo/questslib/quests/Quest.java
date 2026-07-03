package io.github.gcjojo.questslib.quests;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Quest {
    protected @Getter ResourceLocation questId;
    protected @Getter Component questName;
    protected @Getter Component questDescription;

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
