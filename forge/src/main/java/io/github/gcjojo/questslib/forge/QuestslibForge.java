package io.github.gcjojo.questslib.forge;

import dev.architectury.platform.forge.EventBuses;
import io.github.gcjojo.questslib.QuestsLib;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(QuestsLib.MOD_ID)
public final class QuestslibForge {

    public QuestslibForge(FMLJavaModLoadingContext context) {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(QuestsLib.MOD_ID, context.getModEventBus());

        // Run our common setup.
        QuestsLib.init();
    }
}
