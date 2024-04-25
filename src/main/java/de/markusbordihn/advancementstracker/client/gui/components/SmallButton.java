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

package de.markusbordihn.advancementstracker.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class SmallButton extends Button {

  private static final float SCALING = 0.55f;
  private final Minecraft minecraft;
  private final Font font;
  private final int scaledX;
  private final int scaledY;
  private final int scaledWidth;
  private final int scaledHeight;

  public SmallButton(int x, int y, int width, int height, Component component, OnPress onPress) {
    super(x, y, width, height, component, onPress, Button.DEFAULT_NARRATION);
    this.minecraft = Minecraft.getInstance();
    this.font = this.minecraft.font;
    this.scaledX = Math.round(this.getX() / SCALING);
    this.scaledY = Math.round(this.getY() / SCALING);
    this.scaledWidth = Math.round(this.width / SCALING);
    this.scaledHeight = Math.round(this.height / SCALING);
  }

  @Override
  public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.enableDepthTest();

    // Scaling down the button images
    ResourceLocation sprite = SPRITES.get(this.active, this.isHoveredOrFocused());
    guiGraphics.blitSprite(sprite, this.getX(), this.getY(), this.width / 2, this.height);
    guiGraphics.blitSprite(
        sprite, this.getX() + this.width / 2, this.getY(), this.width / 2, this.height);

    // Scaling down button text
    guiGraphics.pose().pushPose();
    guiGraphics.pose().scale(SCALING, SCALING, SCALING);
    guiGraphics.drawCenteredString(
        this.font,
        this.getMessage(),
        this.scaledX + this.scaledWidth / 2,
        this.scaledY + (this.scaledHeight - 8) / 2,
        getFGColor() | Mth.ceil(this.alpha * 255.0F) << 24);
    guiGraphics.pose().popPose();
  }
}
