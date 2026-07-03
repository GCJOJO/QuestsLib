package io.github.gcjojo.questslib.quests;

import lombok.Getter;
import lombok.NonNull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public abstract class QuestTask {
    protected @Getter ResourceLocation taskId;
    protected @Getter Component taskName;
    protected @Getter Component taskDescription;
    protected @Getter Class<? extends QuestTaskData<? extends QuestTask>> taskDataClass = EmptyQuestTaskData.class;

    public QuestTask(ResourceLocation taskId, Component taskName, Component taskDescription) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskDescription = taskDescription;
    }

    public abstract TaskType getTaskType();

    public QuestTaskData<? extends QuestTask> getNewTaskData() {
        return QuestTaskDataRegistry.create(this);
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

        public abstract CompoundTag saveData();

        public abstract void loadData(CompoundTag data);
    }

    private class EmptyQuestTaskData extends QuestTaskData<QuestTask> {
        public EmptyQuestTaskData(@NotNull QuestTask parentTask) {
            super(parentTask);
        }

        @Override
        public float getProgression() {
            return 0;
        }

        @Override
        public CompoundTag saveData() {
            return null;
        }

        @Override
        public void loadData(CompoundTag data) {

        }
    }
}
