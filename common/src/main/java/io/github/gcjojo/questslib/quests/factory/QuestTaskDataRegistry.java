package io.github.gcjojo.questslib.quests.factory;

import io.github.gcjojo.questslib.quests.QuestTask;

import java.util.HashMap;
import java.util.Map;

// Merci Claudeslop
public class QuestTaskDataRegistry {
    private static final Map<Class<? extends QuestTask>, QuestTaskDataFactory<?>> FACTORIES = new HashMap<>();

    public static <T extends QuestTask> void register(Class<T> taskClass, QuestTaskDataFactory<T> factory) {
        FACTORIES.put(taskClass, factory);
    }

    @SuppressWarnings("unchecked")
    public static <T extends QuestTask> QuestTask.QuestTaskData<T> create(T task) {
        QuestTaskDataFactory<T> factory = (QuestTaskDataFactory<T>) FACTORIES.get(task.getClass());
        if (factory == null) {
            throw new IllegalStateException("No factory found for " + task.getClass());
        }
        return factory.create(task);
    }
}