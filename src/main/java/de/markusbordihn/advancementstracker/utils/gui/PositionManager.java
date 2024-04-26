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

package de.markusbordihn.advancementstracker.utils.gui;

import com.mojang.blaze3d.platform.Window;
import de.markusbordihn.advancementstracker.Constants;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PositionManager {

  protected static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private static final int HOTBAR_RIGHT = 90;
  private static final int HOTBAR_LEFT = -90;
  private static final int SAFE_AREA = 25;

  private PositionPoint position = new PositionPoint();
  private BasePosition basePosition = BasePosition.MIDDLE_RIGHT;
  private Window window;
  private int guiScaledHeight;
  private int guiScaledWidth;
  private int height = 100;
  private int width = 100;
  private int defaultHeight = 100;
  private int defaultWidth = 100;
  private int windowHeight = 400;
  private int windowWidth = 640;

  public PositionManager() {}

  public PositionManager(Minecraft minecraft) {
    setInstance(minecraft);
  }

  public void setInstance(Minecraft minecraft) {
    this.window = minecraft.getWindow();
    updateWindow();
  }

  public PositionPoint getTopLeft() {
    return new PositionPoint(0, 0);
  }

  public PositionPoint getTopRight() {
    return new PositionPoint(this.guiScaledWidth - getWidthOrDefault(), 0);
  }

  public PositionPoint getBottomLeft() {
    return new PositionPoint(0, this.guiScaledHeight - getHeightOrDefault());
  }

  public PositionPoint getBottomRight() {
    return new PositionPoint(
        this.guiScaledWidth - getWidthOrDefault(), this.guiScaledHeight - getHeightOrDefault());
  }

  public PositionPoint getMiddleLeft() {
    return new PositionPoint(0, this.guiScaledHeight / 2 - getHeightOrDefault() / 2);
  }

  public PositionPoint getMiddleRight() {
    return new PositionPoint(
        this.guiScaledWidth - this.width, this.guiScaledHeight / 2 - getHeightOrDefault() / 2);
  }

  public PositionPoint getHotbarLeft() {
    return new PositionPoint(
        this.guiScaledWidth / 2 + HOTBAR_LEFT - getWidthOrDefault(),
        this.guiScaledHeight - getHeightOrDefault());
  }

  public PositionPoint getHotbarRight() {
    return new PositionPoint(
        this.guiScaledWidth / 2 + HOTBAR_RIGHT, this.guiScaledHeight - getHeightOrDefault());
  }

  public void setBasePosition(BasePosition basePosition) {
    this.basePosition = basePosition;
    updateBasePosition();
  }

  public void updateBasePosition() {
    switch (this.basePosition) {
      case TOP_LEFT:
        this.position.setOffset(getTopLeft());
        break;
      case TOP_RIGHT:
        this.position.setOffset(getTopRight());
        break;
      case BOTTOM_LEFT:
        this.position.setOffset(getBottomLeft());
        break;
      case BOTTOM_RIGHT:
        this.position.setOffset(getBottomRight());
        break;
      case MIDDLE_LEFT:
        this.position.setOffset(getMiddleLeft());
        break;
      default:
        this.position.setOffset(getMiddleRight());
    }
  }

  public int getWidth() {
    return width;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  public int getHeight() {
    return height;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public PositionPoint getPosition() {
    return position;
  }

  public int getPositionX() {
    return position.getAbsoluteX();
  }

  public void setPositionX(int x) {
    position.setX(Math.min(x, this.guiScaledWidth - getWidthOrDefault() - SAFE_AREA));
  }

  public int getPositionY() {
    return position.getAbsoluteY();
  }

  public void setPositionY(int y) {
    position.setY(Math.min(y, this.guiScaledHeight - getHeightOrDefault() - SAFE_AREA));
  }

  public int getPositionXWidth() {
    return position.getAbsoluteX() + this.width;
  }

  public int getPositionYHeight() {
    return position.getAbsoluteY() + this.height;
  }

  public int getWindowHeight() {
    return this.windowHeight;
  }

  public int getWindowWidth() {
    return this.windowWidth;
  }

  public int getWindowHeightScaled() {
    return this.guiScaledHeight;
  }

  public int getWindowWidthScaled() {
    return this.guiScaledWidth;
  }

  public void setPositionPoint(PositionPoint position) {
    this.position = position;
  }

  public void updateWindow() {
    if (window == null) {
      return;
    }
    int currentGuiScaledWidth = window.getGuiScaledWidth();
    int currentGuiScaleHeight = window.getGuiScaledHeight();
    if (guiScaledWidth == currentGuiScaledWidth && guiScaledHeight == currentGuiScaleHeight) {
      return;
    }
    this.guiScaledHeight = currentGuiScaleHeight;
    this.guiScaledWidth = currentGuiScaledWidth;
    this.windowHeight = window.getHeight();
    this.windowWidth = window.getWidth();
    updateBasePosition();
  }

  private int getWidthOrDefault() {
    return this.width == 0 ? this.defaultWidth : this.width;
  }

  private int getHeightOrDefault() {
    return this.height == 0 ? this.defaultHeight : this.height;
  }

  public enum BasePosition {
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    MIDDLE_LEFT,
    MIDDLE_RIGHT,
    TOP_LEFT,
    TOP_RIGHT
  }
}
