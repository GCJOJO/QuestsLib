package io.github.gcjojo.questslib.quests;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.utils.value.IntValue;
import io.github.gcjojo.questslib.events.QuestsEvents;
import io.github.gcjojo.questslib.quests.tasks.CompositeTask;
import io.github.gcjojo.questslib.quests.tasks.StatTask;
import io.github.gcjojo.questslib.quests.tasks.StatTaskType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class QuestManager {

    public static Map<ResourceLocation, Quest> quests = new HashMap<>();
    public static Map<Player, PlayerQuestDataMap> playerData = new HashMap<>();

    public static PlayerQuestDataMap loadPlayerData(Player player) {
        //if(playerData.containsKey(player))
        //{
        //}
        // Load player data, if doesn't exist, create new
        return new PlayerQuestDataMap();
    }

    public static void savePlayerData(Player player) {

    }

    public static void loadQuests() {

    }

    public static Optional<Quest> getQuest(ResourceLocation questId) {
        return Optional.ofNullable(quests.getOrDefault(questId, null));
    }

    public static void onPlayerJoin(ServerPlayer player) {
        playerData.put(player, loadPlayerData(player));
    }

    public static void onPlayerLeave(ServerPlayer player) {
        savePlayerData(player);
        playerData.remove(player);
    }

    public static void initEvent() {
        PlayerEvent.PLAYER_JOIN.register(QuestManager::onPlayerJoin);
        PlayerEvent.PLAYER_QUIT.register(QuestManager::onPlayerLeave);

        PlayerEvent.PICKUP_ITEM_POST.register(QuestManager::onPlayerPickupItem);
        BlockEvent.BREAK.register(QuestManager::onPlayerBreakBlock);
        BlockEvent.PLACE.register(QuestManager::onEntityPlaceBlock);

        EntityEvent.LIVING_DEATH.register(QuestManager::onEntityDie);
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
            if (statTask.getStatType() != type || statTask.getTargetId() != targetId) return;

            if (!(questData.getCurrentTaskData() instanceof StatTask.StatTaskData statData)) return;

            statData.addAmount(amount);
        } else if (task.getTaskType() == TaskType.Any || task.getTaskType() == TaskType.All) {
            CompositeTask compositeTask = (CompositeTask) task;

            compositeTask.getSubtasks().keySet().stream().filter(
                    subtask -> subtask.getTaskType() == TaskType.Stat &&
                            subtask instanceof StatTask statSubtask &&
                            statSubtask.getStatType() == type &&
                            statSubtask.getTargetId() == targetId).forEach(subtask -> {
                if (!(questData.getCurrentTaskData() instanceof StatTask.StatTaskData statData)) return;
                statData.addAmount(amount);
            });
        }


        QuestsEvents.TASK_PROGRESSION.invoker().taskProgression(player, questId, task.getTaskId());

        if (questData.checkTaskProgression()) {
            QuestsEvents.TASK_COMPLETED.invoker().taskCompleted(player, task.getTaskId());

            questData.nextTask();
            if (questData.getCompletionState() == QuestCompletionState.Completed)
                QuestsEvents.QUEST_COMPLETED.invoker().questCompleted(player, questId);
        }
    }

    public static void onPlayerPickupItem(Player player, ItemEntity itemEntity, ItemStack stack) {
        if (!playerData.containsKey(player)) return;

        PlayerQuestDataMap dataMap = playerData.get(player);
        dataMap.forEach((questId, questData) -> {
            onStatTaskUpdate(player, questData, BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getCount(), StatTaskType.Item);
        });
    }

    public static EventResult onPlayerBreakBlock(Level level, BlockPos pos, BlockState state, ServerPlayer player, IntValue xp) {
        if (!playerData.containsKey(player)) return EventResult.pass();

        PlayerQuestDataMap dataMap = playerData.get(player);
        dataMap.forEach((questId, questData) -> {
            onStatTaskUpdate(player, questData, BuiltInRegistries.BLOCK.getKey(state.getBlock()), 1, StatTaskType.BrokenBlocks);
        });

        return EventResult.pass();
    }

    public static EventResult onEntityPlaceBlock(Level level, BlockPos pos, BlockState state, Entity placer) {
        if (!(placer instanceof Player player)) return EventResult.pass();

        if (!playerData.containsKey(player)) return EventResult.pass();

        PlayerQuestDataMap dataMap = playerData.get(player);
        dataMap.forEach((questId, questData) -> {
            onStatTaskUpdate(player, questData, BuiltInRegistries.BLOCK.getKey(state.getBlock()), 1, StatTaskType.PlacedBlocks);
        });

        return EventResult.pass();
    }

    public static EventResult onEntityDie(LivingEntity entity, DamageSource source) {
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof Player player)) return EventResult.pass();

        if (!playerData.containsKey(player)) return EventResult.pass();

        PlayerQuestDataMap dataMap = playerData.get(player);
        dataMap.forEach((questId, questData) -> {
            onStatTaskUpdate(player, questData, BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), 1, StatTaskType.KilledMobs);
        });

        return EventResult.pass();
    }

    public static float getPlayerProgression(Player player, ResourceLocation questId) {
        if (!playerData.containsKey(player)) return 0.0f;

        PlayerQuestDataMap dataMap = playerData.get(player);
        if (!dataMap.containsKey(questId)) return 0.0f;

        PlayerQuestData questData = dataMap.get(questId);

        return questData.getCurrentTaskData().getProgression();
    }

    public static class PlayerQuestDataMap extends HashMap<ResourceLocation, PlayerQuestData> {
    }
}
