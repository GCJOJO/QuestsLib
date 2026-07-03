package io.github.gcjojo.questslib.forge;

import dev.architectury.platform.forge.EventBuses;
import io.github.gcjojo.questslib.Questslib;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Questslib.MOD_ID)
public final class QuestslibForge {
    public QuestslibForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(Questslib.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // Run our common setup.
        Questslib.init();
    }
}
