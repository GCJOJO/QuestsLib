package io.github.gcjojo.questslib.quests;

import com.google.gson.*;
import io.github.gcjojo.questslib.Questslib;
import io.github.gcjojo.questslib.quests.factory.QuestRewardFactory;
import io.github.gcjojo.questslib.quests.factory.QuestTaskDataRegistry;
import io.github.gcjojo.questslib.quests.rewards.ItemReward;
import io.github.gcjojo.questslib.quests.rewards.QuestReward;
import io.github.gcjojo.questslib.quests.tasks.AllTask;
import io.github.gcjojo.questslib.quests.tasks.AnyTask;
import io.github.gcjojo.questslib.quests.tasks.LocationTask;
import io.github.gcjojo.questslib.quests.tasks.StatTask;
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
        registerTaskClass(ResourceLocation.tryBuild(Questslib.MOD_ID, "stat"), StatTask.class);
        registerTaskClass(ResourceLocation.tryBuild(Questslib.MOD_ID, "location"), LocationTask.class);
        registerTaskClass(ResourceLocation.tryBuild(Questslib.MOD_ID, "any"), AnyTask.class);
        registerTaskClass(ResourceLocation.tryBuild(Questslib.MOD_ID, "all"), AllTask.class);

        QuestTaskDataRegistry.register(StatTask.class, StatTask.StatTaskData::new);
        QuestTaskDataRegistry.register(LocationTask.class, LocationTask.LocationTaskData::new);
        QuestTaskDataRegistry.register(AnyTask.class, AnyTask.AnyTaskData::new);
        QuestTaskDataRegistry.register(AllTask.class, AllTask.AllTaskData::new);
    }

    public static void registerDefaultRewards() {
        registerReward(ResourceLocation.tryBuild(Questslib.MOD_ID, "item"), ItemReward::new);
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
                    Questslib.printException(String.format("Unable to parse quest file %s", questFileLocation.toString()), e);
                else
                    Questslib.printException("Unable to parse quest file", e);
            }
        });

        return quests;
    }

    public static Optional<Quest> loadQuest(JsonObject json) {
        ResourceLocation questId;
        if (!json.has("id") || (questId = ResourceLocation.tryParse(json.get("id").getAsString())) == null) {
            Questslib.getLogger().error("No id provided for quest !");
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
                String rewardIdString = rewardJson.getAsString();
                ResourceLocation rewardId = ResourceLocation.tryParse(rewardIdString);
                JsonObject rewardData = rewardJson.getAsJsonObject();
                constructReward(rewardId, rewardData).ifPresent(rewards::add);
            });
        }

        return Optional.of(new Quest(questId, questName, questDescription, tasks, rewards));
    }

    public static Optional<QuestTask> constructTask(JsonObject json) {
        String taskType;
        if (!json.has("task") || (taskType = json.get("task").getAsString()) == null) {
            Questslib.getLogger().error("No Task Type provided.");
            return Optional.empty();
        }

        ResourceLocation taskTypeId = ResourceLocation.tryParse(taskType);
        if (!questTaskClasses.containsKey(taskTypeId)) {
            Questslib.getLogger().error("Invalid Task Type {}.", taskType);
            return Optional.empty();
        }

        Class<? extends QuestTask> taskClass = questTaskClasses.get(taskTypeId);
        try {
            return Optional.of(taskClass.getConstructor(JsonObject.class).newInstance(json));
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            Questslib.printException(String.format("Unable to create task %s.", taskType), e);
            return Optional.empty();
        }
    }

    public static Optional<QuestReward> constructReward(ResourceLocation rewardId, JsonObject json) {
        if (!questRewardFactories.containsKey(rewardId)) return Optional.empty();
        return Optional.ofNullable(questRewardFactories.get(rewardId).create(json));
    }
}
