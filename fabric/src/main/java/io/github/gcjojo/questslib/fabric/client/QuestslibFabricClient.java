package io.github.gcjojo.questslib.fabric.client;

import io.github.gcjojo.questslib.QuestsLib;
import net.fabricmc.api.ClientModInitializer;

public final class QuestslibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        QuestsLib.initClient();
    }
}
