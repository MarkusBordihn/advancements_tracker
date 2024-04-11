/**
 * Copyright 2021 Markus Bordihn
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

package de.markusbordihn.advancementstracker.client.advancements;

import java.util.Comparator;

import java.util.List;
import java.util.Locale;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.util.StringUtil;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class AdvancementEntry implements Comparator<AdvancementEntry> {

  private final Advancement advancement;

  final ResourceLocation rootId;

  private final int rootLevel;

  // General
  private final ResourceLocation id;
  private final String idString;

  // Display Information
  private ItemStack icon;
  private ResourceLocation background;
  private String description;
  private final String title;
  private AdvancementType frameType;

  // Text Components
  private final Component descriptionComponent;
  private final Component titleComponent;
  private int descriptionColor = 0xFFDDDDDD;
  private int titleColor = 0xFFFFFFFF;

  // Rewards
  private final AdvancementRewards rewards;
  private boolean hasRewards = false;
  private boolean hasRewardsLoaded = false;

  // Progress
  private final AdvancementEntryProgress advancementProgress;

  AdvancementEntry(AdvancementHolder advancementHolder, AdvancementProgress advancementProgress) {
    this(advancementHolder, advancementProgress, null, 0);
  }

  AdvancementEntry(AdvancementHolder advancementHolder, AdvancementProgress advancementProgress, @Nullable AdvancementNode root, int rootLevel) {
    // Advancements Data
    this.advancement = advancementHolder.value();
    this.id = advancementHolder.id();
    this.idString = id.toString();
    this.rootId = root == null ? null : root.holder().id();
    this.rootLevel = rootLevel;

    // Advancement Progress
    this.advancementProgress = new AdvancementEntryProgress(advancementHolder, advancementProgress);

    // Handle display information like background, colors and description.
    if (advancement.display().isPresent()) {
      DisplayInfo displayInfo = advancement.display().get();
      this.background = displayInfo.getBackground().orElse(null);

      // Title
      this.icon = displayInfo.getIcon();
      this.title = displayInfo.getTitle().getString();
      TextColor titleTextColor = displayInfo.getTitle().getStyle().getColor();
      if (titleTextColor != null) {
        this.titleColor = titleTextColor.getValue();
      }

      // Description
      this.description = displayInfo.getDescription().getString();
      TextColor descriptionTextColor = displayInfo.getDescription().getStyle().getColor();
      if (descriptionTextColor != null) {
        this.descriptionColor = descriptionTextColor.getValue();
      }

      this.frameType = displayInfo.getType();
    } else {
      this.background = null;
      this.title = idString;
    }

    // Use background from root advancement if we don't have any itself.
    if (this.background == null && root != null) {
      this.background = root.advancement().display().flatMap(DisplayInfo::getBackground).orElse(null);
    }

    // Stripped version for ui renderer.
    this.descriptionComponent = Component.literal(stripControlCodes(this.description));
    this.titleComponent = Component.literal(stripControlCodes(this.title));

    // Handle Rewards like experience, loot and recipes.
    this.rewards = advancement.rewards();
  }

  public boolean isTracked() {
    return TrackedAdvancementsManager.isTrackedAdvancement(getId());
  }

  public AdvancementEntryProgress getProgress() {
    return this.advancementProgress;
  }

  public ResourceLocation getId() {
    return this.id;
  }

  public String getIdString() {
    return this.idString;
  }

  public ResourceLocation getBackground() {
    return this.background;
  }

  public ItemStack getIcon() {
    return this.icon;
  }

  public Advancement getAdvancement() {
    return this.advancement;
  }

  public Component getDescription() {
    return this.descriptionComponent;
  }

  public String getDescriptionString() {
    return this.description;
  }

  public int getDescriptionColor() {
    return this.descriptionColor;
  }

  public String getSortName() {
    return stripControlCodes(this.title).toLowerCase(Locale.ROOT);
  }

  public Component getTitle() {
    return this.titleComponent;
  }

  public String getTitleString() {
    return this.title;
  }

  public int getTitleColor() {
    return this.titleColor;
  }

  public void updateAdvancementProgress(AdvancementProgress advancementProgress) {
    this.advancementProgress.update(advancementProgress);
  }

  public int getRewardsExperience() {
    return this.rewards.experience();
  }

  public List<ResourceLocation> getRewardsLoot() {
    return this.rewards.loot();
  }

  public List<ResourceLocation> getRewardsRecipes() {
    return this.rewards.recipes();
  }

  public boolean hasRewards() {
    if (!this.hasRewardsLoaded && this.rewards != null) {
      this.hasRewards = hasExperienceReward() || hasLootReward() || hasRecipesReward();
      this.hasRewardsLoaded = true;
    }
    return this.hasRewards;
  }

  public boolean hasExperienceReward() {
    return getRewardsExperience() > 0;
  }

  public boolean hasLootReward() {
    return !getRewardsLoot().isEmpty();
  }

  public boolean hasRecipesReward() {
    return !getRewardsRecipes().isEmpty();
  }

  private static String stripControlCodes(String value) {
    return value == null ? "" : StringUtil.stripColor(value);
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (!(object instanceof AdvancementEntry other)) {
      return false;
    }
    return this.id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return this.id.hashCode();
  }

  @Override
  public int compare(AdvancementEntry firstAdvancementEntry,
      AdvancementEntry secondAdvancementEntry) {
    return firstAdvancementEntry.id.compareTo(secondAdvancementEntry.id);
  }

  public static Comparator<AdvancementEntry> sortByTitle() {
    return Comparator.comparing(entry -> entry.title);
  }

  public static Comparator<AdvancementEntry> sortByStatus() {
    return (AdvancementEntry firstAdvancementEntry, AdvancementEntry secondAdvancementEntry) -> {
      int result = Boolean.compare(firstAdvancementEntry.getProgress().isDone(),
          secondAdvancementEntry.getProgress().isDone());
      if (result == 0) {
        result = firstAdvancementEntry.title.compareTo(secondAdvancementEntry.title);
      }
      return result;
    };
  }

  @Override
  public String toString() {
    if (this.rootId == null) {
      return String.format("[Root Advancement] (%s) %s: %s %s", this.frameType, this.id, this.title,
          this.advancementProgress.getProgress());
    }
    return String.format("[Advancement %s] (%s) %s => %s: %s %s", this.rootLevel, this.frameType,
        this.rootId, this.id, this.title, this.advancementProgress.getProgress());
  }

}
