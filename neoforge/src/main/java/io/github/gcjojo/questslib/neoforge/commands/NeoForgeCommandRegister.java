package io.github.gcjojo.questslib.neoforge.commands;

import io.github.gcjojo.questslib.commands.QuestCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class NeoForgeCommandRegister {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        QuestCommand.registerCommand(event.getDispatcher());
    }
}
