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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.TextComponent;

import de.markusbordihn.advancementstracker.Constants;
import de.markusbordihn.advancementstracker.client.advancements.AdvancementEntry;
import de.markusbordihn.advancementstracker.client.advancements.TrackedAdvancementsManager;
import de.markusbordihn.advancementstracker.client.gui.screens.AdvancementsTrackerScreen;

public class AdvancementCategoryPanel
    extends ObjectSelectionList<AdvancementCategoryPanel.RootAdvancementEntry> {

  protected static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private final int listLeft;
  private final int listWidth;

  private AdvancementsTrackerScreen parent;

  public AdvancementCategoryPanel(AdvancementsTrackerScreen parent, int listWidth, int top,
      int listLeft, int bottom) {
    super(parent.getMinecraftInstance(), listWidth, parent.height, top, bottom,
        parent.getFontRenderer().lineHeight * 3 + 8);
    this.parent = parent;
    this.listWidth = listWidth;
    this.listLeft = listLeft;
    this.setLeftPos(listLeft);
    this.refreshList();
    this.setRenderBackground(false);
    this.setRenderSelection(false);
  }

  private static String stripControlCodes(String value) {
    return net.minecraft.util.StringUtil.stripColor(value);
  }

  public void refreshList() {
    this.clearEntries();

    // Build root advancements list.
    parent.buildRootAdvancementsList(this::addEntry,
        mod -> new RootAdvancementEntry(mod, this.parent));

    // Pre-select first entry if we have nothing selected.
    if (this.getSelected() == null && getItemCount() > 0 && getEntry(0) != null) {
      this.refreshSelection();
      RootAdvancementEntry rootAdvancementEntry = getEntry(0);
      parent.setSelectedRootAdvancement(rootAdvancementEntry);
      this.setSelected(rootAdvancementEntry);
    }
  }

  public class RootAdvancementEntry extends ObjectSelectionList.Entry<RootAdvancementEntry> {

    private static final ResourceLocation miscTexture =
        new ResourceLocation(Constants.MOD_ID, "textures/gui/misc.png");

    private final AdvancementEntry advancementEntry;
    private final AdvancementsTrackerScreen parent;
    private final Font font;
    private final ItemStack icon;
    private final ResourceLocation background;
    private final TextComponent description;
    private final TextComponent title;
    private final int descriptionColor;

    private boolean isSelected = false;

    RootAdvancementEntry(AdvancementEntry advancementEntry, AdvancementsTrackerScreen parent) {
      this.advancementEntry = advancementEntry;
      this.background = advancementEntry.background;
      this.description = new TextComponent(stripControlCodes(advancementEntry.description));
      this.descriptionColor = advancementEntry.descriptionColor;
      this.font = parent.getFontRenderer();
      this.icon = advancementEntry.icon;
      this.parent = parent;
      this.title = new TextComponent(stripControlCodes(advancementEntry.title));
    }

    public AdvancementEntry getAdvancementEntry() {
      return advancementEntry;
    }

    private void renderBackground(PoseStack poseStack, int top, int entryWidth, int entryHeight) {
      if (this.background == null) {
        return;
      }
      if (isSelected) {
        RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1);
      } else {
        RenderSystem.setShaderColor(0.4f, 0.4f, 0.4f, 1);
      }
      RenderSystem.setShaderTexture(0, this.background);
      poseStack.pushPose();
      GuiComponent.blit(poseStack, getLeft() + 1, top - 1, 0, 0, entryWidth - 2, entryHeight + 2,
          16, 16);
      poseStack.popPose();
    }

    private void renderIcon(int top) {
      if (this.icon == null) {
        return;
      }
      minecraft.getItemRenderer().renderGuiItem(this.icon, getLeft() + 1, top + 6);
    }

    private void renderTrackedAdvancementsStatus(PoseStack poseStack, int top, int left,
        int entryWidth) {
      if (TrackedAdvancementsManager.hasTrackedAdvancement(advancementEntry)) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, miscTexture);
        poseStack.pushPose();
        poseStack.scale(0.5f, 0.5f, 0.5f);
        GuiComponent.blit(poseStack, (left + entryWidth - 12) * 2, (top + 1) * 2, 81, 6, 16, 16,
            256, 256);
        poseStack.popPose();
      }
    }

    private void renderDecoration(PoseStack poseStack, int top, int entryWidth, int entryHeight) {
      int topPosition = top - 2;
      int leftPosition = getLeft();
      int rightPosition = leftPosition + entryWidth - 2;
      int bottomPosition = top + entryHeight;

      RenderSystem.setShaderColor(1, 1, 1, 1);
      RenderSystem.setShaderTexture(0, miscTexture);
      poseStack.pushPose();
      GuiComponent.blit(poseStack, leftPosition, topPosition, 0, 0, entryWidth - 1, 1,
          entryWidth - 1, 256);
      GuiComponent.blit(poseStack, rightPosition, topPosition + 1, 255, 0, 1, entryHeight + 2, 256,
          entryHeight);
      GuiComponent.blit(poseStack, leftPosition, bottomPosition, 0, 255, entryWidth - 1, 1,
          entryWidth - 1, 256);
      GuiComponent.blit(poseStack, leftPosition, topPosition + 1, 0, 0, 1, entryHeight + 2, 256,
          entryHeight);
      poseStack.popPose();
    }

    @Override
    public Component getNarration() {
      return new TranslatableComponent("narrator.select", advancementEntry.title);
    }

    @Override
    public void render(PoseStack poseStack, int entryIdx, int top, int left, int entryWidth,
        int entryHeight, int mouseX, int mouseY, boolean isFocused, float partialTick) {

      // Selection state
      this.isSelected = isSelectedItem(entryIdx);

      // Positions
      int iconWidth = 14;
      int maxFontWidth = listWidth - iconWidth - 4;
      float textPositionLeft = (float) left + iconWidth;

      // Background
      this.renderBackground(poseStack, top, entryWidth, entryHeight);

      // Icon
      this.renderIcon(top);

      // Tracked Advancements
      this.renderTrackedAdvancementsStatus(poseStack, top, left, entryWidth);

      // Title (only one line)
      int titleColor = isSelected ? 0xFFFF00 : 0xFFFFFF;
      int titleWidth = font.width(title) > maxFontWidth ? maxFontWidth - 6 : maxFontWidth;
      font.drawShadow(poseStack,
          Language.getInstance()
              .getVisualOrder(FormattedText.composite(font.substrByWidth(title, titleWidth))),
          textPositionLeft + 3, top + (float) 1, titleColor);
      font.draw(poseStack,
          Language.getInstance()
              .getVisualOrder(FormattedText.composite(font.substrByWidth(title, titleWidth))),
          textPositionLeft + 3, top + (float) 1, titleColor);
      if (titleWidth != maxFontWidth) {
        font.draw(poseStack, Constants.ELLIPSIS, textPositionLeft + titleWidth, top + 1.0f,
            titleColor);
      }

      // Description (two lines)
      List<FormattedCharSequence> descriptionParts = font.split(description, maxFontWidth);
      int descriptionLines = 1;
      for (FormattedCharSequence descriptionPart : descriptionParts) {
        float descriptionTopPosition = top + (float) (2 + font.lineHeight) * descriptionLines;
        font.drawShadow(poseStack, descriptionPart, textPositionLeft + 3, descriptionTopPosition,
            this.descriptionColor);
        font.draw(poseStack, descriptionPart, textPositionLeft + 3, descriptionTopPosition,
            this.descriptionColor);
        if (descriptionParts.size() == 3 && descriptionLines == 2) {
          font.draw(poseStack, Constants.ELLIPSIS,
              textPositionLeft + (font.width(descriptionPart) < maxFontWidth - 6
                  ? font.width(descriptionPart) + 6
                  : maxFontWidth - 6),
              descriptionTopPosition, 0xFFFFFF);
          break;
        }
        descriptionLines++;
      }

      // Decoration
      this.renderDecoration(poseStack, top, entryWidth, entryHeight);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
        parent.setSelectedRootAdvancement(this);
        setSelected(this);
      }
      return false;
    }

  }

  @Override
  protected int getScrollbarPosition() {
    return this.listWidth + this.listLeft;
  }

  @Override
  public int getRowWidth() {
    return this.listWidth;
  }

  @Override
  protected void renderBackground(PoseStack poseStack) {
    // this.parent.renderBackground(poseStack);
  }

}