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

package de.markusbordihn.advancementstracker.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import de.markusbordihn.advancementstracker.client.advancements.AdvancementEntry;

public class AdvancementTooltip {

  private final AdvancementEntry advancementEntry;
  private final Font font;
  private final Minecraft minecraft;
  private static int tooltipWith = 200;
  private static int tooltipHeight = 200;

  public AdvancementTooltip(AdvancementEntry advancementEntry) {
    this.advancementEntry = advancementEntry;
    this.minecraft = Minecraft.getInstance();
    this.font = minecraft.font;
  }

  public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    guiGraphics.pose().pushPose();
    guiGraphics.drawString(this.font, advancementEntry.getTitle(), mouseX, mouseY, 0xFFFFFFFF);
    guiGraphics.fill(mouseX, mouseY, mouseX + tooltipWith, mouseY + tooltipHeight, 0x80000000);
    guiGraphics.pose().popPose();
  }

}
