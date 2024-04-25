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

package de.markusbordihn.advancementstracker.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.markusbordihn.advancementstracker.Constants;
import de.markusbordihn.advancementstracker.client.advancements.AdvancementEntry;
import de.markusbordihn.advancementstracker.client.advancements.AdvancementsManager;
import de.markusbordihn.advancementstracker.client.gui.components.SmallButton;
import de.markusbordihn.advancementstracker.client.gui.panel.AdvancementCategoryPanel;
import de.markusbordihn.advancementstracker.client.gui.panel.AdvancementOverviewPanel;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class AdvancementsTrackerScreen extends Screen {

  protected static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private static final ResourceLocation miscTexture =
      new ResourceLocation(Constants.MOD_ID, "textures/gui/misc.png");
  private static final int PADDING = 10;
  private static final int STATUS_BAR_HEIGHT = 11;
  private static final int SCROLLBAR_WIDTH = 6;
  private static boolean showCompletedAdvancements = true;
  private static boolean showOnlyRewardedAdvancements = false;
  private static Screen parentScreen = null;
  Set<AdvancementEntry> rootAdvancements;
  Set<AdvancementEntry> childAdvancements;
  private int listWidth;
  private CategorySortType sortType = CategorySortType.NORMAL;
  private boolean sorted = false;
  private AdvancementEntry selectedRootAdvancement = null;
  private AdvancementEntry selectedChildAdvancement = null;
  private AdvancementCategoryPanel advancementCategoryPanel;
  private AdvancementOverviewPanel advancementOverviewPanel;
  private AdvancementDetailScreen showAdvancementDetailScreen;
  private boolean showAdvancementDetail = false;
  private int numberOfCompletedAdvancements = 0;
  private int numberOfRootAdvancements = 0;
  private int numberOfTotalAdvancements = 0;
  private int completedCheckboxX;
  private int onlyRewardedCheckboxX;

  public AdvancementsTrackerScreen() {
    this(new TextComponent("Advancements Tracker"));
  }

  public AdvancementsTrackerScreen(Component component) {
    super(component);
  }

  public static void toggleVisibility() {
    Minecraft minecraft = Minecraft.getInstance();
    if (!(minecraft.screen instanceof AdvancementsTrackerScreen)) {
      parentScreen = minecraft.screen;
      Minecraft.getInstance().setScreen(new AdvancementsTrackerScreen());
    } else if (minecraft.screen instanceof AdvancementsTrackerScreen) {
      Minecraft.getInstance().setScreen(parentScreen);
      parentScreen = null;
    }
  }

  private static void toggleShowCompletedAdvancements() {
    showCompletedAdvancements = !showCompletedAdvancements;
  }

  private static void toggleShowOnlyRewardedAdvancements() {
    showOnlyRewardedAdvancements = !showOnlyRewardedAdvancements;
  }

  public Minecraft getMinecraftInstance() {
    return minecraft;
  }

  public Font getFontRenderer() {
    return font;
  }

  public <T extends ObjectSelectionList.Entry<T>> void buildRootAdvancementsList(
      Consumer<T> listViewConsumer, Function<AdvancementEntry, T> newEntry) {
    if (this.rootAdvancements == null) {
      this.reloadRootAdvancements();
    }
    this.rootAdvancements.forEach(
        advancementEntry -> listViewConsumer.accept(newEntry.apply(advancementEntry)));
  }

  public void reloadRootAdvancements() {
    this.reloadRootAdvancements(CategorySortType.NORMAL);
  }

  public void reloadRootAdvancements(CategorySortType sortType) {
    if (sortType == CategorySortType.NORMAL) {
      this.rootAdvancements = AdvancementsManager.getRootAdvancements();
    } else {
      this.rootAdvancements = AdvancementsManager.getSortedRootAdvancements(sortType);
    }
    if (this.advancementCategoryPanel != null) {
      this.advancementCategoryPanel.refreshList();
    }
  }

  private void resortRootAdvancements(CategorySortType newSort) {
    this.sortType = newSort;

    for (CategorySortType sort : CategorySortType.values()) {
      if (sort.button != null) sort.button.active = sortType != sort;
    }
    sorted = false;
  }

  public AdvancementEntry getSelectedRootAdvancement() {
    return this.selectedRootAdvancement;
  }

  public void setSelectedRootAdvancement(AdvancementCategoryPanel.RootAdvancementEntry entry) {
    AdvancementEntry advancementEntry = entry.getAdvancementEntry();
    if (advancementEntry == null || this.selectedRootAdvancement == advancementEntry) {
      return;
    }
    this.selectedRootAdvancement = advancementEntry;
    log.debug("Selected root entry {}", this.selectedRootAdvancement);
    this.reloadChildAdvancements();
    this.numberOfCompletedAdvancements =
        AdvancementsManager.getNumberOfCompletedAdvancements(this.selectedRootAdvancement);
    this.numberOfTotalAdvancements =
        AdvancementsManager.getNumberOfAdvancements(this.selectedRootAdvancement);
  }

  public <T extends ObjectSelectionList.Entry<T>> void buildChildAdvancementsList(
      Consumer<T> listViewConsumer, Function<AdvancementEntry, T> newEntry) {
    if (this.childAdvancements == null) {
      return;
    }
    this.childAdvancements.forEach(
        advancementEntry -> {
          if ((showCompletedAdvancements || !advancementEntry.getProgress().isDone())
              && (!showOnlyRewardedAdvancements || advancementEntry.hasRewards())) {
            listViewConsumer.accept(newEntry.apply(advancementEntry));
          }
        });
  }

  public void reloadChildAdvancements() {
    this.reloadChildAdvancements(CategorySortType.NORMAL);
  }

  public void reloadChildAdvancements(CategorySortType sortType) {
    if (this.selectedRootAdvancement == null) {
      return;
    }
    if (sortType == CategorySortType.NORMAL) {
      this.childAdvancements = AdvancementsManager.getAdvancements(this.selectedRootAdvancement);
    } else {
      this.childAdvancements =
          AdvancementsManager.getSortedAdvancements(this.selectedRootAdvancement, sortType);
    }
    if (this.advancementOverviewPanel != null) {
      this.advancementOverviewPanel.refreshList();
    }
  }

  public void showAdvancementDetail(boolean visible) {
    this.showAdvancementDetail = visible;
    this.showAdvancementDetailScreen =
        visible && this.selectedChildAdvancement != null
            ? new AdvancementDetailScreen(this.selectedChildAdvancement)
            : null;
    if (this.showAdvancementDetailScreen != null) {
      this.showAdvancementDetailScreen.init(this.minecraft, width, height);
    }
  }

  public AdvancementEntry getSelectedChildAdvancement() {
    return this.selectedChildAdvancement;
  }

  public void setSelectedChildAdvancement(AdvancementOverviewPanel.ChildAdvancementEntry entry) {
    AdvancementEntry advancementEntry = entry.getAdvancementEntry();
    if (this.selectedChildAdvancement == advancementEntry) {
      return;
    }
    this.selectedChildAdvancement = advancementEntry;
    log.debug("Selected child entry {}", this.selectedChildAdvancement);
  }

  private void renderNumberOfRootAdvancements(PoseStack poseStack) {
    if (numberOfRootAdvancements > 0) {
      float scaleFactor = 0.75f;
      TranslatableComponent text =
          new TranslatableComponent(
              Constants.ADVANCEMENTS_SCREEN_PREFIX + "numCategories", numberOfRootAdvancements);
      poseStack.pushPose();
      poseStack.scale(scaleFactor, scaleFactor, scaleFactor);
      font.drawShadow(
          poseStack,
          text,
          (this.listWidth - PADDING - 52.0f) / scaleFactor,
          (this.height - 8) / scaleFactor,
          0xFFFFFF);
      font.draw(
          poseStack,
          text,
          (this.listWidth - PADDING - 52.0f) / scaleFactor,
          (this.height - 8) / scaleFactor,
          0xFFFFFF);
      poseStack.popPose();
    }
  }

  private void renderAdvancementsStats(PoseStack poseStack) {
    if (this.numberOfTotalAdvancements > 0) {
      float scaleFactor = 0.75f;
      TranslatableComponent text =
          new TranslatableComponent(
              Constants.ADVANCEMENTS_SCREEN_PREFIX + "numCompleted",
              this.numberOfCompletedAdvancements,
              this.numberOfTotalAdvancements);

      poseStack.pushPose();
      poseStack.scale(scaleFactor, scaleFactor, scaleFactor);
      font.drawShadow(
          poseStack,
          text,
          (width - 92.0f) / scaleFactor,
          (this.height - 8) / scaleFactor,
          0xFFFFFF);
      font.draw(
          poseStack,
          text,
          (width - 92.0f) / scaleFactor,
          (this.height - 8) / scaleFactor,
          0xFFFFFF);
      poseStack.popPose();
    }
  }

  private void renderCompletedCheckbox(PoseStack poseStack) {
    RenderSystem.setShaderColor(1, 1, 1, 1);
    RenderSystem.setShaderTexture(0, miscTexture);

    this.completedCheckboxX = this.listWidth;
    float scaleFactorIcon = 0.6f;
    poseStack.pushPose();
    poseStack.scale(scaleFactorIcon, scaleFactorIcon, scaleFactorIcon);
    GuiComponent.blit(
        poseStack,
        Math.round(this.completedCheckboxX / scaleFactorIcon),
        Math.round((this.height - 10) / scaleFactorIcon),
        showCompletedAdvancements ? 42 : 22,
        6,
        15,
        15,
        256,
        256);
    poseStack.popPose();

    float scaleFactorText = 0.75f;
    TranslatableComponent text =
        new TranslatableComponent(Constants.ADVANCEMENTS_SCREEN_PREFIX + "showCompleted");

    poseStack.pushPose();
    poseStack.scale(scaleFactorText, scaleFactorText, scaleFactorText);
    font.drawShadow(
        poseStack,
        text,
        Math.round((this.listWidth + 12.0f) / scaleFactorText),
        Math.round((this.height - 8) / scaleFactorText),
        0xFFFFFF);
    font.draw(
        poseStack,
        text,
        Math.round((this.listWidth + 12.0f) / scaleFactorText),
        Math.round((this.height - 8) / scaleFactorText),
        0xFFFFFF);
    poseStack.popPose();
  }

  private void renderOnlyRewardedCheckbox(PoseStack poseStack) {
    RenderSystem.setShaderColor(1, 1, 1, 1);
    RenderSystem.setShaderTexture(0, miscTexture);

    this.onlyRewardedCheckboxX = this.listWidth + 78;
    float scaleFactorIcon = 0.6f;
    poseStack.pushPose();
    poseStack.scale(scaleFactorIcon, scaleFactorIcon, scaleFactorIcon);
    GuiComponent.blit(
        poseStack,
        Math.round(this.onlyRewardedCheckboxX / scaleFactorIcon),
        Math.round((this.height - 10) / scaleFactorIcon),
        showOnlyRewardedAdvancements ? 42 : 22,
        6,
        15,
        15,
        256,
        256);
    poseStack.popPose();

    float scaleFactorText = 0.75f;
    int fontColor = showOnlyRewardedAdvancements ? 0xFF0000 : 0xFFFFFF;
    TranslatableComponent text =
        new TranslatableComponent(Constants.ADVANCEMENTS_SCREEN_PREFIX + "showOnlyRewarded");
    poseStack.pushPose();
    poseStack.scale(scaleFactorText, scaleFactorText, scaleFactorText);
    font.drawShadow(
        poseStack,
        text,
        Math.round((this.listWidth + 90.0f) / scaleFactorText),
        Math.round((this.height - 8) / scaleFactorText),
        fontColor);
    font.draw(
        poseStack,
        text,
        Math.round((this.listWidth + 90.0f) / scaleFactorText),
        Math.round((this.height - 8) / scaleFactorText),
        fontColor);
    poseStack.popPose();
  }

  public boolean showingAdvancementDetail() {
    return this.showAdvancementDetail
        && this.selectedChildAdvancement != null
        && this.showAdvancementDetailScreen != null;
  }

  @Override
  protected void init() {
    super.init();

    // Calculate viewport and general design
    this.listWidth = Math.max(width / 3, 100);
    int topPosition = PADDING + 10;

    // Panel Positions
    int categoryPanelLeftPosition = 0;

    // Define scroll panels
    this.advancementCategoryPanel =
        new AdvancementCategoryPanel(
            this,
            this.listWidth,
            topPosition,
            categoryPanelLeftPosition,
            height - STATUS_BAR_HEIGHT);
    this.advancementOverviewPanel =
        new AdvancementOverviewPanel(
            this,
            width - this.listWidth - (2 * SCROLLBAR_WIDTH) - 1,
            topPosition,
            this.advancementCategoryPanel.getWidth() + SCROLLBAR_WIDTH,
            height - STATUS_BAR_HEIGHT);

    // Add Scroll panels for advancements
    this.addRenderableWidget(this.advancementCategoryPanel);
    this.addRenderableWidget(this.advancementOverviewPanel);

    // Sort Buttons for root advancements
    int buttonPositionX = 5;
    int buttonPositionY = this.height - 11;
    CategorySortType.NORMAL.button =
        new SmallButton(
            buttonPositionX,
            buttonPositionY,
            20,
            10,
            CategorySortType.NORMAL.getButtonText(),
            b -> resortRootAdvancements(CategorySortType.NORMAL));
    this.addRenderableWidget(CategorySortType.NORMAL.button);
    int buttonMargin = 1;
    buttonPositionX += 20 + buttonMargin;
    CategorySortType.A_TO_Z.button =
        new SmallButton(
            buttonPositionX,
            buttonPositionY,
            20,
            10,
            CategorySortType.A_TO_Z.getButtonText(),
            b -> resortRootAdvancements(CategorySortType.A_TO_Z));
    this.addRenderableWidget(CategorySortType.A_TO_Z.button);
    buttonPositionX += 20 + buttonMargin;
    CategorySortType.Z_TO_A.button =
        new SmallButton(
            buttonPositionX,
            buttonPositionY,
            20,
            10,
            CategorySortType.Z_TO_A.getButtonText(),
            b -> resortRootAdvancements(CategorySortType.Z_TO_A));
    this.addRenderableWidget(CategorySortType.Z_TO_A.button);
    reloadRootAdvancements();

    // Sort Buttons for child Advancements
    reloadChildAdvancements();

    // Cache specific numbers
    this.numberOfRootAdvancements = AdvancementsManager.getNumberOfRootAdvancements();
  }

  @Override
  public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
    this.renderBackground(poseStack);

    // Render panels for category and overview
    this.advancementCategoryPanel.render(poseStack, mouseX, mouseY, partialTick);
    this.advancementOverviewPanel.render(poseStack, mouseX, mouseY, partialTick);

    super.render(poseStack, mouseX, mouseY, partialTick);

    // Render stats
    this.renderNumberOfRootAdvancements(poseStack);
    this.renderAdvancementsStats(poseStack);

    // Title
    font.draw(poseStack, this.title, this.listWidth + PADDING + 10f, 8, 16777215);

    // Checkbox for show/hide completed Advancements
    this.renderCompletedCheckbox(poseStack);

    // Checkbox for show/hide rewarded Advancements
    this.renderOnlyRewardedCheckbox(poseStack);

    // Advancement details
    if (this.showingAdvancementDetail()) {
      this.showAdvancementDetailScreen.render(poseStack, mouseX, mouseY, partialTick);
    }
  }

  @Override
  public void renderBackground(PoseStack poseStack) {
    // Background
    this.fillGradient(poseStack, 0, 0, this.width, this.height, -1072689136, -804253680);
    this.fillGradient(poseStack, 0, height - 12, this.width, this.height, -1072689136, -804253680);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (button != 0) {
      super.mouseClicked(mouseX, mouseY, button);
    }
    if (this.showingAdvancementDetail()) {
      if (this.showAdvancementDetailScreen.isMouseOver(mouseX, mouseY)) {
        this.showAdvancementDetailScreen.mouseClicked(mouseX, mouseY, button);
      } else {
        // Ignore events, if we are showing the advancement details.
        this.showAdvancementDetail(false);
      }
      return false;
    } else if (mouseX > this.completedCheckboxX
        && mouseX < this.completedCheckboxX + 8
        && mouseY > this.height - 11) {
      // Handle clicks on the show complete advancements' checkbox.
      toggleShowCompletedAdvancements();
      reloadChildAdvancements();
      return false;
    } else if (mouseX > this.onlyRewardedCheckboxX
        && mouseX < this.onlyRewardedCheckboxX + 8
        && mouseY > this.height - 11) {
      // Handle clicks on the show only rewarded advancements.
      toggleShowOnlyRewardedAdvancements();
      reloadChildAdvancements();
      return false;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
    if (this.showingAdvancementDetail()) {
      this.showAdvancementDetailScreen.mouseScrolled(mouseX, mouseY, scroll);
    }
    return super.mouseScrolled(mouseX, mouseY, scroll);
  }

  @Override
  public boolean mouseDragged(
      double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    if (this.showingAdvancementDetail()) {
      this.showAdvancementDetailScreen.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
  }

  @Override
  public void tick() {
    if (!sorted) {
      reloadRootAdvancements(sortType);
      reloadChildAdvancements(sortType);
      sorted = true;
    }
  }

  @Override
  public boolean keyPressed(int key1, int key2, int key3) {
    if (key1 == GLFW.GLFW_KEY_ESCAPE && this.showingAdvancementDetail()) {
      this.showAdvancementDetail(false);
      return false;
    } else {
      return super.keyPressed(key1, key2, key3);
    }
  }

  // Sorting Support
  public enum CategorySortType implements Comparator<AdvancementEntry> {
    NORMAL,
    A_TO_Z {
      @Override
      protected int compare(String name1, String name2) {
        return name1.compareTo(name2);
      }
    },
    Z_TO_A {
      @Override
      protected int compare(String name1, String name2) {
        return name2.compareTo(name1);
      }
    };

    Button button;

    protected int compare(String name1, String name2) {
      return name1.equals(name2) ? 1 : 0;
    }

    @Override
    public int compare(AdvancementEntry advancement1, AdvancementEntry advancement2) {
      return compare(advancement1.getSortName(), advancement2.getSortName());
    }

    Component getButtonText() {
      return new TranslatableComponent(
          Constants.MOD_PREFIX + "sort." + StringUtils.toLowerCase(name()));
    }
  }
}
