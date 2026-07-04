package io.github.gcjojo.questslib;

import io.github.gcjojo.liblib.utils.PlayerData;
import io.github.gcjojo.questslib.quests.QuestManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;

@Getter
@Setter
public class QuestPlayerData extends PlayerData {
    QuestManager.PlayerQuestDataMap questData = new QuestManager.PlayerQuestDataMap();

    @Override
    public CompoundTag serialize() {
        CompoundTag nbt = new CompoundTag();
        questData.forEach((questId, data) -> nbt.put(questId.toString(), data.serialize()));
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag data) {

    }
}
