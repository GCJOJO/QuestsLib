package io.github.gcjojo.questslib.quests;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.utils.value.IntValue;
import io.github.gcjojo.questslib.Questslib;
import io.github.gcjojo.questslib.events.QuestsEvents;
import io.github.gcjojo.questslib.quests.tasks.StatTask;
import io.github.gcjojo.questslib.quests.tasks.StatTaskType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
        ArrayList quest1Tasks = new ArrayList<QuestTask>();
        quest1Tasks.add(new StatTask(ResourceLocation.tryBuild(Questslib.MOD_ID, "test_item_task"), Component.literal("Test Item Task"), Component.literal("Test Item Task"),
                ResourceLocation.tryBuild(ResourceLocation.DEFAULT_NAMESPACE, "dirt"), 10, StatTaskType.Item));

        quests.put(ResourceLocation.tryBuild(Questslib.MOD_ID, "test_item_quest"), new Quest(ResourceLocation.tryBuild(Questslib.MOD_ID, "test_item_quest"), Component.literal("Test Item Quest"), Component.literal("This quest test obtain items tasks"), quest1Tasks));
    }

    @Nullable
    public static Quest getQuest(ResourceLocation questId) {
        return quests.getOrDefault(questId, null);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        playerData.put(player, loadPlayerData(player));
        playerData.get(player).put(ResourceLocation.tryBuild(Questslib.MOD_ID, "test_item_quest"), new PlayerQuestData(ResourceLocation.tryBuild(Questslib.MOD_ID, "test_item_quest")));
    }

    public static void onPlayerLeave(ServerPlayer player) {
        savePlayerData(player);
        playerData.remove(player);
    }

    public static void initEvent() {
        PlayerEvent.PLAYER_JOIN.register(QuestManager::onPlayerJoin);
        PlayerEvent.PLAYER_QUIT.register(QuestManager::onPlayerLeave);

        PlayerEvent.PICKUP_ITEM_POST.register(QuestManager::PlayerPickupItem);
        BlockEvent.BREAK.register(QuestManager::PlayerBreakBlock);
        BlockEvent.PLACE.register(QuestManager::EntityPlaceBlock);
    }

    public static void PlayerPickupItem(Player player, ItemEntity itemEntity, ItemStack stack) {
        if (!playerData.containsKey(player)) return;

        PlayerQuestDataMap dataMap = playerData.get(player);
        dataMap.forEach((questId, questData) -> {
            if (questData.getCompletionState() == QuestCompletionState.None) return;

            Quest quest = getQuest(questId);
            if (quest == null) return;

            QuestTask task = questData.getCurrentTask().orElse(null);
            if (task == null) return;

            if (task.getTaskType() != TaskType.Stat) return;

            StatTask statTask = (StatTask) task;
            if (statTask.getStatType() != StatTaskType.Item || statTask.getItem() != stack.getItem()) return;

            if (!(questData.getCurrentTaskData() instanceof StatTask.StatTaskData statData)) return;

            statData.addAmount(stack.getCount());

            QuestsEvents.TASK_PROGRESSION.invoker().taskProgression(player, questId, task.getTaskId());

            if (questData.checkTaskProgression()) {
                QuestsEvents.TASK_COMPLETED.invoker().taskCompleted(player, task.getTaskId());

                questData.nextTask();
                if (questData.getCompletionState() == QuestCompletionState.Completed)
                    QuestsEvents.QUEST_COMPLETED.invoker().questCompleted(player, questId);
            }
        });
    }

    public static EventResult PlayerBreakBlock(Level level, BlockPos pos, BlockState state, ServerPlayer player, IntValue xp) {

        return EventResult.pass();
    }

    public static EventResult EntityPlaceBlock(Level level, BlockPos pos, BlockState state, Entity placer) {
        if (!(placer instanceof Player)) return EventResult.pass();

        Player player = (Player) placer;

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
