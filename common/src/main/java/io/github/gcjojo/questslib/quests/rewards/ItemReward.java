package io.github.gcjojo.questslib.quests.rewards;

import com.google.gson.JsonObject;
import io.github.gcjojo.liblib.utils.MathUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemReward extends QuestReward {
    ResourceLocation itemId;
    int amount = 0;

    public ItemReward(JsonObject json) {
        super(json);
        if (json.has("item"))
            itemId = ResourceLocation.tryParse(json.get("item").getAsString());
        if (json.has("amount"))
            amount = MathUtils.clamp(json.get("amount").getAsInt(), 0, 64);
    }

    @Override
    public void rewardPlayer(Player player) {
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        Inventory playerInv = player.getInventory();
        ServerLevel level = (ServerLevel) serverPlayer.level();
        level.registryAccess().registry(Registries.ITEM).ifPresent(registry -> {
            Item item = registry.get(itemId);
            if (item == null) return;

            ItemStack stack = new ItemStack(item, amount);
            if (playerInv.getFreeSlot() == -1)
                player.drop(stack, false);
            else
                playerInv.add(stack);
        });
    }
}
