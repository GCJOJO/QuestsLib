package io.github.gcjojo.questslib.forge;

import dev.architectury.platform.forge.EventBuses;
import io.github.gcjojo.questslib.QuestsLib;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(QuestsLib.MOD_ID)
public final class QuestslibForge {

    public QuestslibForge(FMLJavaModLoadingContext context) {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(QuestsLib.MOD_ID, context.getModEventBus());

        // Run our common setup.
        QuestsLib.init();
    }

    @Mod.EventBusSubscriber(modid = QuestsLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEntryForge {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            QuestsLib.initClient();
        }
    }
}
