package io.github.gcjojo.questslib.fabric;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import io.github.gcjojo.questslib.QuestsLib;
import io.github.gcjojo.questslib.commands.QuestCommand;
import net.fabricmc.api.ModInitializer;

public final class QuestslibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        QuestsLib.init();
        CommandRegistrationEvent.EVENT.register(((dispatcher, registry, selection) -> {
            QuestCommand.registerCommand(dispatcher);
        }));
    }
}
