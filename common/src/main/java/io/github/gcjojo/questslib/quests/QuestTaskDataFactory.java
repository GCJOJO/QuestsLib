package io.github.gcjojo.questslib.quests;

public interface QuestTaskDataFactory<T extends QuestTask> {
    QuestTask.QuestTaskData<T> create(T task);
}
