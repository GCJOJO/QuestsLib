package io.github.gcjojo.questslib.quests.tasks;

import com.google.gson.JsonObject;
import io.github.gcjojo.liblib.utils.MathUtils;
import io.github.gcjojo.questslib.quests.QuestTask;
import io.github.gcjojo.questslib.quests.enums.StatTaskType;
import io.github.gcjojo.questslib.quests.enums.TaskType;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

@Getter
public class StatTask extends QuestTask {
    protected ResourceLocation targetId;
    protected int amount;
    protected StatTaskType statType;

    public StatTask(ResourceLocation taskId, MutableComponent name, MutableComponent description,
                    ResourceLocation targetId, int amount, StatTaskType type) {
        super(taskId, name, description);
        this.targetId = targetId;
        this.amount = amount;
        this.statType = type;

        this.taskDataClass = StatTaskData.class;
    }

    public StatTask(JsonObject object) {
        super(object);

        this.statType = StatTaskType.None;
        this.targetId = null;
        this.amount = 1;

        if (object.has("stat_type"))
            this.statType = StatTaskType.fromId(object.get("stat_type").getAsString());
        if (object.has("target"))
            this.targetId = ResourceLocation.tryParse(object.get("target").getAsString());
        if (object.has("amount"))
            this.amount = object.get("amount").getAsInt();

        this.taskDataClass = StatTaskData.class;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.Stat;
    }

    public Item getItem() {
        if (statType == StatTaskType.Item && BuiltInRegistries.ITEM.containsKey(this.targetId))
            return BuiltInRegistries.ITEM.get(this.targetId);
        return null;
    }

    public Block getBlock() {
        if (statType == StatTaskType.BrokenBlocks || statType == StatTaskType.PlacedBlocks && BuiltInRegistries.BLOCK.containsKey(this.targetId))
            return BuiltInRegistries.BLOCK.get(this.targetId);
        return null;
    }

    public EntityType<? extends Entity> getEntityType() {
        if (statType == StatTaskType.KilledMobs && BuiltInRegistries.ENTITY_TYPE.containsKey(this.targetId))
            return BuiltInRegistries.ENTITY_TYPE.get(this.targetId);
        return null;
    }

    public static class StatTaskData extends QuestTaskData<StatTask> {
        private int amount = 0;

        public StatTaskData(StatTask parentTask) {
            super(parentTask);
        }

        public void addAmount(int amount) {
            this.amount += amount;
        }

        @Override
        public float getProgression() {
            return MathUtils.clamp((float) amount / (float) this.task.getAmount(), 0.0f, 1.0f);
        }

        @Override
        public CompoundTag serialize() {
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("Amount", amount);
            return nbt;
        }

        @Override
        public void deserialize(CompoundTag nbt) {
            if (nbt.contains("Amount"))
                this.amount = nbt.getInt("Amount");
        }
    }
}
