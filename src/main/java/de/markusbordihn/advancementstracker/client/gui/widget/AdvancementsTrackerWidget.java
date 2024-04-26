/*
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

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.markusbordihn.advancementstracker.Constants;
import de.markusbordihn.advancementstracker.client.advancements.AdvancementEntry;
import de.markusbordihn.advancementstracker.client.advancements.AdvancementsManager;
import de.markusbordihn.advancementstracker.client.advancements.TrackedAdvancementsManager;
import de.markusbordihn.advancementstracker.client.keymapping.ModKeyMapping;
import de.markusbordihn.advancementstracker.config.ClientConfig;
import de.markusbordihn.advancementstracker.utils.gui.PositionManager;
import de.markusbordihn.advancementstracker.utils.gui.PositionManager.BasePosition;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(Dist.CLIENT)
public class AdvancementsTrackerWidget {

  protected static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

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
          .append(
              Component.translatable(
                      HOT_KEY_ADVANCEMENT_TRACKER,
                      ModKeyMapping.KEY_SHOW_WIDGET.getTranslatedKeyMessage())
                  .withStyle(ChatFormatting.YELLOW))
          .withStyle(ChatFormatting.WHITE);
  private static MutableComponent noTrackedAdvancementsText =
      Component.translatable(Constants.MOD_PREFIX + "advancementsWidget.noTrackedAdvancements")
          .append(
              Component.translatable(
                      HOT_KEY_ADVANCEMENT_OVERVIEW,
                      ModKeyMapping.KEY_SHOW_OVERVIEW.getTranslatedKeyMessage())
                  .withStyle(ChatFormatting.YELLOW))
          .append(
              Component.translatable(
                      HOT_KEY_ADVANCEMENT_TRACKER,
                      ModKeyMapping.KEY_SHOW_WIDGET.getTranslatedKeyMessage())
                  .withStyle(ChatFormatting.YELLOW))
          .withStyle(ChatFormatting.WHITE);

  private static PositionManager positionManager = new PositionManager();
  private static Set<AdvancementEntry> trackedAdvancements;
  private static boolean hudVisible = true;

  private final Font font;
  private final ItemRenderer itemRenderer;
  private final Minecraft minecraft;
  private final TextureManager textureManager;

  private int x;
  private int y;

  public AdvancementsTrackerWidget(Minecraft minecraft) {
    this.font = minecraft.font;
    this.itemRenderer = minecraft.getItemRenderer();
    this.minecraft = minecraft;
    this.textureManager = minecraft.getTextureManager();
    positionManager.setInstance(minecraft);
    positionManager.setWidth(120);
    positionManager.setHeight(0);
    positionManager.setBasePosition(BasePosition.MIDDLE_RIGHT);
  }

  @SubscribeEvent
  public static void handleLevelEventLoad(LevelEvent.Load event) {
    // Ignore server side worlds.
    if (!event.getLevel().isClientSide()) {
      return;
    }

    updatePredefinedText();
    hudVisible = CLIENT.widgetEnabled.get() && CLIENT.widgetVisible.get();
    if (hudVisible) {
      log.info("Widget will be automatically visible on the start.");
    } else if (Boolean.TRUE.equals(CLIENT.widgetEnabled.get())) {
      log.info(
          "Widget will not be automatically visible on the start and you need to use the hot-keys to make it visible!");
    } else {
      log.info("Widget is disabled!");
      return;
    }

    log.info("Set widget size to {}x{}", CLIENT.widgetWidth.get(), CLIENT.widgetHeight.get());
    positionManager.setHeight(CLIENT.widgetHeight.get());
    positionManager.setWidth(CLIENT.widgetWidth.get());

    log.info("Set widget base position to: {}", CLIENT.widgetPosition.get());
    positionManager.setBasePosition(CLIENT.widgetPosition.get());

    log.info(
        "Set widget position top offset {} and left offset {}",
        CLIENT.widgetTop.get(),
        CLIENT.widgetLeft.get());
    positionManager.setPositionX(CLIENT.widgetLeft.get());
    positionManager.setPositionY(CLIENT.widgetTop.get());
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

  private static void updatePredefinedText() {
    // Update text for custom key-mapping.
    noAdvancementsText =
        Component.translatable(Constants.MOD_PREFIX + "advancementsWidget.noAdvancements")
            .append(
                Component.translatable(
                        HOT_KEY_ADVANCEMENT_TRACKER,
                        ModKeyMapping.KEY_SHOW_WIDGET.getTranslatedKeyMessage())
                    .withStyle(ChatFormatting.YELLOW))
            .withStyle(ChatFormatting.WHITE);
    noTrackedAdvancementsText =
        Component.translatable(Constants.MOD_PREFIX + "advancementsWidget.noTrackedAdvancements")
            .append(
                Component.translatable(
                        HOT_KEY_ADVANCEMENT_OVERVIEW,
                        ModKeyMapping.KEY_SHOW_OVERVIEW.getTranslatedKeyMessage())
                    .withStyle(ChatFormatting.YELLOW))
            .append(
                Component.translatable(
                        HOT_KEY_ADVANCEMENT_TRACKER,
                        ModKeyMapping.KEY_SHOW_WIDGET.getTranslatedKeyMessage())
                    .withStyle(ChatFormatting.YELLOW))
            .withStyle(ChatFormatting.WHITE);
  }

  @SubscribeEvent()
  public void renderOverlay(RenderGuiOverlayEvent.Pre event) {

    // Check if widget is enabled or debug overlay is open and ignore event.
    if (!hudVisible || this.minecraft.options.renderDebug) {
      return;
    }

    // Disable overlay if visibility is disabled or if there is another screen besides chat.
    if (this.minecraft.screen != null && !(this.minecraft.screen instanceof ChatScreen)) {
      return;
    }

    // Makes sure only to render widget together with scoreboard gui overlay.
    if (event.getOverlay() != VanillaGuiOverlay.SCOREBOARD.type()) {
      return;
    }

    // Use Position Manager for Updates and update x and y reference.
    positionManager.updateWindow();
    x = positionManager.getPositionX();
    y = positionManager.getPositionY();

    // Get pose stack and render buffer for additional effects.
    PoseStack poseStack = event.getPoseStack();

    // Render background and title
    renderTitle(poseStack);

    // Render tracked advancement or additional hints, if needed.
    if (TrackedAdvancementsManager.hasTrackedAdvancements()) {
      MultiBufferSource.BufferSource multiBufferSource =
          Minecraft.getInstance().renderBuffers().bufferSource();
      renderAdvancements(poseStack, multiBufferSource, x, y + this.font.lineHeight + 4);
    } else if (AdvancementsManager.hasAdvancements()) {
      renderNoTrackedAdvancements(poseStack, x, y + this.font.lineHeight + 4);
    } else {
      renderNoAdvancements(poseStack, x, y + this.font.lineHeight + 4);
    }
  }

  private void renderTitle(PoseStack poseStack) {
    poseStack.pushPose();
    GuiComponent.fill(
        poseStack,
        x,
        y,
        positionManager.getPositionXWidth(),
        y + this.font.lineHeight + 2,
        BACKGROUND_COLOR);
    font.draw(poseStack, ADVANCEMENT_TITLE_TEXT, x + 2.0f, y + 2.0f, Constants.FONT_COLOR_GRAY);
    poseStack.popPose();
  }

  private void renderNoTrackedAdvancements(PoseStack poseStack, int x, int y) {
    int textContentHeight = (this.font.lineHeight + 2) * 11;
    int textContentWidth = positionManager.getWidth();
    poseStack.pushPose();
    GuiComponent.fill(
        poseStack, x, y, x + textContentWidth, y + textContentHeight, BACKGROUND_COLOR);
    font.drawWordWrap(
        noTrackedAdvancementsText, x + 5, y + 5, textContentWidth - 10, textContentHeight - 5);
    poseStack.popPose();
  }

  private void renderNoAdvancements(PoseStack poseStack, int x, int y) {
    int textContentHeight = (this.font.lineHeight + 2) * 9;
    int textContentWidth = positionManager.getWidth();
    poseStack.pushPose();
    GuiComponent.fill(
        poseStack, x, y, x + textContentWidth, y + textContentHeight, BACKGROUND_COLOR);
    font.drawWordWrap(
        noAdvancementsText, x + 5, y + 5, textContentWidth - 10, textContentHeight - 5);
    poseStack.popPose();
  }

  private void renderAdvancements(
      PoseStack poseStack, MultiBufferSource.BufferSource multiBufferSource, int x, int y) {
    poseStack.pushPose();
    int topPos = y;
    int numberOfAdvancementsRendered = 0;
    try {
      for (AdvancementEntry advancementEntry : trackedAdvancements) {
        // Check if the screen space is big enough to render all advancements.
        if (topPos + (font.lineHeight * 4) < positionManager.getWindowHeightScaled()) {
          topPos +=
              renderAdvancement(poseStack, multiBufferSource, x, topPos, advancementEntry) + 2;
          numberOfAdvancementsRendered++;
        } else {
          renderAdvancementEllipsis(
              poseStack, x, topPos, trackedAdvancements.size(), numberOfAdvancementsRendered);
          break;
        }
      }
    } catch (ConcurrentModificationException exception) {
      log.debug("Advancement list was modified during rendering. This is expected in some cases.");
    }
    poseStack.popPose();
  }

  private void renderAdvancementEllipsis(
      PoseStack poseStack,
      int x,
      int y,
      int numberOfAdvancements,
      int numberOfAdvancementsRendered) {

    // Background
    poseStack.pushPose();
    GuiComponent.fill(
        poseStack,
        x,
        y,
        positionManager.getPositionXWidth(),
        y + font.lineHeight,
        BACKGROUND_COLOR);
    poseStack.popPose();

    // Note that not all tracked advancements are visible.
    float textScale = 0.75f;
    Component text =
        Component.translatable(
            Constants.ADVANCEMENTS_WIDGET_PREFIX + "notAllVisible",
            numberOfAdvancementsRendered,
            numberOfAdvancements);
    poseStack.pushPose();
    poseStack.scale(textScale, textScale, textScale);
    font.drawShadow(
        poseStack, text, (x + 16) / textScale, (y + 2) / textScale, Constants.FONT_COLOR_GRAY);
    font.draw(
        poseStack, text, (x + 16) / textScale, (y + 2) / textScale, Constants.FONT_COLOR_GRAY);
    poseStack.popPose();
  }

  private int renderAdvancement(
      PoseStack poseStack,
      MultiBufferSource.BufferSource multiBufferSource,
      int x,
      int y,
      AdvancementEntry advancementEntry) {

    // Positions
    int maxFontWidth = positionManager.getWidth() - 2;
    int referenceTopPosition = y + 3;
    int referenceLeftPosition = x + 2;

    // Pre-calculations
    float titleScale = 0.75f;
    int titlePaddingLeft = 10;
    int titlePaddingRight = advancementEntry.getProgress().getProgressTotal() > 1 ? 20 : 0;
    int titleWidth =
        advancementEntry.getTitleWidth() * titleScale
                > maxFontWidth - titlePaddingLeft - titlePaddingRight
            ? maxFontWidth - titlePaddingLeft - titlePaddingRight - Math.round(7 * titleScale)
            : maxFontWidth - titlePaddingLeft - titlePaddingRight;
    int titleWidthScaled = Math.round(titleWidth / titleScale);
    FormattedCharSequence titleText =
        Language.getInstance()
            .getVisualOrder(
                FormattedText.composite(
                    font.substrByWidth(advancementEntry.getTitle(), titleWidthScaled)));

    float descriptionScale = 0.75f;
    List<FormattedCharSequence> descriptionParts =
        font.split(
            advancementEntry.getDescription(), Math.round(maxFontWidth / descriptionScale) - 3);
    int descriptionLines = 1;

    // Calculate expected content size
    int expectedContentSize =
        Math.round(
            (font.lineHeight * titleScale + 3)
                + ((font.lineHeight * descriptionScale + 3)
                    * (Math.min(descriptionParts.size(), 3))));

    // Background
    poseStack.pushPose();
    GuiComponent.fill(
        poseStack,
        x,
        y,
        positionManager.getPositionXWidth(),
        y + expectedContentSize,
        BACKGROUND_COLOR);
    poseStack.popPose();

    // Title (only one line)
    poseStack.pushPose();
    poseStack.scale(titleScale, titleScale, titleScale);
    font.drawShadow(
        poseStack,
        titleText,
        (referenceLeftPosition + titlePaddingLeft) / titleScale,
        referenceTopPosition / titleScale,
        Constants.FONT_COLOR_YELLOW);
    font.draw(
        poseStack,
        titleText,
        (referenceLeftPosition + titlePaddingLeft) / titleScale,
        referenceTopPosition / titleScale,
        Constants.FONT_COLOR_YELLOW);

    // Show ellipsis if title is to long.
    if (titleWidth != maxFontWidth - titlePaddingLeft - titlePaddingRight) {
      font.draw(
          poseStack,
          Constants.ELLIPSIS,
          ((referenceLeftPosition + titlePaddingLeft) / titleScale) + titleWidthScaled,
          referenceTopPosition / titleScale,
          Constants.FONT_COLOR_YELLOW);
    }
    poseStack.popPose();

    // Show Progress, if we have more than one requirements.
    if (advancementEntry.getProgress().getProgressTotal() > 1) {
      float progressScale = 0.6f;
      int progressPositionLeft =
          referenceLeftPosition
              + maxFontWidth
              - Math.round(advancementEntry.getProgress().getProgressStringWidth() * progressScale)
              - 2;
      poseStack.pushPose();
      poseStack.scale(progressScale, progressScale, progressScale);
      font.drawShadow(
          poseStack,
          advancementEntry.getProgress().getProgressString(),
          progressPositionLeft / progressScale,
          (referenceTopPosition - 1) / progressScale,
          Constants.FONT_COLOR_YELLOW);
      font.draw(
          poseStack,
          advancementEntry.getProgress().getProgressString(),
          progressPositionLeft / progressScale,
          (referenceTopPosition - 1) / progressScale,
          Constants.FONT_COLOR_YELLOW);
      poseStack.popPose();
    }
    referenceTopPosition += (int) (font.lineHeight * titleScale + 3);

    // Icon
    if (advancementEntry.getIcon() != null) {
      poseStack.pushPose();
      renderGuiItem(
          advancementEntry.getIcon(),
          multiBufferSource,
          referenceLeftPosition - 4,
          referenceTopPosition - 14,
          0.65f);
      poseStack.popPose();
    }

    // Description (max three lines)
    poseStack.pushPose();
    poseStack.scale(descriptionScale, descriptionScale, descriptionScale);
    for (FormattedCharSequence descriptionPart : descriptionParts) {
      boolean shouldEnd = false;
      font.drawShadow(
          poseStack,
          descriptionPart,
          referenceLeftPosition / descriptionScale,
          referenceTopPosition / descriptionScale,
          advancementEntry.getDescriptionColor());
      font.draw(
          poseStack,
          descriptionPart,
          referenceLeftPosition / descriptionScale,
          referenceTopPosition / descriptionScale,
          advancementEntry.getDescriptionColor());
      if ((descriptionParts.size() >= 3 && descriptionLines == 3)) {
        font.draw(
            poseStack,
            Constants.ELLIPSIS,
            (referenceLeftPosition / descriptionScale)
                + ((font.width(descriptionPart) / descriptionScale)
                        < maxFontWidth / descriptionScale - 3
                    ? (font.width(descriptionPart) / descriptionScale) - 7
                    : (maxFontWidth / descriptionScale) - 7),
            referenceTopPosition / descriptionScale,
            0xFFFFFF);
        shouldEnd = true;
      } else if (descriptionParts.size() == 2 && descriptionLines == 2) {
        shouldEnd = true;
      }
      referenceTopPosition += (int) (font.lineHeight * descriptionScale + 3);
      if (shouldEnd) {
        break;
      }
      descriptionLines++;
    }
    poseStack.popPose();

    // Return actual content position
    return referenceTopPosition - y;
  }

  /**
   * @deprecated
   */
  @Deprecated
  private void renderGuiItem(
      ItemStack itemStack,
      MultiBufferSource.BufferSource multiBufferSource,
      int x,
      int y,
      float scale) {
    this.renderGuiItem(
        itemStack, multiBufferSource, x, y, scale, itemRenderer.getModel(itemStack, null, null, 0));
  }

  /**
   * @deprecated
   */
  @Deprecated
  private void renderGuiItem(
      ItemStack itemStack,
      MultiBufferSource.BufferSource multiBufferSource,
      int x,
      int y,
      float scale,
      BakedModel model) {
    this.textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, false);
    RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
    RenderSystem.enableBlend();
    RenderSystem.blendFunc(
        GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    PoseStack modelPoseStack = RenderSystem.getModelViewStack();
    modelPoseStack.pushPose();
    modelPoseStack.translate(x, y, 100.0F + itemRenderer.blitOffset);
    modelPoseStack.translate(8.0D, 8.0D, 0.0D);
    modelPoseStack.scale(scale, -scale, scale);
    modelPoseStack.scale(16.0F, 16.0F, 16.0F);
    RenderSystem.applyModelViewMatrix();
    PoseStack modelPoseStack1 = new PoseStack();
    boolean flag = !model.usesBlockLight();
    if (flag) {
      Lighting.setupForFlatItems();
    }
    itemRenderer.render(
        itemStack,
        ItemTransforms.TransformType.GUI,
        false,
        modelPoseStack1,
        multiBufferSource,
        15728880,
        OverlayTexture.NO_OVERLAY,
        model);
    multiBufferSource.endBatch();
    RenderSystem.enableDepthTest();
    if (flag) {
      Lighting.setupFor3DItems();
    }
    modelPoseStack.popPose();
    RenderSystem.applyModelViewMatrix();
  }
}
