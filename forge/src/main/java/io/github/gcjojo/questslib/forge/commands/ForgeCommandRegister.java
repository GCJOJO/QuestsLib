package io.github.gcjojo.questslib.forge.commands;

import io.github.gcjojo.questslib.QuestsLib;
import io.github.gcjojo.questslib.commands.QuestCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = QuestsLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeCommandRegister {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        QuestCommand.registerCommand(event.getDispatcher());
    }
}
