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

package de.markusbordihn.advancementstracker.client.advancements;

import com.google.common.collect.Lists;
import de.markusbordihn.advancementstracker.Constants;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdvancementEntryProgress {

  protected static final Logger log = LogManager.getLogger(Constants.LOG_NAME);
  // Helper Tools
  private final Font font;
  private final Minecraft minecraft;
  private AdvancementProgress advancementProgress;
  private Map<String, CriterionProgress> criteriaMap = new HashMap<>();
  // Ids
  private ResourceLocation id;
  private String namespace = "";
  // Progress with default values
  private Date firstProgressDate;
  private Date lastProgressDate;
  private Float progress = 0f;
  private String progressString = "";
  private boolean isDone = false;
  private int progressStringWidth = 0;
  private int progressTotal = 0;
  // Criteria
  private Iterable<String> completedCriteria;
  private Iterable<String> remainingCriteria;
  private int completedCriteriaNumber;
  private int remainingCriteriaNumber;
  private int maxCriteraRequired;

  AdvancementEntryProgress(Advancement advancement, AdvancementProgress advancementProgress) {
    // General Helper Tools
    this.minecraft = Minecraft.getInstance();
    this.font = this.minecraft.font;

    // ID's
    this.id = advancement.getId();
    this.namespace = advancement.getId().getNamespace();

    // Advancement Progress
    this.maxCriteraRequired = advancement.getMaxCriteraRequired();
    this.advancementProgress = advancementProgress;
    update(this.advancementProgress);
  }

  public void update(AdvancementProgress advancementProgress) {
    if (advancementProgress == null) {
      return;
    }

    this.advancementProgress = advancementProgress;
    this.isDone = advancementProgress.isDone();
    this.firstProgressDate = advancementProgress.getFirstProgressDate();
    this.progress = advancementProgress.getPercent();

    // Handle completed Criteria
    this.completedCriteria = advancementProgress.getCompletedCriteria();
    this.completedCriteriaNumber = (int) this.completedCriteria.spliterator().getExactSizeIfKnown();
    for (String criteriaId : this.completedCriteria) {
      criteriaMap.put(criteriaId, advancementProgress.getCriterion(criteriaId));
    }

    // Handle remaining Criteria
    this.remainingCriteria = advancementProgress.getRemainingCriteria();
    this.remainingCriteriaNumber = (int) this.remainingCriteria.spliterator().getExactSizeIfKnown();
    for (String criteriaId : this.remainingCriteria) {
      criteriaMap.put(criteriaId, advancementProgress.getCriterion(criteriaId));
    }

    // Number of complete Criteria
    if (this.remainingCriteriaNumber > 0 || this.completedCriteriaNumber > 0) {
      this.progressTotal = this.completedCriteriaNumber + this.remainingCriteriaNumber;
      this.progressString = this.completedCriteriaNumber + "/" + this.progressTotal;
      this.progressStringWidth = font.width(this.progressString);
    }

    this.lastProgressDate = this.findLastProgressDate();
  }

  public String getNamespace() {
    return this.namespace;
  }

  public boolean isDone() {
    return this.isDone;
  }

  public int getMaxCriteraRequired() {
    return this.maxCriteraRequired;
  }

  public int getCompletedCriteriaNumber() {
    return this.completedCriteriaNumber;
  }

  public int getRemainingCriteriaNumber() {
    return this.remainingCriteriaNumber;
  }

  public Date getFirstProgressDate() {
    return this.firstProgressDate;
  }

  public Date getLastProgressDate() {
    return this.lastProgressDate;
  }

  public String getProgressString() {
    return this.progressString;
  }

  public int getProgressStringWidth() {
    return this.progressStringWidth;
  }

  public int getProgressTotal() {
    return this.progressTotal;
  }

  public float getProgress() {
    return this.progress;
  }

  public Iterable<String> getCompletedCriteria() {
    return this.completedCriteria;
  }

  public Iterable<String> getCompletedCriteriaHumanReadable() {
    List<String> result = Lists.newArrayList();
    for (String criteria : this.completedCriteria) {
      result.add(getHumanReadableName(criteria));
    }
    return result;
  }

  public Iterable<String> getRemainingCriteria() {
    return this.remainingCriteria;
  }

  public Iterable<String> getRemainingCriteriaHumanReadable() {
    List<String> result = Lists.newArrayList();
    for (String criteria : this.remainingCriteria) {
      result.add(getHumanReadableName(criteria));
    }
    return result;
  }

  private Date findLastProgressDate() {
    Date date = null;
    for (CriterionProgress criterionProgress : this.criteriaMap.values()) {
      Date obtainedDate = criterionProgress.getObtained();
      if (criterionProgress.isDone()
          && (date == null || (obtainedDate != null && obtainedDate.after(date)))) {
        date = criterionProgress.getObtained();
      }
    }
    return date;
  }

  private String getHumanReadableName(String criteria) {

    // Try to translate the name, if we got a namespace.
    List<String> namespaces = Lists.newArrayList();
    if (namespace != null) {
      namespaces.add(namespace);
      if (!namespace.equals("minecraft")) {
        namespaces.add("minecraft");
      }
    }

    for (String possibleNamespace : namespaces) {

      // Normalize names for the namespace.
      String criteriaName =
          criteria.startsWith(possibleNamespace + ":")
              ? criteria.replace(possibleNamespace + ":", "")
              : criteria;

      // Check for item in namespace.
      String itemNameFormat = "item." + possibleNamespace + "." + criteriaName;
      TranslatableComponent itemName = new TranslatableComponent(itemNameFormat);
      if (!itemName.getString().equals(itemNameFormat)) {
        return itemName.getString();
      }

      // Check for block in namespace.
      String blockNameFormat = "block." + possibleNamespace + "." + criteriaName;
      TranslatableComponent blockName = new TranslatableComponent(blockNameFormat);
      if (!blockName.getString().equals(blockNameFormat)) {
        return blockName.getString();
      }

      // Check for entity in namespace.
      String entityNameFormat = "entity." + possibleNamespace + "." + criteriaName;
      TranslatableComponent entityName = new TranslatableComponent(entityNameFormat);
      if (!entityName.getString().equals(entityNameFormat)) {
        return entityName.getString();
      }

      // Check for enchantment in namespace.
      String enchantmentNameFormat = "enchantment." + possibleNamespace + "." + criteriaName;
      TranslatableComponent enchantmentName = new TranslatableComponent(enchantmentNameFormat);
      if (!enchantmentName.getString().equals(enchantmentNameFormat)) {
        return enchantmentName.getString();
      }

      // Check for effect in namespace.
      String effectNameFormat = "effect." + possibleNamespace + "." + criteriaName;
      TranslatableComponent effectName = new TranslatableComponent(effectNameFormat);
      if (!effectName.getString().equals(effectNameFormat)) {
        return effectName.getString();
      }

      // Check for biome in namespace.
      String biomeNameFormat = "biome." + possibleNamespace + "." + criteriaName;
      TranslatableComponent biomeName = new TranslatableComponent(biomeNameFormat);
      if (!biomeName.getString().equals(biomeNameFormat)) {
        return biomeName.getString();
      }
    }

    String advancementNameFormat =
        "advancement."
            + this.id.toString().replace(":", ".").replace("/", ".")
            + "."
            + criteria.replace(":", ".").replace("/", ".");
    TranslatableComponent advancementName = new TranslatableComponent(advancementNameFormat);
    if (!advancementName.getString().equals(advancementNameFormat)) {
      return advancementName.getString();
    }

    log.warn(
        "Unable to translate {} ({}) to a more meaningful name.",
        criteria,
        advancementNameFormat);
    return criteria;
  }
}
