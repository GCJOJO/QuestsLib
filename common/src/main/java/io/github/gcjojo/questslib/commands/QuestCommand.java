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
import io.github.gcjojo.questslib.quests.QuestManager;
import io.github.gcjojo.questslib.quests.QuestTask;
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
                        .executes(QuestCommand::resetQuestState)));
    }

    public static CompletableFuture<Suggestions> suggestQuests(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        QuestManager.getQuests().keySet().forEach(questId -> builder.suggest(questId.toString()));
        return builder.buildFuture();
    }

    public static int openQuestsScreen(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        FriendlyByteBuf questsBuf = new FriendlyByteBuf(Unpooled.buffer());
        QuestManager.PlayerQuestDataMap playerQuestDataMap = QuestManager.getPlayerQuests(player);
        questsBuf.writeMap(playerQuestDataMap, FriendlyByteBuf::writeResourceLocation, PlayerQuestData.WRITER);

        NetworkManager.sendToPlayer(player, QuestsNetwork.SEND_QUESTS_DATA_PACKET_ID, questsBuf);
        NetworkManager.sendToPlayer(player, QuestsNetwork.OPEN_QUESTS_SCREEN_PACKET_ID, new FriendlyByteBuf(Unpooled.buffer()));
        return Command.SINGLE_SUCCESS;
    }

    public static int listQuests(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        QuestManager.PlayerQuestDataMap quests = QuestManager.getPlayerQuestsByState(player, QuestCompletionState.Started);
        quests.putAll(QuestManager.getPlayerQuestsByState(player, QuestCompletionState.Completed));

        quests.forEach((questId, playerQuestData) -> {
            sendPlayerQuestInformation(player, questId, playerQuestData);
        });

        return Command.SINGLE_SUCCESS;
    }

    public static int seeQuestProgression(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation questId = ResourceLocationArgument.getId(context, "quest");
        Optional<PlayerQuestData> playerQuestDataOpt = QuestManager.getPlayerQuestData(player, questId);
        if (playerQuestDataOpt.isPresent()) {
            sendPlayerQuestInformation(player, questId, playerQuestDataOpt.get());
            return Command.SINGLE_SUCCESS;
        }

        if (QuestManager.getQuest(questId).isEmpty())
            player.sendSystemMessage(Component.literal(String.format("Cannot find quest %s", questId)).withStyle(ChatFormatting.RED));
        else
            player.sendSystemMessage(Component.literal(String.format("You have not started quest %s", questId)).withStyle(ChatFormatting.RED));

        return 0;
    }

    public static int setQuestState(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation questId = ResourceLocationArgument.getId(context, "quest");
        QuestCompletionState newState = QuestCompletionState.fromId(StringArgumentType.getString(context, "state"));
        QuestManager.getPlayerQuestData(player, questId).ifPresent(quest -> quest.setCompletionState(newState));
        return Command.SINGLE_SUCCESS;
    }

    public static int resetQuestState(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        QuestManager.getPlayerQuests(player).clear();
        return Command.SINGLE_SUCCESS;
    }

    public static void sendPlayerQuestInformation(ServerPlayer player, ResourceLocation questId, PlayerQuestData playerQuestData) {
        QuestManager.getQuest(questId).ifPresent(quest -> {
            ChatFormatting questColor = ChatFormatting.GOLD;
            if (playerQuestData.getCompletionState() == QuestCompletionState.Completed)
                questColor = ChatFormatting.GREEN;

            player.sendSystemMessage(Component.literal(quest.getQuestName().getString()).withStyle(questColor));
            int currentTaskNumber = playerQuestData.getCurrentTaskId();
            for (int taskNumber = 0; taskNumber <= quest.getTaskAmount(); taskNumber++) {
                QuestTask task = quest.getTask(taskNumber);
                ChatFormatting messageColor = ChatFormatting.GREEN;
                char frontCharacter = '✓';
                if (taskNumber == currentTaskNumber) {
                    messageColor = ChatFormatting.GOLD;
                    frontCharacter = '>';
                } else if (taskNumber > currentTaskNumber) {
                    messageColor = ChatFormatting.DARK_GRAY;
                    frontCharacter = '✕';
                }
                if (task != null && task.getTaskName() != null)
                    player.sendSystemMessage(Component.literal(String.format("%s   %s", frontCharacter, task.getTaskName().getString())).withStyle(messageColor));
            }
        });
    }
}
