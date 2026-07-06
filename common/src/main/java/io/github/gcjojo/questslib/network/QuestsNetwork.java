package io.github.gcjojo.questslib.network;

import dev.architectury.networking.NetworkManager;
import io.github.gcjojo.liblib.LibLib;
import io.github.gcjojo.questslib.QuestsLib;
import io.github.gcjojo.questslib.client.gui.QuestScreen;
import io.github.gcjojo.questslib.quests.PlayerQuestData;
import io.github.gcjojo.questslib.quests.QuestManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class QuestsNetwork {

    public static final ResourceLocation OPEN_QUESTS_SCREEN_PACKET_ID = ResourceLocation.tryBuild(QuestsLib.MOD_ID, "open_quests_screen");
    public static final ResourceLocation SEND_QUESTS_DATA_PACKET_ID = ResourceLocation.tryBuild(QuestsLib.MOD_ID, "send_quests_data");

    public static void registerPackets() {

    }

    public static void registerClientPackets() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OPEN_QUESTS_SCREEN_PACKET_ID, ((buf, context) -> {
            Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new QuestScreen(Component.literal("Quests"))));
        }));

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SEND_QUESTS_DATA_PACKET_ID, (buf, context) -> {
            try {
                Map<ResourceLocation, PlayerQuestData> map = buf.readMap(FriendlyByteBuf::readResourceLocation, PlayerQuestData.READER);
                QuestManager.PlayerQuestDataMap playerQuestDataMap = new QuestManager.PlayerQuestDataMap();
                playerQuestDataMap.putAll(map);
                QuestManager.setPlayerQuests(context.getPlayer(), playerQuestDataMap);
            } catch (Exception e) {
                LibLib.printException("Couldn't receive quest data from server !", e);
            }
        });
    }
}
