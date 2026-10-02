package io.github.gcjojo.questslib.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import io.github.gcjojo.liblib.LibLib;
import io.github.gcjojo.questslib.QuestsLib;
import io.github.gcjojo.questslib.client.gui.QuestScreen;
import io.github.gcjojo.questslib.quests.PlayerQuestData;
import io.github.gcjojo.questslib.quests.QuestsManager;
import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class QuestsNetwork {

    public static void registerPayloadTypes() {
        if (Platform.getEnv() == EnvType.CLIENT) return;

        NetworkManager.registerS2CPayloadType(SendQuestsDataPayload.TYPE, SendQuestsDataPayload.STREAM_CODEC);
        NetworkManager.registerS2CPayloadType(OpenQuestsScreenPayload.TYPE, OpenQuestsScreenPayload.STREAM_CODEC);
    }

    public static void registerPackets() {

    }

    public static void registerClientPackets() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenQuestsScreenPayload.TYPE, OpenQuestsScreenPayload.STREAM_CODEC, ((payload, context) -> {
            QuestsLib.getLogger().info("Received open_quests_screen payload.");
            context.queue(() -> Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new QuestScreen(Component.literal("Quests")))));
        }));

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SendQuestsDataPayload.TYPE, SendQuestsDataPayload.STREAM_CODEC, (payload, context) -> {
            context.queue(() -> {
                try {
                    Map<ResourceLocation, PlayerQuestData> map = payload.questDataMap();
                    QuestsManager.PlayerQuestDataMap playerQuestDataMap = new QuestsManager.PlayerQuestDataMap();
                    playerQuestDataMap.putAll(map);
                    QuestsManager.setPlayerQuests(context.getPlayer(), playerQuestDataMap);
                } catch (Exception e) {
                    LibLib.printException("Couldn't receive quest data from server !", e);
                }
            });
        });
    }

    public record SendQuestsDataPayload(QuestsManager.PlayerQuestDataMap questDataMap) implements CustomPacketPayload {
        public static final Type<SendQuestsDataPayload> TYPE = new Type<>(ResourceLocation.tryBuild(QuestsLib.MOD_ID, "send_quests_data"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SendQuestsDataPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.map(size -> new QuestsManager.PlayerQuestDataMap(), ResourceLocation.STREAM_CODEC.cast(), PlayerQuestData.STREAM_CODEC),
                SendQuestsDataPayload::questDataMap,
                SendQuestsDataPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OpenQuestsScreenPayload() implements CustomPacketPayload {
        public static final Type<OpenQuestsScreenPayload> TYPE = new Type<>(ResourceLocation.tryBuild(QuestsLib.MOD_ID, "open_quests_screen"));

        public static final StreamCodec<RegistryFriendlyByteBuf, OpenQuestsScreenPayload> STREAM_CODEC = StreamCodec.unit(new OpenQuestsScreenPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
