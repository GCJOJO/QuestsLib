package io.github.gcjojo.questslib.commands;


import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.architectury.networking.NetworkManager;
import io.github.gcjojo.questslib.network.QuestsNetwork;
import io.github.gcjojo.questslib.quests.PlayerQuestData;
import io.github.gcjojo.questslib.quests.QuestsManager;
import io.github.gcjojo.questslib.quests.enums.QuestCompletionState;
import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class QuestCommand {
    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("quests")
                .executes(QuestCommand::openQuestsScreen)
                .then(Commands.literal("list")
                        .executes(QuestCommand::listQuests))
                .then(Commands.literal("progression")
                        .then(Commands.argument("quest", ResourceLocationArgument.id())
                                .suggests(QuestCommand::suggestQuests)
                                .executes(QuestCommand::seeQuestProgression)))
                .then(Commands.literal("state")
                        .requires(commandSourceStack -> commandSourceStack.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("quest", ResourceLocationArgument.id())
                                .suggests(QuestCommand::suggestQuests)
                                .then(Commands.argument("state", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (QuestCompletionState state : QuestCompletionState.values())
                                                builder.suggest(state.getStateString());
                                            return builder.buildFuture();
                                        })
                                        .executes(QuestCommand::setQuestState))))
                .then(Commands.literal("reset")
                        .requires(commandSourceStack -> commandSourceStack.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(QuestCommand::resetQuestState))
                .then(Commands.literal("task")
                        .requires(commandSourceStack -> commandSourceStack.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("quest", ResourceLocationArgument.id())
                                .suggests(QuestCommand::suggestQuests)
                                .then(Commands.argument("task", ResourceLocationArgument.id())
                                        .suggests(QuestCommand::suggestTasks)
                                        .executes(QuestCommand::setTask)))));
    }

    public static CompletableFuture<Suggestions> suggestQuests(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        QuestsManager.getQuests().keySet().forEach(questId -> builder.suggest(questId.toString()));
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> suggestTasks(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        ResourceLocation questId = ResourceLocationArgument.getId(context, "quest");
        QuestsManager.getQuest(questId).ifPresent(quest -> quest.getTasks().forEach(task -> {
            builder.suggest(task.getTaskId().toString());
        }));
        return builder.buildFuture();
    }

    public static int openQuestsScreen(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        FriendlyByteBuf questsBuf = new FriendlyByteBuf(Unpooled.buffer());
        QuestsManager.PlayerQuestDataMap playerQuestDataMap = QuestsManager.getPlayerQuests(player);
        questsBuf.writeMap(playerQuestDataMap, FriendlyByteBuf::writeResourceLocation, PlayerQuestData.WRITER);

        NetworkManager.sendToPlayer(player, QuestsNetwork.SEND_QUESTS_DATA_PACKET_ID, questsBuf);
        NetworkManager.sendToPlayer(player, QuestsNetwork.OPEN_QUESTS_SCREEN_PACKET_ID, new FriendlyByteBuf(Unpooled.buffer()));
        return Command.SINGLE_SUCCESS;
    }

    public static int listQuests(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        QuestsManager.PlayerQuestDataMap quests = QuestsManager.getPlayerQuestsByState(player, QuestCompletionState.Started);
        quests.putAll(QuestsManager.getPlayerQuestsByState(player, QuestCompletionState.Completed));

        quests.forEach((questId, playerQuestData) -> {
            sendPlayerQuestInformation(player, questId, playerQuestData);
        });

        return Command.SINGLE_SUCCESS;
    }

    public static int seeQuestProgression(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation questId = ResourceLocationArgument.getId(context, "quest");
        Optional<PlayerQuestData> playerQuestDataOpt = QuestsManager.getPlayerQuestData(player, questId);
        if (playerQuestDataOpt.isPresent()) {
            sendPlayerQuestInformation(player, questId, playerQuestDataOpt.get());
            return Command.SINGLE_SUCCESS;
        }

        if (QuestsManager.getQuest(questId).isEmpty())
            player.sendSystemMessage(Component.literal(String.format("Cannot find quest %s", questId)).withStyle(ChatFormatting.RED));
        else
            player.sendSystemMessage(Component.literal(String.format("You have not started quest %s", questId)).withStyle(ChatFormatting.RED));

        return 0;
    }

    public static int setQuestState(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation questId = ResourceLocationArgument.getId(context, "quest");
        QuestCompletionState newState = QuestCompletionState.fromId(StringArgumentType.getString(context, "state"));
        QuestsManager.getPlayerQuestData(player, questId).ifPresent(quest -> quest.setCompletionState(newState));
        return Command.SINGLE_SUCCESS;
    }

    public static int resetQuestState(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        QuestsManager.getPlayerQuests(player).clear();
        return Command.SINGLE_SUCCESS;
    }

    public static int setTask(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation questId = ResourceLocationArgument.getId(context, "quest");
        ResourceLocation taskId = ResourceLocationArgument.getId(context, "task");

        QuestsManager.getPlayerQuestData(player, questId).ifPresent(quest -> quest.setCurrentTask(taskId));
        return Command.SINGLE_SUCCESS;
    }

    public static void sendPlayerQuestInformation(ServerPlayer player, ResourceLocation questId, PlayerQuestData playerQuestData) {
        QuestsManager.getQuest(questId).ifPresent(quest -> {
            ChatFormatting questColor = ChatFormatting.GOLD;
            if (playerQuestData.getCompletionState() == QuestCompletionState.Completed)
                questColor = ChatFormatting.GREEN;

            player.sendSystemMessage(Component.literal(quest.getQuestName().getString()).withStyle(questColor));
            int currentTaskNumber = playerQuestData.getCurrentTaskIndex();
            for (int taskNumber = 0; taskNumber <= quest.getTaskAmount(); taskNumber++) {
                final int finalTaskNumber = taskNumber;
                quest.getTask(taskNumber).ifPresent(task -> {
                    ChatFormatting messageColor = ChatFormatting.GREEN;
                    char frontCharacter = '✓';
                    if (playerQuestData.getCompletionState() != QuestCompletionState.Completed) {
                        if (finalTaskNumber == currentTaskNumber) {
                            messageColor = ChatFormatting.GOLD;
                            frontCharacter = '>';
                        } else if (finalTaskNumber > currentTaskNumber) {
                            messageColor = ChatFormatting.DARK_GRAY;
                            frontCharacter = '✕';
                        }
                    }
                    player.sendSystemMessage(Component.literal(String.format("%s   %s", frontCharacter, task.getTaskName().getString())).withStyle(messageColor));
                });
            }
        });
    }
}
