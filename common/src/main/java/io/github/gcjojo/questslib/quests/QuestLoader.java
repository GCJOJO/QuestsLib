package io.github.gcjojo.questslib.quests;

import com.google.gson.*;
import io.github.gcjojo.questslib.QuestsLib;
import io.github.gcjojo.questslib.quests.factory.QuestRewardFactory;
import io.github.gcjojo.questslib.quests.factory.QuestTaskDataRegistry;
import io.github.gcjojo.questslib.quests.rewards.ItemReward;
import io.github.gcjojo.questslib.quests.rewards.QuestReward;
import io.github.gcjojo.questslib.quests.tasks.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class QuestLoader {

    private static final Map<ResourceLocation, Class<? extends QuestTask>> questTaskClasses = new HashMap<>();
    private static final Map<ResourceLocation, QuestRewardFactory<? extends QuestReward>> questRewardFactories = new HashMap<>();

    public static void registerDefaultTaskClasses() {
        registerTaskClass(ResourceLocation.tryBuild(QuestsLib.MOD_ID, "stat"), StatTask.class);
        registerTaskClass(ResourceLocation.tryBuild(QuestsLib.MOD_ID, "location"), LocationTask.class);
        registerTaskClass(ResourceLocation.tryBuild(QuestsLib.MOD_ID, "any"), AnyTask.class);
        registerTaskClass(ResourceLocation.tryBuild(QuestsLib.MOD_ID, "all"), AllTask.class);
        registerTaskClass(ResourceLocation.tryBuild(QuestsLib.MOD_ID, "manual"), ManualTask.class);
        registerTaskClass(ResourceLocation.tryBuild(QuestsLib.MOD_ID, "dialogue"), DialogueTask.class);


        QuestTaskDataRegistry.register(StatTask.class, StatTask.StatTaskData::new);
        QuestTaskDataRegistry.register(LocationTask.class, LocationTask.LocationTaskData::new);
        QuestTaskDataRegistry.register(AnyTask.class, AnyTask.AnyTaskData::new);
        QuestTaskDataRegistry.register(AllTask.class, AllTask.AllTaskData::new);
        QuestTaskDataRegistry.register(ManualTask.class, ManualTask.ManualTaskData::new);
        QuestTaskDataRegistry.register(DialogueTask.class, DialogueTask.DialogueTaskData::new);
    }

    public static void registerDefaultRewards() {
        registerReward(ResourceLocation.tryBuild(QuestsLib.MOD_ID, "item"), ItemReward::new);
    }

    public static <T extends QuestTask> void registerTaskClass(ResourceLocation taskName, Class<? extends QuestTask> taskClass) {
        questTaskClasses.putIfAbsent(taskName, taskClass);
    }

    public static <T extends QuestReward> void registerReward(ResourceLocation rewardId, QuestRewardFactory<? extends QuestReward> factory) {
        questRewardFactories.putIfAbsent(rewardId, factory);
    }

    public static Map<ResourceLocation, Quest> loadQuestFile(MinecraftServer server, String namespace) {
        Map<ResourceLocation, Quest> quests = new HashMap<>();
        ResourceLocation questFileLocation = ResourceLocation.tryBuild(namespace, "quests.json");
        server.getResourceManager().getResource(questFileLocation).ifPresent(questFile -> {
            try {
                JsonArray json = new Gson().fromJson(new InputStreamReader(questFile.open()), JsonArray.class);
                json.forEach(questJson -> loadQuest(questJson.getAsJsonObject()).ifPresent(quest -> quests.put(quest.questId, quest)));
            } catch (IOException | JsonSyntaxException | JsonIOException e) {
                if (questFileLocation != null)
                    QuestsLib.printException(String.format("Unable to parse quest file %s", questFileLocation.toString()), e);
                else
                    QuestsLib.printException("Unable to parse quest file", e);
            }
        });

        return quests;
    }

    public static Optional<Quest> loadQuest(JsonObject json) {
        ResourceLocation questId;
        if (!json.has("id") || (questId = ResourceLocation.tryParse(json.get("id").getAsString())) == null) {
            QuestsLib.getLogger().error("No id provided for quest !");
            return Optional.empty();
        }
        Component questName;
        if (json.has("name"))
            questName = Component.translatable(json.get("name").getAsString());
        else
            questName = Component.empty();

        Component questDescription;
        if (json.has("description"))
            questDescription = Component.translatable(json.get("description").getAsString());
        else
            questDescription = Component.empty();

        Optional<? extends QuestTask> trigger = Optional.empty();
        if (json.has("trigger")) {
            JsonObject triggerJson = json.getAsJsonObject("trigger");
            trigger = constructTask(triggerJson);
        }

        List<QuestTask> tasks = new ArrayList<>();

        if (json.has("tasks")) {
            json.get("tasks").getAsJsonArray().forEach(element -> {
                JsonObject obj = element.getAsJsonObject();
                constructTask(obj).ifPresent(tasks::add);
            });
        }

        List<QuestReward> rewards = new ArrayList<>();

        if (json.has("rewards")) {
            json.get("rewards").getAsJsonArray().forEach(rewardJson -> {
                JsonObject rewardData = rewardJson.getAsJsonObject();
                if (!rewardData.has("reward")) return;

                String rewardIdString = rewardData.get("reward").getAsString();
                ResourceLocation rewardId = ResourceLocation.tryParse(rewardIdString);
                constructReward(rewardId, rewardData).ifPresent(rewards::add);
            });
        }

        return Optional.of(new Quest(questId, questName, questDescription, trigger, tasks, rewards));
    }

    public static Optional<QuestTask> constructTask(JsonObject json) {
        String taskType;
        if (!json.has("task") || (taskType = json.get("task").getAsString()) == null) {
            QuestsLib.getLogger().error("No Task Type provided.");
            return Optional.empty();
        }

        ResourceLocation taskTypeId = ResourceLocation.tryParse(taskType);
        if (!questTaskClasses.containsKey(taskTypeId)) {
            QuestsLib.getLogger().error("Invalid Task Type {}.", taskType);
            return Optional.empty();
        }

        Class<? extends QuestTask> taskClass = questTaskClasses.get(taskTypeId);
        try {
            return Optional.of(taskClass.getConstructor(JsonObject.class).newInstance(json));
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            QuestsLib.printException(String.format("Unable to create task %s.", taskType), e);
            return Optional.empty();
        }
    }

    public static Optional<QuestReward> constructReward(ResourceLocation rewardId, JsonObject json) {
        if (!questRewardFactories.containsKey(rewardId)) return Optional.empty();
        return Optional.ofNullable(questRewardFactories.get(rewardId).create(json));
    }
}
