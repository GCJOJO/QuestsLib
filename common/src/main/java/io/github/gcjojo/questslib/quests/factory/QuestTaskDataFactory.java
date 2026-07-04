package io.github.gcjojo.questslib.quests.factory;

import io.github.gcjojo.questslib.quests.QuestTask;

public interface QuestTaskDataFactory<T extends QuestTask> {
    QuestTask.QuestTaskData<T> create(T task);
}
