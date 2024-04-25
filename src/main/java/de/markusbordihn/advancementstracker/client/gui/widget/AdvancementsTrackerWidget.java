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

package de.markusbordihn.advancementstracker.client.gui.widget;

import de.markusbordihn.advancementstracker.AdvancementsTracker;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Set;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.neoforge.client.gui.overlay.ExtendedGui;
import net.neoforged.neoforge.client.gui.overlay.IGuiOverlay;
import net.neoforged.neoforge.event.level.LevelEvent;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import de.markusbordihn.advancementstracker.Constants;
import de.markusbordihn.advancementstracker.client.advancements.AdvancementEntry;
import de.markusbordihn.advancementstracker.client.advancements.AdvancementsManager;
import de.markusbordihn.advancementstracker.client.advancements.TrackedAdvancementsManager;
import de.markusbordihn.advancementstracker.client.keymapping.ModKeyMapping;
import de.markusbordihn.advancementstracker.config.ClientConfig;
import de.markusbordihn.advancementstracker.utils.gui.PositionManager;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class AdvancementsTrackerWidget implements IGuiOverlay {

  public static final AdvancementsTrackerWidget INSTANCE = new AdvancementsTrackerWidget();

  private static final ClientConfig.Config CLIENT = ClientConfig.CLIENT;

  // Pre-defined colors and placeholders
  private static final int BACKGROUND_COLOR = 0x70000000;

  // Pre-defined texts
  private static final String HOT_KEY_ADVANCEMENT_TRACKER =
      Constants.MOD_PREFIX + "advancementsWidget.hotkeyAdvancementTracker";
  private static final String HOT_KEY_ADVANCEMENT_OVERVIEW =
      Constants.MOD_PREFIX + "advancementsWidget.hotkeyAdvancementOverview";
  private static final MutableComponent ADVANCEMENT_TITLE_TEXT =
      Component.literal("☑ Advancements Tracker");
  private static MutableComponent noAdvancementsText =
      Component.translatable(Constants.MOD_PREFIX + "advancementsWidget.noAdvancements")
          .append(ModKeyMapping.KEY_SHOW_WIDGET.getTranslatedKeyMessage())
          .append(Component
              .translatable(HOT_KEY_ADVANCEMENT_TRACKER,
                  ModKeyMapping.KEY_SHOW_WIDGET.getTranslatedKeyMessage())
              .withStyle(ChatFormatting.YELLOW))
          .withStyle(ChatFormatting.WHITE);
  private static MutableComponent noTrackedAdvancementsText =
      Component.translatable(Constants.MOD_PREFIX + "advancementsWidget.noTrackedAdvancements")
          .append(Component
              .translatable(HOT_KEY_ADVANCEMENT_OVERVIEW,
                  ModKeyMapping.KEY_SHOW_OVERVIEW.getTranslatedKeyMessage())
              .withStyle(ChatFormatting.YELLOW))
          .append(Component
              .translatable(HOT_KEY_ADVANCEMENT_TRACKER,
                  ModKeyMapping.KEY_SHOW_WIDGET.getTranslatedKeyMessage())
              .withStyle(ChatFormatting.YELLOW))
          .withStyle(ChatFormatting.WHITE);

  private static final PositionManager positionManager = new PositionManager();
  private static Set<AdvancementEntry> trackedAdvancements;
  private static boolean hudVisible = true;

  @SubscribeEvent
  public static void handleLevelEventLoad(LevelEvent.Load event) {
    // Ignore server side worlds.
    if (!event.getLevel().isClientSide()) {
      return;
    }

    updatePredefinedText();
    hudVisible = CLIENT.widgetEnabled.get() && CLIENT.widgetVisible.get();
    if (hudVisible) {
      AdvancementsTracker.log.info("Widget will be automatically visible on the start.");
    } else if (Boolean.TRUE.equals(CLIENT.widgetEnabled.get())) {
      AdvancementsTracker.log.info(
          "Widget will not be automatically visible on the start and you need to use the hot-keys to make it visible!");
    } else {
      AdvancementsTracker.log.info("Widget is disabled!");
      return;
    }

    AdvancementsTracker.log.info("Set widget size to {}x{}", CLIENT.widgetWidth.get(), CLIENT.widgetHeight.get());
    positionManager.setHeight(CLIENT.widgetHeight.get());
    positionManager.setWidth(CLIENT.widgetWidth.get());

    AdvancementsTracker.log.info("Set widget base position to: {}", CLIENT.widgetPosition.get());
    positionManager.setBasePosition(CLIENT.widgetPosition.get());

    AdvancementsTracker.log.info("Set widget position top offset {} and left offset {}", CLIENT.widgetTop.get(),
        CLIENT.widgetLeft.get());
    positionManager.setPositionX(CLIENT.widgetLeft.get());
    positionManager.setPositionY(CLIENT.widgetTop.get());
  }

  @Override
  public void render(ExtendedGui gui, @NotNull GuiGraphics guiGraphics, float partialTicks, int screenWidth, int screenHeight) {
    Minecraft minecraft = gui.getMinecraft();

    // Check if widget is enabled or debug overlay is open and ignore event.
    if (!hudVisible || minecraft.getDebugOverlay().showDebugScreen()) {
      return;
    }

    // Disable overlay if visibility is disabled or if there is another screen besides chat.
    if (minecraft.screen != null && !(minecraft.screen instanceof ChatScreen)) {
      return;
    }


    // Use Position Manager for Updates and update x and y reference.
    positionManager.updateWindow(screenWidth, screenHeight);

    // Get gui graphics and render buffer for additional effects.
    Font font = gui.getFont();
    PoseStack pose = guiGraphics.pose();
    pose.pushPose();
    pose.translate(positionManager.getPositionX(), positionManager.getPositionY(), 0);

    // Render background and title
    renderTitle(guiGraphics, font);

    // Render tracked advancement or additional hints, if needed.
    if (TrackedAdvancementsManager.hasTrackedAdvancements()) {
      renderAdvancements(guiGraphics, font);
    } else if (AdvancementsManager.hasAdvancements()) {
      renderNoTrackedAdvancements(guiGraphics, font);
    } else {
      renderNoAdvancements(guiGraphics, font);
    }
    pose.popPose();
  }

  public static void reloadConfig() {
    positionManager.setHeight(CLIENT.widgetHeight.get());
    positionManager.setWidth(CLIENT.widgetWidth.get());
    positionManager.setBasePosition(CLIENT.widgetPosition.get());
    positionManager.setPositionX(CLIENT.widgetLeft.get());
    positionManager.setPositionY(CLIENT.widgetTop.get());
  }

  public static void updateTrackedAdvancements() {
    trackedAdvancements = TrackedAdvancementsManager.getTrackedAdvancements();
  }

  public static void toggleVisibility() {
    hudVisible = !hudVisible;
  }

  private void renderTitle(GuiGraphics guiGraphics, Font font) {
    guiGraphics.pose().pushPose();
    guiGraphics.fill(0, 0, positionManager.getPositionXWidth(), font.lineHeight + 2,
        BACKGROUND_COLOR);
    guiGraphics.drawString(font, ADVANCEMENT_TITLE_TEXT, 2, 2, Constants.FONT_COLOR_GRAY, false);
    guiGraphics.pose().popPose();
  }

  private void renderNoTrackedAdvancements(GuiGraphics guiGraphics, Font font) {
    int textContentHeight = (font.lineHeight + 2) * 11;
    int textContentWidth = positionManager.getWidth();
    guiGraphics.pose().pushPose();
    int y = font.lineHeight + 4;
    guiGraphics.fill(0, y, textContentWidth, y + textContentHeight, BACKGROUND_COLOR);
    guiGraphics.drawWordWrap(font, noTrackedAdvancementsText, 5, y + 5,
        textContentWidth - 10, textContentHeight - 5);
    guiGraphics.pose().popPose();
  }

  private void renderNoAdvancements(GuiGraphics guiGraphics, Font font) {
    int textContentHeight = (font.lineHeight + 2) * 9;
    int textContentWidth = positionManager.getWidth();
    guiGraphics.pose().pushPose();
    int y = font.lineHeight + 4;
    guiGraphics.fill(0, y, textContentWidth, y + textContentHeight, BACKGROUND_COLOR);
    guiGraphics.drawWordWrap(font, noAdvancementsText, 5, y + 5, textContentWidth - 10,
        textContentHeight - 5);
    guiGraphics.pose().popPose();
  }

  private void renderAdvancements(GuiGraphics guiGraphics, Font font) {
    guiGraphics.pose().pushPose();
    int topPos = font.lineHeight + 4;
    int numberOfAdvancementsRendered = 0;
    try {
      for (AdvancementEntry advancementEntry : trackedAdvancements) {
        // Check if the screen space is big enough to render all advancements.
        if (topPos + (font.lineHeight * 4) < positionManager.getWindowHeightScaled()) {
          topPos += renderAdvancement(guiGraphics, font, 0, topPos, advancementEntry) + 2;
          numberOfAdvancementsRendered++;
        } else {
          renderAdvancementEllipsis(guiGraphics, font, 0, topPos, trackedAdvancements.size(),
              numberOfAdvancementsRendered);
          break;
        }
      }
    } catch (ConcurrentModificationException exception) {
      AdvancementsTracker.log.debug("Advancement list was modified during rendering. This is expected in some cases.");
    }
    guiGraphics.pose().popPose();
  }

  private void renderAdvancementEllipsis(GuiGraphics guiGraphics, Font font, int x, int y,
      int numberOfAdvancements, int numberOfAdvancementsRendered) {

    // Background
    guiGraphics.pose().pushPose();
    guiGraphics.fill(x, y, positionManager.getPositionXWidth(), y + font.lineHeight,
        BACKGROUND_COLOR);
    guiGraphics.pose().popPose();

    // Note that not all tracked advancements are visible.
    float textScale = 0.75f;
    Component text = Component.translatable(Constants.ADVANCEMENTS_WIDGET_PREFIX + "notAllVisible",
        numberOfAdvancementsRendered, numberOfAdvancements);
    guiGraphics.pose().pushPose();
    guiGraphics.pose().scale(textScale, textScale, textScale);
    guiGraphics.drawString(font, text, Math.round((x + 16) / textScale),
        Math.round((y + 2) / textScale), Constants.FONT_COLOR_GRAY);
    guiGraphics.pose().popPose();
  }

  private int renderAdvancement(GuiGraphics guiGraphics, Font font, int x, int y, AdvancementEntry advancementEntry) {

    // Positions
    int maxFontWidth = positionManager.getWidth() - 2;
    int referenceTopPosition = y + 3;
    int referenceLeftPosition = x + 2;

    // Pre-calculations
    float titleScale = 0.75f;
    int titlePaddingLeft = 10;
    int titlePaddingRight = advancementEntry.getProgress().getProgressTotal() > 1 ? 20 : 0;
    int titleWidth = font.width(advancementEntry.getTitle()) * titleScale > maxFontWidth - titlePaddingLeft
        - titlePaddingRight
            ? maxFontWidth - titlePaddingLeft - titlePaddingRight - Math.round(7 * titleScale)
            : maxFontWidth - titlePaddingLeft - titlePaddingRight;
    int titleWidthScaled = Math.round(titleWidth / titleScale);
    FormattedCharSequence titleText = Language.getInstance().getVisualOrder(
        FormattedText.composite(font.substrByWidth(advancementEntry.getTitle(), titleWidthScaled)));

    float descriptionScale = 0.75f;
    List<FormattedCharSequence> descriptionParts = font.split(advancementEntry.getDescription(),
        Math.round(maxFontWidth / descriptionScale) - 3);
    int descriptionLines = 1;

    // Calculate expected content size
    int expectedContentSize =
        Math.round((font.lineHeight * titleScale + 3) + ((font.lineHeight * descriptionScale + 3)
            * Math.min(descriptionParts.size(), 3)));

    // Background
    guiGraphics.pose().pushPose();
    guiGraphics.fill(x, y, positionManager.getPositionXWidth(), y + expectedContentSize,
        BACKGROUND_COLOR);
    guiGraphics.pose().popPose();

    // Title (only one line)
    guiGraphics.pose().pushPose();
    guiGraphics.pose().scale(titleScale, titleScale, titleScale);
    guiGraphics.drawString(font, titleText,
        Math.round((referenceLeftPosition + titlePaddingLeft) / titleScale),
        Math.round(referenceTopPosition / titleScale), Constants.FONT_COLOR_YELLOW);

    // Show ellipsis if title is to long.
    if (titleWidth != maxFontWidth - titlePaddingLeft - titlePaddingRight) {
      guiGraphics.drawString(font, Constants.ELLIPSIS,
          Math.round(((referenceLeftPosition + titlePaddingLeft) / titleScale) + titleWidthScaled),
          Math.round(referenceTopPosition / titleScale), Constants.FONT_COLOR_YELLOW, false);
    }
    guiGraphics.pose().popPose();

    // Show Progress, if we have more than one requirements.
    if (advancementEntry.getProgress().getProgressTotal() > 1) {
      float progressScale = 0.6f;
      int progressPositionLeft = referenceLeftPosition + maxFontWidth
          - Math.round(font.width(advancementEntry.getProgress().getProgressString()) * progressScale) - 2;
      guiGraphics.pose().pushPose();
      guiGraphics.pose().scale(progressScale, progressScale, progressScale);
      guiGraphics.drawString(font, advancementEntry.getProgress().getProgressString(),
          Math.round(progressPositionLeft / progressScale),
          Math.round((referenceTopPosition - 1) / progressScale), Constants.FONT_COLOR_YELLOW);
      guiGraphics.pose().popPose();
    }
    referenceTopPosition += font.lineHeight * titleScale + 3;

    // Icon
    if (advancementEntry.getIcon() != null) {
      renderGuiItem(guiGraphics, advancementEntry.getIcon(), referenceLeftPosition - 4, referenceTopPosition - 14, 0.65f);
    }

    // Description (max three lines)
    guiGraphics.pose().pushPose();
    guiGraphics.pose().scale(descriptionScale, descriptionScale, descriptionScale);
    for (FormattedCharSequence descriptionPart : descriptionParts) {
      boolean shouldEnd = false;
      guiGraphics.drawString(font, descriptionPart,
          Math.round(referenceLeftPosition / descriptionScale),
          Math.round(referenceTopPosition / descriptionScale),
          advancementEntry.getDescriptionColor());
      if ((descriptionParts.size() >= 3 && descriptionLines == 3)) {
        guiGraphics.drawString(font, Constants.ELLIPSIS, Math.round((referenceLeftPosition
            / descriptionScale)
            + ((font.width(descriptionPart) / descriptionScale) < maxFontWidth / descriptionScale
                - 3 ? (font.width(descriptionPart) / descriptionScale) - 7
                    : (maxFontWidth / descriptionScale) - 7)),
            Math.round(referenceTopPosition / descriptionScale), 0xFFFFFF, false);
        shouldEnd = true;
      } else if (descriptionParts.size() == 2 && descriptionLines == 2) {
        shouldEnd = true;
      }
      referenceTopPosition += font.lineHeight * descriptionScale + 3;
      if (shouldEnd) {
        break;
      }
      descriptionLines++;
    }
    guiGraphics.pose().popPose();

    // Return actual content position
    return referenceTopPosition - y;
  }

  private static void updatePredefinedText() {
    // Update text for custom key-mapping.
    noAdvancementsText =
        Component.translatable(Constants.MOD_PREFIX + "advancementsWidget.noAdvancements")
            .append(Component
                .translatable(HOT_KEY_ADVANCEMENT_TRACKER,
                    ModKeyMapping.KEY_SHOW_WIDGET.getTranslatedKeyMessage())
                .withStyle(ChatFormatting.YELLOW))
            .withStyle(ChatFormatting.WHITE);
    noTrackedAdvancementsText =
        Component.translatable(Constants.MOD_PREFIX + "advancementsWidget.noTrackedAdvancements")
            .append(Component
                .translatable(HOT_KEY_ADVANCEMENT_OVERVIEW,
                    ModKeyMapping.KEY_SHOW_OVERVIEW.getTranslatedKeyMessage())
                .withStyle(ChatFormatting.YELLOW))
            .append(Component
                .translatable(HOT_KEY_ADVANCEMENT_TRACKER,
                    ModKeyMapping.KEY_SHOW_WIDGET.getTranslatedKeyMessage())
                .withStyle(ChatFormatting.YELLOW))
            .withStyle(ChatFormatting.WHITE);
  }

  private void renderGuiItem(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y, float scale) {
    PoseStack pose = guiGraphics.pose();
    pose.pushPose();
    if (scale != 1) {
      //Translate before scaling, and then set xAxis and yAxis to zero so that we don't translate a second time
      pose.translate(x, y, 0);
      pose.scale(scale, scale, scale);
      x = 0;
      y = 0;
    }
    guiGraphics.renderItem(itemStack, x, y);
    pose.popPose();
  }

}
