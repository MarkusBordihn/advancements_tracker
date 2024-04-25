/**
 * Copyright 2022 Markus Bordihn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.markusbordihn.advancementstracker.client.gui.screens;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import de.markusbordihn.advancementstracker.Constants;
import de.markusbordihn.advancementstracker.client.advancements.AdvancementEntry;
import de.markusbordihn.advancementstracker.client.advancements.AdvancementEntryProgress;
import de.markusbordihn.advancementstracker.client.gui.panel.AdvancementInfoPanel;
import org.jetbrains.annotations.NotNull;

public class AdvancementDetailScreen extends Screen {

  private static final ResourceLocation windowBackground =
      new ResourceLocation("textures/gui/advancements/window.png");

  private AdvancementEntry advancementEntry;
  private AdvancementEntryProgress progress;
  private AdvancementInfoPanel advancementInfoPanel;

  private int maxWidth;
  private int maxHeight;
  private int top;
  private int left;

  public AdvancementDetailScreen(AdvancementEntry advancementEntry) {
    this(advancementEntry.getTitle());
    this.advancementEntry = advancementEntry;
    this.progress = advancementEntry.getProgress();
  }

  protected AdvancementDetailScreen(Component component) {
    super(component);
  }

  private List<String> prepareInfoContent() {
    List<String> info = new ArrayList<>();

    // Display description.
    info.add(advancementEntry.getDescriptionString());

    // Display criteria information.
    if (this.progress.getRemainingCriteriaNumber() > 0
        || this.progress.getCompletedCriteriaNumber() > 0) {
      info.add(" ");
      info.add(
          Component.translatable(Constants.ADVANCEMENTS_SCREEN_PREFIX + "criteria").getString());
      if (this.progress.getRemainingCriteriaNumber() > 0) {
        for (String remainingCriteria : this.progress.getRemainingCriteriaHumanReadable()) {
          info.add("❌ " + remainingCriteria);
        }
      }

      if (this.progress.getCompletedCriteriaNumber() > 0) {
        for (String completedCriteria : this.progress.getCompletedCriteriaHumanReadable()) {
          info.add("✔ " + completedCriteria);
        }
      }
    }

    // Display reward information.
    if (this.advancementEntry.hasExperienceReward() || this.advancementEntry.hasLootReward()
        || this.advancementEntry.hasRecipesReward()) {
      info.add(" ");
      info.add(
          Component.translatable(Constants.ADVANCEMENTS_SCREEN_PREFIX + "rewards").getString());

      if (this.advancementEntry.hasExperienceReward()) {
        info.add("+ " + Component.translatable(Constants.ADVANCEMENTS_SCREEN_PREFIX + "experience",
            this.advancementEntry.getRewardsExperience()).getString());
      }

      if (this.advancementEntry.hasLootReward()) {
        for (ResourceLocation loot : this.advancementEntry.getRewardsLoot()) {
          info.add("+ " + loot.toString());
        }
      }

      if (this.advancementEntry.hasRecipesReward()) {
        for (ResourceLocation recipe : this.advancementEntry.getRewardsRecipes()) {
          info.add("+ " + recipe.toString());
        }
      }
    }

    return info;
  }

  @Override
  public void init() {
    maxHeight = Math.min(height - 30, 260);
    maxWidth = 252;
    left = (width - maxWidth) / 2;
    top = (height - maxHeight) / 2;
    this.advancementInfoPanel =
        new AdvancementInfoPanel(minecraft, maxWidth - 18, maxHeight - 38, top + 18, left + 3);
    this.advancementInfoPanel.setInfo(prepareInfoContent());
    this.addRenderableWidget(this.advancementInfoPanel);
  }

  @Override
  public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    // Make sure we use a higher z-index.
    guiGraphics.pose().pushPose();
    guiGraphics.pose().translate(0, 0, 201);

    // Render advancement screen.
    super.render(guiGraphics, mouseX, mouseY, partialTick);

    // Render advancement info panel.
    if (this.advancementInfoPanel != null) {
      this.advancementInfoPanel.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    guiGraphics.pose().popPose();
  }

  @Override
  public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

    // Make sure we use a higher z-index.
    guiGraphics.pose().pushPose();
    guiGraphics.pose().translate(0, 0, 201);

    // Background and frame.
    int heightPerPart = maxHeight / 2;
    RenderSystem.enableBlend();

    // Frame will be constructed in two parts, top and bottom.
    guiGraphics.blit(windowBackground, left, top, 0, 0, maxWidth, heightPerPart);
    guiGraphics.blit(windowBackground, left, top + heightPerPart, 0, 150 - heightPerPart, maxWidth,
        heightPerPart + 10);

    // Background with gradient.
    guiGraphics.fillGradient(left + 9, top + 18, left + maxWidth - 9, top + maxHeight - 20,
        -1072689136, -804253680);
    RenderSystem.disableBlend();

    // Title
    guiGraphics.drawString(this.font, this.title, left + 22, top + 6,
        advancementEntry.getTitleColor());

    // Icon
    if (this.advancementEntry.getIcon() != null && minecraft != null) {
      guiGraphics.renderItem(this.advancementEntry.getIcon(), left + 4, top + 1);
    }

    guiGraphics.pose().popPose();
  }

  @Override
  public boolean isMouseOver(double mouseX, double mouseY) {
    return mouseY >= this.top && mouseY <= this.top + this.maxHeight && mouseX >= this.left
        && mouseX <= this.left + this.maxWidth;
  }

}
