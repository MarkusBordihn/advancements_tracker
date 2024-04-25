/*
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import de.markusbordihn.advancementstracker.Constants;
import java.util.Comparator;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdvancementEntry implements Comparator<AdvancementEntry> {

  protected static final Logger log = LogManager.getLogger(Constants.LOG_NAME);
  // General
  private final ResourceLocation id;
  private final String idString;
  // Text Components
  private final TextComponent descriptionComponent;
  private final TextComponent titleComponent;
  // Helper Tools
  private final Font font;
  private final Minecraft minecraft;
  Advancement advancement;
  Advancement rootAdvancement;
  DisplayInfo displayInfo;
  ResourceLocation rootId;
  String[][] requirements;
  int rootLevel = 0;
  // Display Information
  private ItemStack icon;
  private ResourceLocation background;
  private String description;
  private String title;
  private FrameType frameType;
  private int titleWidth;
  private int descriptionColor = 0xFFCCCCCC;
  private int titleColor = 0xFFFFFFFF;
  // Rewards
  private AdvancementRewards rewards = null;
  private ResourceLocation[] rewardsLoot = null;
  private ResourceLocation[] rewardsRecipes = null;
  private Integer rewardsExperience = null;
  private boolean hasExperienceReward = false;
  private boolean hasLootReward = false;
  private boolean hasRecipesReward = false;
  private boolean hasRewards = false;
  private boolean hasRewardsData = false;
  private boolean hasRewardsLoaded = false;
  // Progress
  private AdvancementEntryProgress advancementProgress;

  AdvancementEntry(Advancement advancement, AdvancementProgress advancementProgress) {
    // General Helper Tools
    this.minecraft = Minecraft.getInstance();
    this.font = this.minecraft.font;

    // Advancement Progress
    this.advancementProgress = new AdvancementEntryProgress(advancement, advancementProgress);

    // Advancements Data
    this.advancement = advancement;
    this.displayInfo = advancement.getDisplay();
    this.id = advancement.getId();
    this.idString = advancement.getId().toString();
    this.rootAdvancement = advancement.getParent();
    this.requirements = advancement.getRequirements();

    if (this.rootAdvancement != null) {
      while (this.rootAdvancement.getParent() != null) {
        this.rootAdvancement = this.rootAdvancement.getParent();
        rootLevel++;
      }
      this.rootId = this.rootAdvancement.getId();
    }

    // Handle display information like background, colors and description.
    if (this.displayInfo != null) {
      this.background = this.displayInfo.getBackground();

      // Title
      this.icon = this.displayInfo.getIcon();
      this.title = this.displayInfo.getTitle().getString();
      this.titleWidth = this.font.width(this.title);
      TextColor titleTextColor = this.displayInfo.getTitle().getStyle().getColor();
      if (titleTextColor != null) {
        this.titleColor = titleTextColor.getValue();
      }

      // Description
      this.description = this.displayInfo.getDescription().getString();
      TextColor descriptionTextColor = this.displayInfo.getDescription().getStyle().getColor();
      if (descriptionTextColor != null) {
        this.descriptionColor = descriptionTextColor.getValue();
      }

      this.frameType = this.displayInfo.getFrame();
    } else {
      this.background = null;
      this.title = advancement.getId().toString();
      this.titleWidth = this.font.width(this.title);
    }

    // Use background from root advancement if we don't have any itself.
    if (this.background == null
        && this.rootAdvancement != null
        && this.rootAdvancement.getDisplay() != null
        && this.rootAdvancement.getDisplay().getBackground() != null) {
      this.background = this.rootAdvancement.getDisplay().getBackground();
    }

    // Stripped version for ui renderer.
    this.descriptionComponent = new TextComponent(stripControlCodes(this.description));
    this.titleComponent = new TextComponent(stripControlCodes(this.title));

    // Handle Rewards like experience, loot and recipes.
    this.rewards = advancement.getRewards();
  }

  private static String stripControlCodes(String value) {
    return value == null ? "" : net.minecraft.util.StringUtil.stripColor(value);
  }

  public static Comparator<AdvancementEntry> sortByTitle() {
    return Comparator.comparing(
        (AdvancementEntry firstAdvancementEntry) -> firstAdvancementEntry.title);
  }

  public static Comparator<AdvancementEntry> sortByStatus() {
    return (AdvancementEntry firstAdvancementEntry, AdvancementEntry secondAdvancementEntry) -> {
      int result =
          Boolean.compare(
              firstAdvancementEntry.getProgress().isDone(),
              secondAdvancementEntry.getProgress().isDone());
      if (result == 0) {
        result = firstAdvancementEntry.title.compareTo(secondAdvancementEntry.title);
      }
      return result;
    };
  }

  public boolean isTracked() {
    return TrackedAdvancementsManager.isTrackedAdvancement(advancement);
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

  public TextComponent getDescription() {
    return this.descriptionComponent;
  }

  public String getDescriptionString() {
    return this.description;
  }

  public int getDescriptionColor() {
    return this.descriptionColor;
  }

  public String getSortName() {
    return StringUtils.toLowerCase(stripControlCodes(this.title));
  }

  public TextComponent getTitle() {
    return this.titleComponent;
  }

  public String getTitleString() {
    return this.title;
  }

  public int getTitleWidth() {
    return this.titleWidth;
  }

  public int getTitleColor() {
    return this.titleColor;
  }

  public void updateAdvancementProgress(AdvancementProgress advancementProgress) {
    this.advancementProgress.update(advancementProgress);
  }

  public Integer getRewardsExperience() {
    if (this.rewardsExperience == null) {
      JsonObject rewardsData = getRewardsData();
      if (rewardsData != null) {
        // Getting rewards experience
        this.rewardsExperience = GsonHelper.getAsInt(rewardsData, "experience", 0);
        if (this.rewardsExperience > 0) {
          this.hasExperienceReward = true;
          this.hasRewardsData = true;
        }
      }
    }
    return this.rewardsExperience;
  }

  public ResourceLocation[] getRewardsLoot() {
    if (this.rewardsLoot == null) {
      JsonObject rewardsData = getRewardsData();
      if (rewardsData != null) {
        // Getting Loot entries
        JsonArray lootArray = GsonHelper.getAsJsonArray(rewardsData, "loot", new JsonArray());
        this.rewardsLoot = new ResourceLocation[lootArray.size()];
        for (int j = 0; j < this.rewardsLoot.length; ++j) {
          this.rewardsLoot[j] =
              new ResourceLocation(GsonHelper.convertToString(lootArray.get(j), "loot[" + j + "]"));
          this.hasLootReward = true;
          this.hasRewardsData = true;
        }
      }
    }
    return this.rewardsLoot;
  }

  public ResourceLocation[] getRewardsRecipes() {
    if (this.rewardsRecipes == null) {
      JsonObject rewardsData = getRewardsData();
      if (rewardsData != null) {
        // Getting recipes entries
        JsonArray recipesArray = GsonHelper.getAsJsonArray(rewardsData, "recipes", new JsonArray());
        this.rewardsRecipes = new ResourceLocation[recipesArray.size()];
        for (int k = 0; k < this.rewardsRecipes.length; ++k) {
          this.rewardsRecipes[k] =
              new ResourceLocation(
                  GsonHelper.convertToString(recipesArray.get(k), "recipes[" + k + "]"));
          this.hasRecipesReward = true;
          this.hasRewardsData = true;
        }
      }
    }
    return this.rewardsRecipes;
  }

  private JsonObject getRewardsData() {
    // There is no direct access to the rewards information, for this reason we
    // are using the JsonObject to get access to the relevant information.
    JsonElement rewardsJson;
    try {
      rewardsJson = this.rewards.serializeToJson();
      JsonObject rewardsObject = rewardsJson.getAsJsonObject();
      if (rewardsObject != null) {
        return rewardsObject;
      }
    } catch (JsonParseException | IllegalStateException e) {
      // Ignore possible JSON Parse Exception and illegal state exceptions
    }
    return null;
  }

  public boolean hasRewards() {
    if (!this.hasRewardsLoaded && this.rewards != null) {
      this.hasRewards =
          getRewardsExperience() != null || getRewardsLoot() != null || getRewardsRecipes() != null;
      this.hasRewardsLoaded = true;
    }
    return this.hasRewards;
  }

  public boolean hasRewardsData() {
    return this.hasRewards() && this.hasRewardsData;
  }

  public boolean hasExperienceReward() {
    return this.hasExperienceReward;
  }

  public boolean hasLootReward() {
    return this.hasLootReward;
  }

  public boolean hasRecipesReward() {
    return this.hasRecipesReward;
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) {
      return false;
    }
    if (!(object instanceof AdvancementEntry)) {
      return false;
    }
    if (object == this) {
      return true;
    }
    return this.id == ((AdvancementEntry) object).id;
  }

  @Override
  public int hashCode() {
    return this.id.hashCode();
  }

  @Override
  public int compare(
      AdvancementEntry firstAdvancementEntry, AdvancementEntry secondAdvancementEntry) {
    return firstAdvancementEntry.id.compareTo(secondAdvancementEntry.id);
  }

  @Override
  public String toString() {
    if (this.rootAdvancement == null) {
      return String.format(
          "[Root Advancement] (%s) %s: %s %s",
          this.frameType, this.id, this.title, this.advancementProgress.getProgress());
    }
    return String.format(
        "[Advancement %s] (%s) %s => %s: %s %s",
        this.rootLevel,
        this.frameType,
        this.rootId,
        this.id,
        this.title,
        this.advancementProgress.getProgress());
  }
}
