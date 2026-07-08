package io.github.gcjojo.questslib.quests;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.utils.value.IntValue;
import io.github.gcjojo.liblib.api.BlablaLibAPI;
import io.github.gcjojo.liblib.events.LibLibEvents;
import io.github.gcjojo.questslib.QuestsLib;
import io.github.gcjojo.questslib.events.QuestsEvents;
import io.github.gcjojo.questslib.quests.enums.LocationTaskType;
import io.github.gcjojo.questslib.quests.enums.QuestCompletionState;
import io.github.gcjojo.questslib.quests.enums.StatTaskType;
import io.github.gcjojo.questslib.quests.enums.TaskType;
import io.github.gcjojo.questslib.quests.tasks.CompositeTask;
import io.github.gcjojo.questslib.quests.tasks.DialogueTask;
import io.github.gcjojo.questslib.quests.tasks.LocationTask;
import io.github.gcjojo.questslib.quests.tasks.StatTask;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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

import java.util.Map;

public class GameEventsListener {

    public static void onServerLevelLoad(ServerLevel serverLevel) {
        QuestsManager.loadQuests(serverLevel.getServer());
    }

    public static void onPlayerJoin(ServerPlayer player) {
        QuestsManager.playersData.put(player, QuestsManager.loadPlayerData(player));
    }

    public static void onPlayerLeave(ServerPlayer player) {
        QuestsManager.savePlayerData(player);
        QuestsManager.playersData.remove(player);
    }

    public static void initEvent() {
        LifecycleEvent.SERVER_LEVEL_LOAD.register(GameEventsListener::onServerLevelLoad);

        PlayerEvent.PLAYER_JOIN.register(GameEventsListener::onPlayerJoin);
        PlayerEvent.PLAYER_QUIT.register(GameEventsListener::onPlayerLeave);

        PlayerEvent.PICKUP_ITEM_POST.register(GameEventsListener::onPlayerPickupItem);
        LibLibEvents.PLAYER_INVENTORY_CHANGED.register(GameEventsListener::onPlayerInventoryChanged);
        BlockEvent.BREAK.register(GameEventsListener::onPlayerBreakBlock);
        BlockEvent.PLACE.register(GameEventsListener::onEntityPlaceBlock);

        EntityEvent.LIVING_DEATH.register(GameEventsListener::onEntityDie);

        LibLibEvents.PLAYER_ENTERED_BIOME.register(GameEventsListener::onPlayerEnteredBiome);
        LibLibEvents.PLAYER_ENTERED_STRUCTURE.register(GameEventsListener::onPlayerEnteredStructure);

        BlablaLibAPI.DIALOGUE_COMPLETED.register(GameEventsListener::onPlayerCompletedDialogue);
    }

    public static void checkQuestTrigger(Player player, ResourceLocation questId, TriggerCheck checkFunction) {
        if (checkFunction.checkTrigger())
            QuestsManager.startQuest(player, questId);
    }

    public static void updateTask(Player player, PlayerQuestData questData, TaskUpdateCheck updateCheck, TaskUpdate update) {
        if (questData.getCompletionState() == QuestCompletionState.None || questData.getCompletionState() == QuestCompletionState.Completed)
            return;

        ResourceLocation questId = questData.getQuestId();
        Quest quest = QuestsManager.getQuest(questId).orElse(null);
        if (quest == null) return;

        QuestTask task = questData.getCurrentTask().orElse(null);
        if (task == null) return;

        QuestTask.QuestTaskData<? extends QuestTask> taskData = questData.getCurrentTaskData();
        if (updateCheck.canUpdateTask(task)) {
            update.updateTask(taskData);
        } else if (task instanceof CompositeTask compositeTask) {
            CompositeTask.CompositeTaskData<? extends CompositeTask> compositeData = (CompositeTask.CompositeTaskData<? extends CompositeTask>) taskData;

            compositeTask.getSubtasks().keySet().stream().filter(
                            updateCheck::canUpdateTask)
                    .forEach(subtask ->
                            compositeData.getSubtaskData(subtask.getTaskId()).ifPresent(subtaskData -> {
                                update.updateTask(subtaskData);
                                if (subtaskData.checkProgression())
                                    subtask.rewardPlayer(player);
                            }));
        }

        onTaskUpdate(player, questId, task.getTaskId(), questData);
    }

    public static void onTaskUpdate(Player player, ResourceLocation questId, ResourceLocation taskId, PlayerQuestData questData) {
        QuestsEvents.TASK_PROGRESSION.invoker().taskProgression(player, questId, taskId);

        if (questData.checkTaskProgression()) {
            QuestsEvents.TASK_COMPLETED.invoker().taskCompleted(player, questId, taskId);
            QuestsManager.getTask(questId, taskId).ifPresent(task -> task.rewardPlayer(player));

            questData.nextTask();
            if (questData.getCompletionState() == QuestCompletionState.Completed) {
                QuestsEvents.QUEST_COMPLETED.invoker().questCompleted(player, questId);
                QuestsManager.getQuest(questId).ifPresent(quest -> quest.rewardPlayer(player));
            }
        }
    }

    public static void onStatTaskUpdate(Player player, PlayerQuestData questData, ResourceLocation targetId, int amount, StatTaskType type) {
        updateTask(player, questData, (task -> GameEventsListener.checkStatTask(task, targetId, type)), (taskData) -> {
            StatTask.StatTaskData statTaskData = (StatTask.StatTaskData) taskData;
            statTaskData.addAmount(amount);
        });

        /*if (questData.getCompletionState() == QuestCompletionState.None || questData.getCompletionState() == QuestCompletionState.Completed)
            return;

        ResourceLocation questId = questData.getQuestId();
        Quest quest = QuestsManager.getQuest(questId).orElse(null);
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

        onTaskUpdate(player, questId, task.getTaskId(), questData);*/
    }

    public static void onLocationTaskUpdated(ServerPlayer player, PlayerQuestData questData, ResourceLocation locationId, LocationTaskType type) {
        ServerLevel level = (ServerLevel) player.level();

        updateTask(player, questData, (task -> GameEventsListener.checkLocationTask(task, locationId, type, level)), (taskData) -> {
            LocationTask.LocationTaskData locationTaskData = (LocationTask.LocationTaskData) taskData;
            locationTaskData.setHasVisitedLocation(true);
        });

        /*if (questData.getCompletionState() == QuestCompletionState.None || questData.getCompletionState() == QuestCompletionState.Completed)
            return;
        ResourceLocation questId = questData.getQuestId();
        Quest quest = QuestsManager.getQuest(questId).orElse(null);
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

        onTaskUpdate(player, questId, task.getTaskId(), questData);*/
    }

    public static void onDialogueTaskUpdated(ServerPlayer player, PlayerQuestData questData, ResourceLocation dialogueId) {
        updateTask(player, questData, (task -> GameEventsListener.checkDialogueTask(task, dialogueId)), (taskData) -> {
            DialogueTask.DialogueTaskData dialogueTaskData = (DialogueTask.DialogueTaskData) taskData;
            dialogueTaskData.setHasReadDialogue(true);
        });
    }

    public static void onPlayerInventoryChanged(Player player, Map<ResourceLocation, Integer> inventoryDifference) {
        QuestsManager.PlayerQuestDataMap dataMap = QuestsManager.playersData.get(player);

        dataMap.forEach((questId, questData) -> {
            inventoryDifference.forEach((itemId, amount) -> onStatTaskUpdate(player, questData, itemId, amount, StatTaskType.Item));
        });
    }

    public static void onPlayerPickupItem(Player player, ItemEntity itemEntity, ItemStack stack) {
        if (!QuestsManager.playersData.containsKey(player)) return;

        QuestsManager.PlayerQuestDataMap dataMap = QuestsManager.playersData.get(player);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        int stackAmount = stack.getCount();
        dataMap.forEach((questId, questData) -> {
            if (isStackUsedForQuest(stack, player, questId)) return;
            onStatTaskUpdate(player, questData, itemId, stackAmount, StatTaskType.Item);
            markStackAsUsedForQuest(stack, player, questId);
        });
    }

    public static EventResult onPlayerBreakBlock(Level level, BlockPos pos, BlockState state, ServerPlayer player, IntValue xp) {
        if (!QuestsManager.playersData.containsKey(player)) return EventResult.pass();

        QuestsManager.PlayerQuestDataMap dataMap = QuestsManager.playersData.get(player);
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        dataMap.forEach((questId, questData) -> onStatTaskUpdate(player, questData, blockId, 1, StatTaskType.BrokenBlocks));

        return EventResult.pass();
    }

    public static EventResult onEntityPlaceBlock(Level level, BlockPos pos, BlockState state, Entity placer) {
        if (!(placer instanceof Player player)) return EventResult.pass();

        if (!QuestsManager.playersData.containsKey(player)) return EventResult.pass();

        QuestsManager.PlayerQuestDataMap dataMap = QuestsManager.playersData.get(player);
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        dataMap.forEach((questId, questData) -> onStatTaskUpdate(player, questData, blockId, 1, StatTaskType.PlacedBlocks));

        return EventResult.pass();
    }

    public static EventResult onEntityDie(LivingEntity entity, DamageSource source) {
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof Player player)) return EventResult.pass();

        if (!QuestsManager.playersData.containsKey(player)) return EventResult.pass();

        QuestsManager.PlayerQuestDataMap dataMap = QuestsManager.playersData.get(player);
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        dataMap.forEach((questId, questData) -> onStatTaskUpdate(player, questData, entityId, 1, StatTaskType.KilledMobs));

        return EventResult.pass();
    }

    public static void onPlayerEnteredBiome(ServerPlayer player, ResourceLocation biomeId) {
        QuestsLib.getLogger().info("Player {} has entered biome {}", player.getName().getString(), biomeId.toString());

        if (!QuestsManager.playersData.containsKey(player)) return;

        QuestsManager.PlayerQuestDataMap dataMap = QuestsManager.playersData.get(player);
        dataMap.forEach((questId, questData) -> onLocationTaskUpdated(player, questData, biomeId, LocationTaskType.Biome));
    }

    public static void onPlayerEnteredStructure(ServerPlayer player, ResourceLocation structureId) {
        QuestsLib.getLogger().info("Player {} has entered structure {}", player.getName().getString(), structureId.toString());

        if (!QuestsManager.playersData.containsKey(player)) return;

        QuestsManager.PlayerQuestDataMap dataMap = QuestsManager.playersData.get(player);
        dataMap.forEach((questId, questData) -> onLocationTaskUpdated(player, questData, structureId, LocationTaskType.Structure));
    }

    public static void onPlayerCompletedDialogue(ServerPlayer player, ResourceLocation dialogueId) {
        if (!QuestsManager.playersData.containsKey(player)) return;

        QuestsManager.PlayerQuestDataMap dataMap = QuestsManager.playersData.get(player);
        dataMap.forEach((questId, questData) -> onDialogueTaskUpdated(player, questData, dialogueId));
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

    public static boolean checkStatTask(QuestTask task, ResourceLocation targetId, StatTaskType type) {
        if (task.getTaskType() != TaskType.Stat) return false;
        StatTask statTask = (StatTask) task;
        return statTask.getStatType() == type && statTask.getTargetId().equals(targetId);
    }

    public static boolean checkLocationTask(QuestTask task, ResourceLocation locationId, LocationTaskType type, ServerLevel level) {
        if (task.getTaskType() != TaskType.Location) return false;
        LocationTask locationTask = (LocationTask) task;
        return locationTask.getLocationTaskType() != type && locationTask.locationMatch(level, locationId);
    }

    public static boolean checkDialogueTask(QuestTask task, ResourceLocation dialogueId) {
        if (task.getTaskType() != TaskType.CompleteDialogue) return false;
        DialogueTask dialogueTask = (DialogueTask) task;
        return dialogueTask.getDialogueId().equals(dialogueId);
    }

    public interface TaskUpdateCheck {
        boolean canUpdateTask(QuestTask task);
    }

    public interface TaskUpdate {
        void updateTask(QuestTask.QuestTaskData<? extends QuestTask> questData);
    }

    public interface TriggerCheck {
        boolean checkTrigger();
    }
}
