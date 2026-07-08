package io.github.gcjojo.questslib.quests;

import dev.architectury.platform.Platform;
import io.github.gcjojo.liblib.LibLib;
import io.github.gcjojo.questslib.QuestPlayerSaveData;
import io.github.gcjojo.questslib.QuestsLib;
import io.github.gcjojo.questslib.quests.enums.QuestCompletionState;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class QuestsManager {
    @Getter
    public static Map<ResourceLocation, Quest> quests = new HashMap<>();
    public static Map<Player, PlayerQuestDataMap> playersData = new HashMap<>();
    public static Map<Player, PlayerQuestTriggerDataMap> playerTriggersData = new HashMap<>();

    static ResourceLocation QUEST_PLAYER_SAVE_DATA = ResourceLocation.tryBuild(QuestsLib.MOD_ID, "quest_player_save_data");

    public static PlayerQuestDataMap loadPlayerData(Player player) {
        return LibLib.getPlayerDataManager().deserializePlayerData(player, QUEST_PLAYER_SAVE_DATA, QuestPlayerSaveData.class).getQuestData();
    }

    public static void savePlayerData(Player player) {
        QuestPlayerSaveData questPlayerSaveData = new QuestPlayerSaveData();
        questPlayerSaveData.setQuestData(playersData.get(player));
        LibLib.getPlayerDataManager().serializePlayerData(player, questPlayerSaveData, QUEST_PLAYER_SAVE_DATA);
    }

    public static PlayerQuestDataMap getPlayerQuests(Player player) {
        if (!playersData.containsKey(player)) return new PlayerQuestDataMap();

        return playersData.get(player);
    }

    public static void setPlayerQuests(Player player, PlayerQuestDataMap dataMap) {
        playersData.put(player, dataMap);
    }

    public static PlayerQuestDataMap getPlayerQuestsByState(Player player, QuestCompletionState state) {
        PlayerQuestDataMap quests = new PlayerQuestDataMap();
        getPlayerQuests(player).entrySet().stream()
                .filter(quest -> quest.getValue().getCompletionState() == state)
                .forEach(entry -> quests.put(entry.getKey(), entry.getValue()));
        return quests;
    }

    public static Optional<PlayerQuestData> getPlayerQuestData(Player player, ResourceLocation questId) {
        if (!playersData.containsKey(player))
            playersData.putIfAbsent(player, new PlayerQuestDataMap());

        PlayerQuestDataMap playerData = playersData.get(player);
        if (!playerData.containsKey(questId))
            playerData.putIfAbsent(questId, new PlayerQuestData(questId));

        return Optional.ofNullable(playerData.get(questId));
    }

    public static void startQuest(Player player, ResourceLocation questId) {
        getPlayerQuestData(player, questId).ifPresent(playerQuestData -> playerQuestData.setCompletionState(QuestCompletionState.Started));
    }

    public static void loadQuests(MinecraftServer server) {
        List<String> namespaces = new ArrayList<>(Platform.getModIds());
        namespaces.addAll(server.getResourceManager().getNamespaces());

        namespaces.forEach(namespace -> quests.putAll(QuestLoader.loadQuestFile(server, namespace)));
        QuestsLib.getLogger().info("Loaded {} quest(s) !", quests.size());
    }

    public static Optional<Quest> getQuest(ResourceLocation questId) {
        return Optional.ofNullable(quests.getOrDefault(questId, null));
    }

    public static Optional<QuestTask> getTask(ResourceLocation questId, ResourceLocation taskId) {
        Optional<Quest> quest = getQuest(questId);
        if (quest.isEmpty()) return Optional.empty();
        return quest.get().getTask(taskId);
    }

    public static float getPlayerProgression(Player player, ResourceLocation questId) {
        if (!playersData.containsKey(player)) return 0.0f;

        PlayerQuestDataMap dataMap = playersData.get(player);
        if (!dataMap.containsKey(questId)) return 0.0f;

        PlayerQuestData questData = dataMap.get(questId);

        return questData.getCurrentTaskData().getProgression();
    }

    public static class PlayerQuestDataMap extends HashMap<ResourceLocation, PlayerQuestData> {
    }

    public static class PlayerQuestTriggerDataMap extends HashMap<ResourceLocation, QuestTask.QuestTaskData<? extends QuestTask>> {
    }
}
