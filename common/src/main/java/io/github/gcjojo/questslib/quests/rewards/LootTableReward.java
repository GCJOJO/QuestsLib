package io.github.gcjojo.questslib.quests.rewards;

import com.google.gson.JsonObject;
import io.github.gcjojo.questslib.QuestsLib;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class LootTableReward extends QuestReward {
    public ResourceLocation lootTableId;
    public int maxGivenStacks = -1;

    public LootTableReward(JsonObject json) {
        super(json);

        if (json.has("loot_table"))
            lootTableId = ResourceLocation.tryParse(json.get("loot_table").getAsString());
        if (json.has("max_given_stacks"))
            maxGivenStacks = json.get("max_given_stacks").getAsInt();
    }

    @Override
    public void rewardPlayer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return;

        ServerLevel level = serverPlayer.serverLevel();
        Inventory playerInv = player.getInventory();
        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, lootTableId));

        if (lootTable == LootTable.EMPTY) {
            QuestsLib.getLogger().warn("{} is an invalid loot table ", lootTableId);
            return;
        }

        var builder = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, player.position());
        var givenItems = lootTable.getRandomItems(builder.create(LootContextParamSets.COMMAND));

        if (maxGivenStacks >= 1)
            givenItems = new ObjectArrayList<>(givenItems.stream().limit(maxGivenStacks).toList());

        givenItems.forEach(playerInv::add);
    }
}
