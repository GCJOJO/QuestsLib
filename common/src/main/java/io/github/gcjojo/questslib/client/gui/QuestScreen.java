package io.github.gcjojo.questslib.client.gui;

import io.github.gcjojo.liblib.client.gui.GuiScreen;
import io.github.gcjojo.liblib.client.gui.elements.*;
import io.github.gcjojo.liblib.math.Color;
import io.github.gcjojo.questslib.quests.PlayerQuestData;
import io.github.gcjojo.questslib.quests.Quest;
import io.github.gcjojo.questslib.quests.QuestsManager;
import io.github.gcjojo.questslib.quests.enums.QuestCompletionState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;

public class QuestScreen extends GuiScreen {
    GuiColorRect backgroundRect;
    GuiColorRect dividerRect;
    GuiScrollbarContainer questsContainer;
    GuiScrollbarContainer viewingQuestContainer;

    public QuestScreen(Component component) {
        super(component);
    }

    @Override
    public boolean isRenderBackground() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        clearElements();

        backgroundRect = new GuiColorRect(this, 0, 0, this.width, this.height, new Color(40, 44, 52, 128));
        addElement(backgroundRect);

        dividerRect = new GuiColorRect(this, (int) (this.width * 0.50f), 0, this.width, this.height, new Color(22, 25, 31, 200));
        addElement(dividerRect);

        GuiText text = new GuiText(this, Component.literal("Quests"));
        text.setPosition(new Vec2(10, 5));
        text.setScale(new Vec2(2, 2));
        addElement(text);

        questsContainer = new GuiScrollbarContainer(this, (int) (this.width * 0.49f) - 20, this.height - 40, GuiBoxContainer.BoxDirection.Vertical);
        questsContainer.setPosition(new Vec2(10, 30));
        addElement(questsContainer);

        GuiText currentQuestsText = new GuiText(this, Component.literal("Current Quests"));
        currentQuestsText.setScale(new Vec2(1.5f, 1.5f));
        questsContainer.addChild(currentQuestsText);

        QuestsManager.PlayerQuestDataMap completedQuests = new QuestsManager.PlayerQuestDataMap();

        QuestsManager.getPlayerQuests(Minecraft.getInstance().player).forEach((questId, questData) -> {
            if (questData.getCompletionState() == QuestCompletionState.Completed) {
                completedQuests.put(questId, questData);
                return;
            }
            QuestsManager.getQuest(questId).ifPresent(quest -> addQuestToContainer(quest, questData));
        });

        if (!completedQuests.isEmpty()) {
            GuiText completedQuestsText = new GuiText(this, Component.literal("Completed Quests"));
            completedQuestsText.setScale(new Vec2(1.5f, 1.5f));
            questsContainer.addChild(completedQuestsText);
            completedQuests.forEach((questId, questData) -> {
                QuestsManager.getQuest(questId).ifPresent(quest -> addQuestToContainer(quest, questData));
            });
        }

        for (int i = 0; i <= 49; i++) {
            questsContainer.addChild(new GuiText(this, Component.literal(String.format("Text %s", i + 1))));
        }

        viewingQuestContainer = new GuiScrollbarContainer(this, (int) (this.width * 0.49f), this.height - 40, GuiBoxContainer.BoxDirection.Vertical);
        viewingQuestContainer.setPosition(new Vec2(this.width * 0.51f, 30));

        addElement(viewingQuestContainer);
    }

    private void onQuestButton(Quest quest, PlayerQuestData questData) {
        viewingQuestContainer.clearChildren();
        GuiText questName = new GuiText(this, quest.getQuestName());
        questName.setScale(new Vec2(1.25f, 1.25f));

        GuiText questDescription = new GuiText(this, quest.getQuestDescription());

        float progression = questData.getCompletionState() == QuestCompletionState.Completed ? 100.0f : (float) questData.getCurrentTaskIndex() / (float) quest.getTaskAmount() * 100;
        GuiProgressBar.BarColor progressBarColor = questData.getCompletionState() == QuestCompletionState.Completed ? GuiProgressBar.BarColor.Green : GuiProgressBar.BarColor.Orange;

        GuiProgressBar questProgression = new GuiProgressBar(this, 0.0f, 100.0f, progression, 6, (int) (this.width * 0.5f - 40),
                GuiProgressBar.ProgressBarDirection.Horizontal, progressBarColor, GuiProgressBar.BarColor.Gray);

        viewingQuestContainer.addChild(questName);
        viewingQuestContainer.addChild(questDescription);
        viewingQuestContainer.addChild(questProgression);
    }

    private void addQuestToContainer(Quest quest, PlayerQuestData questData) {
        GuiButton questButton = new GuiButton(this, quest.getQuestName(), () -> onQuestButton(quest, questData));
        questButton.setButtonWidth((int) questsContainer.getContainerWidth());
        questButton.setDrawOffset(new Vec2(questButton.getButtonWidth() * 0.5f, questButton.getButtonHeight() * 0.5f));

        if (questData.getCompletionState() == QuestCompletionState.Completed)
            questButton.getText().setColor(new Color(128, 128, 128, 255));

        questsContainer.addChild(questButton);
    }
}
