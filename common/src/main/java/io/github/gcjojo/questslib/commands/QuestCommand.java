package io.github.gcjojo.questslib.commands;


import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.gcjojo.questslib.quests.QuestManager;
import io.github.gcjojo.questslib.quests.QuestTask;
import io.github.gcjojo.questslib.quests.enums.QuestCompletionState;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

public class QuestCommand {
    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("quests")
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
                                        .executes(QuestCommand::setQuestState)))));
    }

    public static CompletableFuture<Suggestions> suggestQuests(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        QuestManager.getQuests().keySet().forEach(questId -> builder.suggest(questId.toString()));
        return builder.buildFuture();
    }

    public static int listQuests(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        QuestManager.PlayerQuestDataMap startedQuests = QuestManager.getPlayerQuestsByState(player, QuestCompletionState.Started);
        QuestManager.PlayerQuestDataMap completedQuests = QuestManager.getPlayerQuestsByState(player, QuestCompletionState.Completed);

        startedQuests.forEach((questId, playerQuestData) -> {
            QuestManager.getQuest(questId).ifPresent(quest -> {
                player.sendSystemMessage(Component.literal(quest.getQuestName().getString()).withStyle(ChatFormatting.GOLD));
                int currentTaskNumber = playerQuestData.getCurrentTaskId();
                for (int taskNumber = 0; taskNumber <= quest.getTaskAmount(); taskNumber++) {
                    QuestTask task = quest.getTask(taskNumber);
                    ChatFormatting messageColor = ChatFormatting.GREEN;
                    if (taskNumber == currentTaskNumber)
                        messageColor = ChatFormatting.GOLD;
                    else if (taskNumber > currentTaskNumber)
                        messageColor = ChatFormatting.DARK_GRAY;
                    if (task != null && task.getTaskName() != null)
                        player.sendSystemMessage(Component.literal(String.format("    %s", task.getTaskName().getString())).withStyle(messageColor));
                }
            });
        });

        completedQuests.forEach((questId, playerQuestData) -> {
            QuestManager.getQuest(questId).ifPresent(quest -> {
                player.sendSystemMessage(Component.literal(quest.getQuestName().getString()).withStyle(ChatFormatting.GREEN));
            });
        });
        return Command.SINGLE_SUCCESS;
    }

    public static int seeQuestProgression(CommandContext<CommandSourceStack> context) {
        return Command.SINGLE_SUCCESS;
    }

    public static int setQuestState(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation questId = ResourceLocationArgument.getId(context, "quest");
        QuestCompletionState newState = QuestCompletionState.fromId(StringArgumentType.getString(context, "state"));
        QuestManager.getPlayerQuestData(player, questId).ifPresent(quest -> quest.setCompletionState(newState));
        return Command.SINGLE_SUCCESS;
    }
}
