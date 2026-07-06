package io.github.gcjojo.questslib.quests;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.platform.Platform;
import dev.architectury.utils.value.IntValue;
import io.github.gcjojo.liblib.LibLib;
import io.github.gcjojo.liblib.events.LibLibEvents;
import io.github.gcjojo.questslib.QuestPlayerSaveData;
import io.github.gcjojo.questslib.QuestsLib;
import io.github.gcjojo.questslib.events.QuestsEvents;
import io.github.gcjojo.questslib.quests.enums.LocationTaskType;
import io.github.gcjojo.questslib.quests.enums.QuestCompletionState;
import io.github.gcjojo.questslib.quests.enums.StatTaskType;
import io.github.gcjojo.questslib.quests.enums.TaskType;
import io.github.gcjojo.questslib.quests.tasks.CompositeTask;
import io.github.gcjojo.questslib.quests.tasks.LocationTask;
import io.github.gcjojo.questslib.quests.tasks.StatTask;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class QuestManager {
    @Getter
    public static Map<ResourceLocation, Quest> quests = new HashMap<>();
    public static Map<Player, PlayerQuestDataMap> playersData = new HashMap<>();
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

    public static Optional<QuestTask> getTask(ResourceLocation questId, int taskNumber) {
        Optional<Quest> quest = getQuest(questId);
        return quest.map(value -> value.getTask(taskNumber));
    }

    public static Optional<QuestTask> getTask(ResourceLocation questId, ResourceLocation taskId) {
        Optional<Quest> quest = getQuest(questId);
        if (quest.isEmpty()) return Optional.empty();
        return quest.get().getTask(taskId);
    }

    public static void onServerLevelLoad(ServerLevel serverLevel) {
        loadQuests(serverLevel.getServer());
    }

    public static void onPlayerJoin(ServerPlayer player) {
        playersData.put(player, loadPlayerData(player));
    }

    public static void onPlayerLeave(ServerPlayer player) {
        savePlayerData(player);
        playersData.remove(player);
    }

    public static void initEvent() {
        LifecycleEvent.SERVER_LEVEL_LOAD.register(QuestManager::onServerLevelLoad);

        PlayerEvent.PLAYER_JOIN.register(QuestManager::onPlayerJoin);
        PlayerEvent.PLAYER_QUIT.register(QuestManager::onPlayerLeave);

        PlayerEvent.PICKUP_ITEM_POST.register(QuestManager::onPlayerPickupItem);
        LibLibEvents.PLAYER_INVENTORY_CHANGED.register(QuestManager::onPlayerInventoryChanged);
        BlockEvent.BREAK.register(QuestManager::onPlayerBreakBlock);
        BlockEvent.PLACE.register(QuestManager::onEntityPlaceBlock);

        EntityEvent.LIVING_DEATH.register(QuestManager::onEntityDie);

        LibLibEvents.PLAYER_ENTERED_BIOME.register(QuestManager::onPlayerEnteredBiome);
        LibLibEvents.PLAYER_ENTERED_STRUCTURE.register(QuestManager::onPlayerEnteredStructure);
    }

    public static void onTaskUpdate(Player player, ResourceLocation questId, ResourceLocation taskId, PlayerQuestData questData) {
        QuestsEvents.TASK_PROGRESSION.invoker().taskProgression(player, questId, taskId);

        if (questData.checkTaskProgression()) {
            QuestsEvents.TASK_COMPLETED.invoker().taskCompleted(player, taskId);
            getTask(questId, taskId).ifPresent(task -> task.rewardPlayer(player));

            questData.nextTask();
            if (questData.getCompletionState() == QuestCompletionState.Completed) {
                QuestsEvents.QUEST_COMPLETED.invoker().questCompleted(player, questId);
                getQuest(questId).ifPresent(quest -> quest.rewardPlayer(player));
            }
        }
    }

    public static void onStatTaskUpdate(Player player, PlayerQuestData questData, ResourceLocation targetId, int amount, StatTaskType type) {
        if (questData.getCompletionState() == QuestCompletionState.None || questData.getCompletionState() == QuestCompletionState.Completed)
            return;

        ResourceLocation questId = questData.getQuestId();
        Quest quest = getQuest(questId).orElse(null);
        if (quest == null) return;

        QuestTask task = questData.getCurrentTask().orElse(null);
        if (task == null) return;

        if (task.getTaskType() == TaskType.Stat) {
            StatTask statTask = (StatTask) task;
            if (statTask.getStatType() != type || !statTask.getTargetId().equals(targetId)) return;

            if (!(questData.getCurrentTaskData() instanceof StatTask.StatTaskData statData)) return;

            statData.addAmount(amount);
        } else if (task.getTaskType() == TaskType.Any || task.getTaskType() == TaskType.All) {
            CompositeTask compositeTask = (CompositeTask) task;
            if (!(questData.getCurrentTaskData() instanceof CompositeTask.CompositeTaskData<? extends CompositeTask> compositeData))
                return;

            compositeTask.getSubtasks().keySet().stream().filter(
                    subtask -> subtask.getTaskType() == TaskType.Stat &&
                            subtask instanceof StatTask statSubtask &&
                            statSubtask.getStatType() == type &&
                            statSubtask.getTargetId().equals(targetId)).forEach(subtask ->
                    compositeData.getSubtaskData(subtask.getTaskId()).ifPresent(subtaskData -> {
                        if (!(subtaskData instanceof StatTask.StatTaskData subStatTaskData)) return;
                        subStatTaskData.addAmount(amount);
                        if (subStatTaskData.checkProgression())
                            subtask.rewardPlayer(player);
                    }));
        }

        onTaskUpdate(player, questId, task.getTaskId(), questData);
    }

    public static void onLocationTaskUpdated(ServerPlayer player, PlayerQuestData questData, ResourceLocation locationId, LocationTaskType type) {
        if (questData.getCompletionState() == QuestCompletionState.None || questData.getCompletionState() == QuestCompletionState.Completed)
            return;
        ResourceLocation questId = questData.getQuestId();
        Quest quest = getQuest(questId).orElse(null);
        if (quest == null) return;

        QuestTask task = questData.getCurrentTask().orElse(null);
        if (task == null) return;

        ServerLevel level = (ServerLevel) player.level();
        if (task.getTaskType() == TaskType.Location) {
            LocationTask locationTask = (LocationTask) task;
            if (locationTask.getLocationTaskType() != type || !locationTask.locationMatch(level, locationId)) return;
            if (!(questData.getCurrentTaskData() instanceof LocationTask.LocationTaskData locationData)) return;
            locationData.setHasVisitedLocation(true);
        } else if (task.getTaskType() == TaskType.Any || task.getTaskType() == TaskType.All) {
            CompositeTask compositeTask = (CompositeTask) task;
            if (!(questData.getCurrentTaskData() instanceof CompositeTask.CompositeTaskData<? extends CompositeTask> compositeData))
                return;

            compositeTask.getSubtasks().keySet().stream().filter(
                    subtask -> subtask.getTaskType() == TaskType.Location &&
                            subtask instanceof LocationTask locationTask &&
                            locationTask.getLocationTaskType() == type &&
                            locationTask.locationMatch(level, locationId)).forEach(subtask ->
                    compositeData.getSubtaskData(subtask.getTaskId()).ifPresent(subtaskData -> {
                        if (!(subtaskData instanceof LocationTask.LocationTaskData locationTaskData)) return;
                        locationTaskData.setHasVisitedLocation(true);
                        if (locationTaskData.checkProgression())
                            subtask.rewardPlayer(player);
                    }));
        }

        onTaskUpdate(player, questId, task.getTaskId(), questData);
    }

    public static void onPlayerInventoryChanged(Player player, Map<ResourceLocation, Integer> inventoryDifference) {
        PlayerQuestDataMap dataMap = playersData.get(player);

        dataMap.forEach((questId, questData) -> {
            inventoryDifference.forEach((itemId, amount) -> onStatTaskUpdate(player, questData, itemId, amount, StatTaskType.Item));
        });
    }

    public static void onPlayerPickupItem(Player player, ItemEntity itemEntity, ItemStack stack) {
        if (!playersData.containsKey(player)) return;

        PlayerQuestDataMap dataMap = playersData.get(player);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        int stackAmount = stack.getCount();
        dataMap.forEach((questId, questData) -> {
            if (isStackUsedForQuest(stack, player, questId)) return;
            onStatTaskUpdate(player, questData, itemId, stackAmount, StatTaskType.Item);
            markStackAsUsedForQuest(stack, player, questId);
        });
    }

    public static void markStackAsUsedForQuest(ItemStack stack, Player player, ResourceLocation questId) {
        CompoundTag nbt = stack.getOrCreateTag();
        if (!nbt.contains("Quests"))
            nbt.put("Quests", new CompoundTag());


        if (!nbt.getCompound("Quests").contains(player.getStringUUID()))
            nbt.getCompound("Quests").put(player.getStringUUID(), new CompoundTag());

        nbt.getCompound("Quests").getCompound(player.getStringUUID()).put(questId.toString(), new CompoundTag());
    }

    public static boolean isStackUsedForQuest(ItemStack stack, Player player, ResourceLocation questId) {
        CompoundTag nbt = stack.getOrCreateTag();
        return nbt.contains("Quests") && nbt.getCompound("Quests").contains(player.getStringUUID()) &&
                nbt.getCompound("Quests").getCompound(player.getStringUUID()).contains(questId.toString());
    }

    public static EventResult onPlayerBreakBlock(Level level, BlockPos pos, BlockState state, ServerPlayer player, IntValue xp) {
        if (!playersData.containsKey(player)) return EventResult.pass();

        PlayerQuestDataMap dataMap = playersData.get(player);
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        dataMap.forEach((questId, questData) -> onStatTaskUpdate(player, questData, blockId, 1, StatTaskType.BrokenBlocks));

        return EventResult.pass();
    }

    public static EventResult onEntityPlaceBlock(Level level, BlockPos pos, BlockState state, Entity placer) {
        if (!(placer instanceof Player player)) return EventResult.pass();

        if (!playersData.containsKey(player)) return EventResult.pass();

        PlayerQuestDataMap dataMap = playersData.get(player);
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        dataMap.forEach((questId, questData) -> onStatTaskUpdate(player, questData, blockId, 1, StatTaskType.PlacedBlocks));

        return EventResult.pass();
    }

    public static EventResult onEntityDie(LivingEntity entity, DamageSource source) {
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof Player player)) return EventResult.pass();

        if (!playersData.containsKey(player)) return EventResult.pass();

        PlayerQuestDataMap dataMap = playersData.get(player);
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        dataMap.forEach((questId, questData) -> onStatTaskUpdate(player, questData, entityId, 1, StatTaskType.KilledMobs));

        return EventResult.pass();
    }

    public static void onPlayerEnteredBiome(ServerPlayer player, ResourceLocation biomeId) {
        QuestsLib.getLogger().info("Player {} has entered biome {}", player.getName().getString(), biomeId.toString());

        if (!playersData.containsKey(player)) return;

        PlayerQuestDataMap dataMap = playersData.get(player);
        dataMap.forEach((questId, questData) -> onLocationTaskUpdated(player, questData, biomeId, LocationTaskType.Biome));
    }

    public static void onPlayerEnteredStructure(ServerPlayer player, ResourceLocation structureId) {
        QuestsLib.getLogger().info("Player {} has entered structure {}", player.getName().getString(), structureId.toString());

        if (!playersData.containsKey(player)) return;

        PlayerQuestDataMap dataMap = playersData.get(player);
        dataMap.forEach((questId, questData) -> onLocationTaskUpdated(player, questData, structureId, LocationTaskType.Structure));
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
}
