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
import de.markusbordihn.advancementstracker.client.gui.screens.AdvancementsTrackerScreen;
import org.jetbrains.annotations.NotNull;

public class AdvancementCategoryPanel
    extends ObjectSelectionList<AdvancementCategoryPanel.RootAdvancementEntry> {

  private final int listLeft;
  private final int listWidth;

  private AdvancementsTrackerScreen parent;

  public AdvancementCategoryPanel(AdvancementsTrackerScreen parent, int listWidth, int top,
      int listLeft, int bottom) {//TODO: Should we base the height off of what bottom would be rather than parent.height?
    super(parent.getMinecraftInstance(), listWidth, parent.height, top, parent.getFontRenderer().lineHeight * 3 + 8);
    this.parent = parent;
    this.listWidth = listWidth;
    this.listLeft = listLeft;
    this.setX(listLeft);
    this.setRenderBackground(false);
  }

  @Override
  protected void renderSelection(@NotNull GuiGraphics graphics, int y, int entryWidth, int entryHeight, int borderColor, int fillColor) {
  }

  public void refreshList() {
    this.clearEntries();

    // Build root advancements list.
    parent.buildRootAdvancementsList(this::addEntry,
        mod -> new RootAdvancementEntry(mod, this.parent));

    // Pre-select first entry if we have nothing selected.
    if (this.getSelected() == null && parent.getSelectedRootAdvancement() != null) {
      this.refreshSelection();
      for (int i = 0; i < getItemCount(); i++) {
        if (getEntry(i) != null && getEntry(i).advancementEntry != null
            && getEntry(i).advancementEntry == parent.getSelectedRootAdvancement()) {
          this.setSelected(getEntry(i));
        }
      }
    }
    if (this.getSelected() == null && getItemCount() > 0 && getEntry(0) != null) {
      this.refreshSelection();
      RootAdvancementEntry rootAdvancementEntry = getEntry(0);
      parent.setSelectedRootAdvancement(rootAdvancementEntry);
    }
  }

  private void refreshSelection() {
    RootAdvancementEntry rootAdvancementEntry = this.getSelected();
    if (rootAdvancementEntry != null) {
      this.setSelected(rootAdvancementEntry);
      this.ensureVisible(rootAdvancementEntry);
    }
  }

  public class RootAdvancementEntry extends ObjectSelectionList.Entry<RootAdvancementEntry> {

    private static final ResourceLocation miscTexture =
        new ResourceLocation(Constants.MOD_ID, "textures/gui/misc.png");

    private final AdvancementEntry advancementEntry;
    private final AdvancementsTrackerScreen parent;
    private final Font font;
    private final int descriptionColor;
    private final int iconWidth;
    private final int titleColor;

    private FormattedCharSequence titleParts;
    private List<FormattedCharSequence> descriptionParts;
    private boolean isSelected = false;
    private boolean isMouseOvered = false;
    private int maxFontWidth;
    private int titleWidth;

    RootAdvancementEntry(AdvancementEntry advancementEntry, AdvancementsTrackerScreen parent) {
      this.advancementEntry = advancementEntry;
      this.descriptionColor = advancementEntry.getDescriptionColor();
      this.font = parent.getFontRenderer();
      this.parent = parent;
      this.titleColor = advancementEntry.getTitleColor();

      // Do expensive pre-calculation for the render
      this.iconWidth = 14;
      this.maxFontWidth = listWidth - iconWidth - 4;
      this.titleWidth =
          font.width(advancementEntry.getTitle()) > maxFontWidth ? maxFontWidth - 6 : maxFontWidth;
      this.titleParts = Language.getInstance().getVisualOrder(
          FormattedText.composite(font.substrByWidth(advancementEntry.getTitle(), titleWidth)));
      this.descriptionParts = font.split(advancementEntry.getDescription(), maxFontWidth);
    }

    public AdvancementEntry getAdvancementEntry() {
      return advancementEntry;
    }

    private void renderBackground(GuiGraphics guiGraphics, int top, int entryWidth,
        int entryHeight) {
      if (this.advancementEntry.getBackground() == null) {
        return;
      }
      if (this.isSelected || this.isMouseOvered) {
        RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1);
      } else {
        RenderSystem.setShaderColor(0.4f, 0.4f, 0.4f, 1);
      }
      guiGraphics.pose().pushPose();
      guiGraphics.blit(this.advancementEntry.getBackground(), getX() + 1, top - 1, 0, 0,
          entryWidth - 2, entryHeight + 2, 16, 16);
      guiGraphics.pose().popPose();
    }

    private void renderIcon(GuiGraphics guiGraphics, int top) {
      if (this.advancementEntry.getIcon() == null) {
        return;
      }
      guiGraphics.renderItem(this.advancementEntry.getIcon(), getX() + 1, top + 6);
    }

    private void renderTrackedAdvancementsStatus(GuiGraphics guiGraphics, int top, int left,
        int entryWidth) {
      if (TrackedAdvancementsManager.hasTrackedAdvancement(this.advancementEntry)) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(0.5f, 0.5f, 0.5f);
        guiGraphics.blit(miscTexture, (left + entryWidth - 12) * 2, (top + 1) * 2, 81, 6, 16, 16,
            256, 256);
        guiGraphics.pose().popPose();
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

    @Override
    public Component getNarration() {
      return Component.translatable("narrator.select", advancementEntry.getTitleString());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int entryIdx, int top, int left, int entryWidth,
        int entryHeight, int mouseX, int mouseY, boolean isFocused, float partialTick) {

      // Selection state
      this.isSelected = isSelectedItem(entryIdx);

      // Mouse over state
      this.isMouseOvered = this.isMouseOver(mouseX, mouseY);

      // Positions
      int textPositionLeft = left + iconWidth;

      // Background
      this.renderBackground(guiGraphics, top, entryWidth, entryHeight);

      // Select and mouse over effects
      if (this.isSelected) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1);
      } else {
        if (this.isMouseOvered) {
          RenderSystem.setShaderColor(0.9f, 0.9f, 0.9f, 1);
        } else {
          RenderSystem.setShaderColor(0.7f, 0.7f, 0.7f, 1);
        }
      }

      // Icon
      this.renderIcon(guiGraphics, top);

      // Tracked Advancements
      this.renderTrackedAdvancementsStatus(guiGraphics, top, left, entryWidth);

      // Title (only one line)
      int currentTitleColor = isSelected ? 0xFFFF00 : this.titleColor;
      guiGraphics.drawString(this.font, this.titleParts, textPositionLeft + 3, top + 1,
          currentTitleColor);
      if (titleWidth != maxFontWidth) {
        guiGraphics.drawString(this.font, Constants.ELLIPSIS, textPositionLeft + titleWidth,
            top + 1, currentTitleColor, false);
      }

      // Description (two lines)
      int descriptionLines = 1;
      for (FormattedCharSequence descriptionPart : this.descriptionParts) {
        int descriptionTopPosition = top + (2 + font.lineHeight) * descriptionLines;
        guiGraphics.drawString(this.font, descriptionPart, textPositionLeft + 3,
            descriptionTopPosition, this.descriptionColor);
        if (this.descriptionParts.size() == 3 && descriptionLines == 2) {
          guiGraphics.drawString(this.font, Constants.ELLIPSIS,
              textPositionLeft + (font.width(descriptionPart) < maxFontWidth - 6
                  ? font.width(descriptionPart) + 6
                  : maxFontWidth - 6),
              descriptionTopPosition, 0xFFFFFF, false);
          break;
        }
        descriptionLines++;
      }

      // Decoration
      this.renderDecoration(guiGraphics, top, entryWidth, entryHeight);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
        parent.setSelectedRootAdvancement(this);
        setSelected(this);
      }
      return super.mouseClicked(mouseX, mouseY, button);
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
