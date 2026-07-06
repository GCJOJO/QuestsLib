package io.github.gcjojo.questslib.client.gui;

import io.github.gcjojo.liblib.client.gui.GuiScreen;
import io.github.gcjojo.liblib.client.gui.elements.*;
import io.github.gcjojo.liblib.math.Color;
import io.github.gcjojo.questslib.quests.Quest;
import io.github.gcjojo.questslib.quests.QuestManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;

public class QuestScreen extends GuiScreen {
    GuiColorRect backgroundRect;
    GuiColorRect headerRect;
    GuiColorRect dividerRect;
    GuiSliderContainer questsContainer;

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

        backgroundRect = new GuiColorRect(this, 0, 0, this.width, this.height, new Color(40, 44, 52, 255));
        addElement(backgroundRect);

        dividerRect = new GuiColorRect(this, (int) (this.width * 0.49f), 0, (int) (this.width * 0.51f), this.height, new Color(32, 35, 41, 255));
        addElement(dividerRect);

        GuiText text = new GuiText(this, Component.literal("Quests"));
        text.setPosition(new Vec2(10, 5));
        text.setScale(new Vec2(2, 2));

        headerRect = new GuiColorRect(this, 0, 0, (int) (this.width * 0.49f), (int) text.getHeight() + 6, new Color(32, 35, 41, 255));
        addElement(headerRect);
        addElement(text);

        questsContainer = new GuiSliderContainer(this, (int) (this.width * 0.49f) - 20, this.height - 40, GuiBoxContainer.BoxDirection.Vertical);
        questsContainer.setPosition(new Vec2(10, 20));
        addElement(questsContainer);

        QuestManager.getPlayerQuests(Minecraft.getInstance().player).forEach((questId, questData) -> {
            QuestManager.getQuest(questId).ifPresent(quest -> {
                GuiButton questButton = new GuiButton(this, quest.getQuestName(), () -> onQuestButton(quest));
                questButton.setButtonWidth((int) questsContainer.getContainerWidth());
                questButton.setDrawOffset(new Vec2(questButton.getButtonWidth() * 0.5f, questButton.getButtonHeight() * 0.5f));
                questsContainer.addChild(questButton);
            });
        });

        for (int i = 0; i <= 49; i++) {
            questsContainer.addChild(new GuiText(this, Component.literal(String.format("Text %s", i + 1))));
        }
    }

    private void onQuestButton(Quest quest) {
    }
}
