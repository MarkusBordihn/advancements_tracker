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

public class PositionPoint {

  private int x;
  private int y;
  private int offsetX;
  private int offsetY;
  private int absoluteX;
  private int absoluteY;

  public PositionPoint() {
    this(0, 0, 0, 0);
  }

  public PositionPoint(int x, int y) {
    this(x, y, 0, 0);
  }

  public PositionPoint(int x, int y, int offsetX, int offsetY) {
    this.offsetX = offsetX;
    this.offsetY = offsetY;
    this.x = x;
    this.y = y;
    recalculateAbsolutePosition();
  }

  public int getAbsoluteX() {
    return this.absoluteX;
  }

  public int getAbsoluteY() {
    return this.absoluteY;
  }

  public int getX() {
    return this.x;
  }

  public void setX(int x) {
    this.x = x;
    recalculateAbsolutePosition();
  }

  public int getY() {
    return this.y;
  }

  public void setY(int y) {
    this.y = y;
    recalculateAbsolutePosition();
  }

  public int getOffsetX() {
    return this.offsetX;
  }

  public void setOffsetX(int offsetX) {
    this.offsetX = offsetX;
    recalculateAbsolutePosition();
  }

  public int getOffsetY() {
    return this.offsetY;
  }

  public void setOffsetY(int offsetY) {
    this.offsetY = offsetY;
    recalculateAbsolutePosition();
  }

  public void setOffset(PositionPoint positionPoint) {
    setOffsetX(positionPoint.getX());
    setOffsetY(positionPoint.getY());
  }

  private void recalculateAbsolutePosition() {
    // Cache absolute position and make sure it's always greater than 0.
    this.absoluteX = Math.max(0, this.offsetX + this.x);
    this.absoluteY = Math.max(0, this.offsetY + this.y);
  }

  public String toString() {
    return "PositionPoint{x:"
        + this.x
        + ", y:"
        + this.y
        + ", offsetX:"
        + this.offsetX
        + ", offsetY:"
        + this.offsetY
        + "}";
  }
}
