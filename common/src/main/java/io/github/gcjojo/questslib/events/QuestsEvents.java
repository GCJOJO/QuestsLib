package io.github.gcjojo.questslib.events;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public interface QuestsEvents {
    public Event<TaskProgression> TASK_PROGRESSION = EventFactory.createLoop();
    public Event<TaskCompleted> TASK_COMPLETED = EventFactory.createLoop();
    public Event<QuestCompleted> QUEST_COMPLETED = EventFactory.createLoop();

    interface TaskProgression {
        void taskProgression(Player player, ResourceLocation questId, ResourceLocation taskId);
    }

    interface TaskCompleted {
        void taskCompleted(Player player, ResourceLocation taskId);
    }

    interface QuestCompleted {
        void questCompleted(Player player, ResourceLocation questId);
    }

}
