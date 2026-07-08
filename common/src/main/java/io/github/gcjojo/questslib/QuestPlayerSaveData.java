package io.github.gcjojo.questslib;

import io.github.gcjojo.liblib.utils.PlayerSaveData;
import io.github.gcjojo.questslib.quests.PlayerQuestData;
import io.github.gcjojo.questslib.quests.QuestsManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

@Getter
@Setter
public class QuestPlayerSaveData extends PlayerSaveData {
    QuestsManager.PlayerQuestDataMap questData = new QuestsManager.PlayerQuestDataMap();

    @Override
    public CompoundTag serialize() {
        CompoundTag nbt = new CompoundTag();
        questData.forEach((questId, data) -> nbt.put(questId.toString(), data.serialize()));
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt) {
        nbt.getAllKeys().forEach(questIdString -> {
            ResourceLocation questId = ResourceLocation.tryParse(questIdString);
            PlayerQuestData data = PlayerQuestData.deserialize(questId, nbt.getCompound(questIdString));
            questData.putIfAbsent(questId, data);
        });
    }
}
