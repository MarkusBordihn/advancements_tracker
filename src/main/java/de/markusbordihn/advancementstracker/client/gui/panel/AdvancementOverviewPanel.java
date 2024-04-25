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

package de.markusbordihn.advancementstracker.client.gui.panel;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.locale.Language;

import de.markusbordihn.advancementstracker.Constants;
import de.markusbordihn.advancementstracker.client.advancements.AdvancementEntry;
import de.markusbordihn.advancementstracker.client.advancements.TrackedAdvancementsManager;
import de.markusbordihn.advancementstracker.client.gui.components.AdvancementTooltip;
import de.markusbordihn.advancementstracker.client.gui.screens.AdvancementsTrackerScreen;
import org.jetbrains.annotations.NotNull;

public class AdvancementOverviewPanel
    extends ObjectSelectionList<AdvancementOverviewPanel.ChildAdvancementEntry> {

  private final int listLeft;
  private final int listWidth;

  private AdvancementsTrackerScreen parent;
  private AdvancementTooltip advancementTooltip;

  public AdvancementOverviewPanel(AdvancementsTrackerScreen parent, int listWidth, int top,
      int listLeft, int bottom) {
    //TODO: Should we base the height off of what bottom would be rather than parent.height?
    super(parent.getMinecraftInstance(), listWidth, parent.height, top, parent.getFontRenderer().lineHeight * 4 + 12);
    this.parent = parent;
    this.listWidth = listWidth;
    this.listLeft = listLeft;
    this.setX(listLeft + 1);
    this.setRenderBackground(false);
  }

  @Override
  protected void renderSelection(@NotNull GuiGraphics graphics, int y, int entryWidth, int entryHeight, int borderColor, int fillColor) {
  }

  public void refreshList() {
    this.clearEntries();

    // Build child advancements list.
    parent.buildChildAdvancementsList(this::addEntry,
        mod -> new ChildAdvancementEntry(mod, this.parent));

    // Reset scroll bar
    if (getScrollAmount() > 0) {
      setScrollAmount(0);
    }

    // Reset tooltip
    this.advancementTooltip = null;
  }

  public void setAdvancementTooltip(AdvancementTooltip advancementTooltip) {
    this.advancementTooltip = advancementTooltip;
  }

  public class ChildAdvancementEntry extends ObjectSelectionList.Entry<ChildAdvancementEntry> {

    private static final ResourceLocation miscTexture =
        new ResourceLocation(Constants.MOD_ID, "textures/gui/misc.png");

    private final AdvancementEntry advancementEntry;
    private final AdvancementsTrackerScreen parent;
    private final AdvancementTooltip advancementTooltip;
    private final Font font;
    private final boolean isDone;
    private final int completedCriteriaNumber;
    private final int descriptionColor;
    private final int iconWidth;
    private final int remainingCriteriaNumber;
    private final int titleColor;

    private FormattedCharSequence titleParts;
    private List<FormattedCharSequence> descriptionParts;
    private boolean isMouseOvered = false;

    // Cached positions and sizes
    private int maxFontWidth;
    private int progressWidth = 182;
    private int relativeLeftPosition;
    private int relativeTopPosition;
    private int titleWidth;

    ChildAdvancementEntry(AdvancementEntry advancementEntry, AdvancementsTrackerScreen parent) {
      this.advancementEntry = advancementEntry;
      this.advancementTooltip = new AdvancementTooltip(advancementEntry);
      this.completedCriteriaNumber = advancementEntry.getProgress().getCompletedCriteriaNumber();
      this.descriptionColor = advancementEntry.getDescriptionColor();
      this.font = parent.getFontRenderer();
      this.isDone = advancementEntry.getProgress().isDone();
      this.parent = parent;
      this.remainingCriteriaNumber = advancementEntry.getProgress().getRemainingCriteriaNumber();
      this.titleColor = advancementEntry.getTitleColor();

      // Do expensive pre-calculation for the render
      this.iconWidth = 18;
      this.maxFontWidth = listWidth - this.iconWidth - 4;
      this.titleWidth =
          font.width(advancementEntry.getTitle()) > this.maxFontWidth ? this.maxFontWidth - 6
              : this.maxFontWidth;
      this.titleParts = Language.getInstance().getVisualOrder(
          FormattedText.composite(font.substrByWidth(advancementEntry.getTitle(), titleWidth)));
      this.descriptionParts = font.split(advancementEntry.getDescription(), this.maxFontWidth);
    }

    public AdvancementEntry getAdvancementEntry() {
      return advancementEntry;
    }

    private void renderBackground(GuiGraphics guiGraphics, int top, int entryWidth,
        int entryHeight) {
      if (this.advancementEntry.getBackground() == null) {
        return;
      }
      if (this.isMouseOvered) {
        RenderSystem.setShaderColor(0.6f, 0.6f, 0.6f, 1);
      } else {
        RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1);
      }
      guiGraphics.pose().pushPose();
      guiGraphics.blit(this.advancementEntry.getBackground(), getX() + 1, top - 1, 0, 0,
          entryWidth - 2, entryHeight + 1, 16, 16);
      guiGraphics.pose().popPose();
    }

    private void renderIcon(GuiGraphics guiGraphics, int top) {
      if (this.advancementEntry.getIcon() == null) {
        return;
      }
      guiGraphics.renderItem(this.advancementEntry.getIcon(), getX() + 3, top + 2);
    }

    private void renderRewards(GuiGraphics guiGraphics, int top, int left, int entryWidth) {
      if (!this.advancementEntry.hasRewards()) {
        return;
      }

      float scaling = 0.70f;
      int positionLeft = Math.round((left + entryWidth - 14) / scaling);
      int positionTop = Math.round((top - 1) / scaling);

      // Experience Reward
      if (this.advancementEntry.hasExperienceReward()) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scaling, scaling, scaling);
        guiGraphics.blit(miscTexture, positionLeft, positionTop, 102, 7, 12, 15, 256, 256);
        guiGraphics.pose().popPose();
        positionLeft -= 16;
      }

      // Loot Reward
      if (this.advancementEntry.hasLootReward()) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scaling, scaling, scaling);
        guiGraphics.blit(miscTexture, positionLeft, positionTop, 137, 6, 14, 16, 256, 256);
        guiGraphics.pose().popPose();
        positionLeft -= 16;
      }

      // Recipe Reward
      if (this.advancementEntry.hasRecipesReward()) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scaling, scaling, scaling);
        guiGraphics.blit(miscTexture, positionLeft, positionTop, 118, 6, 14, 16, 256, 256);
        guiGraphics.pose().popPose();
        positionLeft -= 16;
      }
    }

    protected static final ResourceLocation EXPERIENCE_BAR_BACKGROUND_SPRITE = new ResourceLocation("hud/experience_bar_background");
    protected static final ResourceLocation EXPERIENCE_BAR_PROGRESS_SPRITE = new ResourceLocation("hud/experience_bar_progress");

    private void renderProgress(GuiGraphics guiGraphics, int top, int entryWidth, int iconWidth) {
      int progressPositionLeft = getX() + iconWidth + 5;
      int progressPositionTop = top + 33;

      // Render empty bar.
      guiGraphics.blitSprite(EXPERIENCE_BAR_BACKGROUND_SPRITE, progressPositionLeft, progressPositionTop, progressWidth, 5);

      // Render progress bar and numbers.
      if (this.remainingCriteriaNumber > 0 || this.isDone) {
        int progressTotal = this.completedCriteriaNumber + this.remainingCriteriaNumber;
        int progressDone = this.completedCriteriaNumber;
        RenderSystem.setShaderColor(1, 1, 1, 1);

        guiGraphics.blitSprite(EXPERIENCE_BAR_PROGRESS_SPRITE, 182, 5, 0, 0, progressPositionLeft, progressPositionTop, this.isDone ? progressWidth : (progressWidth / progressTotal * progressDone), 5);

        // Only render numbers if we have enough space.
        if (entryWidth > progressWidth + 42) {
          float scaling = 0.75f;
          float positionScaling = 1.33f;
          guiGraphics.pose().pushPose();
          guiGraphics.pose().scale(scaling, scaling, scaling);
          guiGraphics.drawString(this.font, advancementEntry.getProgress().getProgressString(),
              Math.round((progressPositionLeft + progressWidth + 5) * positionScaling),
              Math.round((progressPositionTop) * positionScaling),
              this.remainingCriteriaNumber >= 1 ? ChatFormatting.YELLOW.getColor()
                  : ChatFormatting.GREEN.getColor());
          guiGraphics.pose().popPose();
        }
      }
    }

    private void renderDecoration(GuiGraphics guiGraphics, int top, int entryWidth,
        int entryHeight) {
      int topPosition = top - 2;
      int leftPosition = getX();
      int rightPosition = leftPosition + entryWidth - 2;
      int bottomPosition = top + entryHeight;
      guiGraphics.pose().pushPose();
      guiGraphics.blit(miscTexture, leftPosition, topPosition, 0, 0, entryWidth - 1, 1,
          entryWidth - 1, 256);
      guiGraphics.blit(miscTexture, rightPosition, topPosition + 1, 255, 0, 1, entryHeight + 2, 256,
          entryHeight);
      guiGraphics.blit(miscTexture, leftPosition, bottomPosition, 0, 255, entryWidth - 1, 1,
          entryWidth - 1, 256);
      guiGraphics.blit(miscTexture, leftPosition, topPosition + 1, 0, 0, 1, entryHeight + 2, 256,
          entryHeight);
      guiGraphics.pose().popPose();
    }

    private void renderTrackingCheckbox(GuiGraphics guiGraphics, int top, int left) {
      int iconPosition = 22;
      if (this.isDone) {
        iconPosition = 3;
      } else if (this.advancementEntry.isTracked()) {
        iconPosition = 42;
      }
      guiGraphics.pose().pushPose();
      guiGraphics.blit(miscTexture, left + 2, top + 27, iconPosition, 6, 15, 15, 256, 256);
      guiGraphics.pose().popPose();
    }

    @Override
    public Component getNarration() {
      return Component.translatable("narrator.select", advancementEntry.getTitleString());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int entryIdx, int top, int left, int entryWidth,
        int entryHeight, int mouseX, int mouseY, boolean isFocused, float partialTick) {

      // Mouse over state
      this.isMouseOvered = this.isMouseOver(mouseX, mouseY);

      // Positions
      int textPositionLeft = left + iconWidth;

      // Update relative position for other calculations like mouse clicks.
      this.relativeLeftPosition = left;
      this.relativeTopPosition = top;

      // Background
      this.renderBackground(guiGraphics, top, entryWidth, entryHeight);

      // Mouse over effects
      if (this.isMouseOvered) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1);
      } else {
        RenderSystem.setShaderColor(0.9f, 0.9f, 0.9f, 1);
      }

      // Icon
      this.renderIcon(guiGraphics, top);

      // Title (only one line)
      guiGraphics.drawString(this.font, titleParts, textPositionLeft + 3, top + 1, this.titleColor);
      if (this.titleWidth != maxFontWidth) {
        guiGraphics.drawString(this.font, Constants.ELLIPSIS, textPositionLeft + this.titleWidth,
            top + 1, this.titleColor, false);
      }

      // Description (max. two lines)
      int descriptionLines = 1;
      for (FormattedCharSequence descriptionPart : this.descriptionParts) {
        int descriptionTopPosition = top + (2 + font.lineHeight) * descriptionLines;
        guiGraphics.drawString(this.font, descriptionPart, textPositionLeft + 3,
            descriptionTopPosition, this.descriptionColor);
        if (this.descriptionParts.size() >= 3 && descriptionLines == 2) {
          guiGraphics.drawString(this.font, Constants.ELLIPSIS,
              textPositionLeft + (font.width(descriptionPart) < maxFontWidth - 6
                  ? font.width(descriptionPart) + 6
                  : maxFontWidth - 6),
              descriptionTopPosition, this.descriptionColor, false);
          break;
        }
        descriptionLines++;
      }

      // Rewards
      this.renderRewards(guiGraphics, top, left, entryWidth);

      // Progress
      this.renderProgress(guiGraphics, top, entryWidth, iconWidth);

      // Decoration
      this.renderDecoration(guiGraphics, top, entryWidth, entryHeight);

      // Checkbox for enabling tracking
      this.renderTrackingCheckbox(guiGraphics, top, left);

      // Additional Tooltips with mouse over
      if (super.isMouseOver(mouseX, mouseY)) {
        setAdvancementTooltip(this.advancementTooltip);
      }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      // We only care for mouse click events with button 0.
      if (button == 0) {
        double relativeX = mouseX - this.relativeLeftPosition;
        double relativeY = mouseY - this.relativeTopPosition;
        if ((relativeX > 3 && relativeX < 15) && (relativeY > 27 && relativeY < 42)) {
          TrackedAdvancementsManager.toggleTrackedAdvancement(this.getAdvancementEntry());
        } else {
          parent.setSelectedChildAdvancement(this);
          parent.showAdvancementDetail(true);
          setSelected(this);
        }
      }
      return super.mouseClicked(mouseX, mouseY, button);
    }
  }

  @Override
  public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

    // Render tool tips separate to make sure they are fully visible.
    if (this.advancementTooltip != null) {
      this.advancementTooltip = null;
    }
  }

  @Override
  public boolean isMouseOver(double mouseX, double mouseY) {
    return !parent.showingAdvancementDetail() && mouseY >= getY() && mouseY <= getBottom()
           && mouseX >= getX() && mouseX <= getRight() + 5;
  }

  @Override
  protected int getScrollbarPosition() {
    return this.listWidth + this.listLeft;
  }

  @Override
  public int getRowWidth() {
    return this.listWidth;
  }

}
