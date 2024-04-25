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

import de.markusbordihn.advancementstracker.Constants;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(Dist.CLIENT)
public class AdvancementsManager {

  protected static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private static AdvancementEntry selectedAdvancement;
  private static AdvancementEntry selectedRootAdvancement;
  private static Map<Advancement, AdvancementProgress> advancementProgressMap = new HashMap<>();
  private static Map<ResourceLocation, Set<AdvancementEntry>> advancementsMap = new HashMap<>();
  private static Set<AdvancementEntry> rootAdvancements = new HashSet<>();
  private static Set<String> advancementsIndex = new HashSet<>();
  private static boolean hasAdvancements = false;

  protected AdvancementsManager() {}

  @SubscribeEvent
  public static void handleWorldEventLoad(WorldEvent.Load event) {
    // Ignore server side worlds.
    if (!event.getWorld().isClientSide()) {
      return;
    }

    reset();
  }

  public static void reset() {
    log.debug("Reset Advancements Manager ...");
    advancementProgressMap = new HashMap<>();
    advancementsIndex = new HashSet<>();
    advancementsMap = new HashMap<>();
    hasAdvancements = false;
    rootAdvancements = new HashSet<>();
    selectedAdvancement = null;
    selectedRootAdvancement = null;
  }

  public static void addAdvancementRoot(Advancement advancement) {
    String advancementId = advancement.getId().toString();
    if (hasAdvancement(advancementId)) {
      return;
    }
    AdvancementProgress advancementProgress = getAdvancementProgress(advancement);
    AdvancementEntry advancementEntry = new AdvancementEntry(advancement, advancementProgress);
    rootAdvancements.add(advancementEntry);
    advancementsIndex.add(advancementId);
    log.debug("Added Root Advancement: {}", advancementEntry);
  }

  public static void addAdvancementTask(Advancement advancement) {
    String advancementId = advancement.getId().toString();
    Advancement rootAdvancement = advancement.getParent();

    // Try to add root advancement, if this is a child advancement.
    if (rootAdvancement != null) {
      while (rootAdvancement.getParent() != null) {
        rootAdvancement = rootAdvancement.getParent();
      }
      addAdvancementRoot(rootAdvancement);
    }

    // Skip rest, if the advancement is already known.
    if (hasAdvancement(advancementId)) {
      return;
    }

    // Get advancements stats and store the advancement data.
    AdvancementProgress advancementProgress = getAdvancementProgress(advancement);
    AdvancementEntry advancementEntry = new AdvancementEntry(advancement, advancementProgress);
    Set<AdvancementEntry> childAdvancements = advancementsMap.computeIfAbsent(advancementEntry.rootId,
        key -> new HashSet<>());
    childAdvancements.add(advancementEntry);
    advancementsIndex.add(advancementId);
    if (!hasAdvancements) {
      hasAdvancements = true;
    }
    log.debug("Added Advancement Task: {}", advancementEntry);
    TrackedAdvancementsManager.checkForTrackedAdvancement(advancementEntry);
  }

  public static boolean hasAdvancement(Advancement advancement) {
    return hasAdvancement(advancement.getId().toString());
  }

  public static boolean hasAdvancement(String advancementId) {
    return advancementsIndex.contains(advancementId);
  }

  public static boolean hasRootAdvancement(Advancement advancement) {
    for (AdvancementEntry rootAdvancement : rootAdvancements) {
      if (advancement.getId() == rootAdvancement.getId()) {
        return true;
      }
    }
    return false;
  }

  public static AdvancementEntry getRootAdvancement(Advancement advancement) {
    for (AdvancementEntry rootAdvancement : rootAdvancements) {
      if (advancement.getId() == rootAdvancement.getId()) {
        return rootAdvancement;
      }
    }
    return null;
  }

  public static Set<AdvancementEntry> getRootAdvancements() {
    return rootAdvancements;
  }

  public static Set<AdvancementEntry> getSortedRootAdvancements(
      Comparator<AdvancementEntry> comparator) {
    Set<AdvancementEntry> advancements = getRootAdvancements();
    if (advancements == null) {
      return new HashSet<>();
    }
    return advancements.stream()
        .sorted(comparator)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public static Set<AdvancementEntry> getRootAdvancementsByTile() {
    return getSortedRootAdvancements(AdvancementEntry.sortByTitle());
  }

  public static int getNumberOfRootAdvancements() {
    return rootAdvancements.size();
  }

  public static int getNumberOfAdvancements(AdvancementEntry rootAdvancement) {
    Set<AdvancementEntry> advancements = getAdvancements(rootAdvancement);
    return advancements.size();
  }

  public static int getNumberOfCompletedAdvancements(AdvancementEntry rootAdvancement) {
    int completedAdvancements = 0;
    Set<AdvancementEntry> advancements = getAdvancements(rootAdvancement);
    for (AdvancementEntry advancementEntry : advancements) {
      if (advancementEntry.getProgress().isDone()) {
        completedAdvancements++;
      }
    }
    return completedAdvancements;
  }

  public static AdvancementEntry getAdvancement(Advancement advancement) {
    return getAdvancement(advancement.getId().toString());
  }

  public static AdvancementEntry getAdvancement(String id) {
    for (Set<AdvancementEntry> advancementEntries : advancementsMap.values()) {
      for (AdvancementEntry advancementEntry : advancementEntries) {
        if (id.equals(advancementEntry.getIdString())) {
          return advancementEntry;
        }
      }
    }
    return null;
  }

  public static Set<AdvancementEntry> getAdvancements(AdvancementEntry rootAdvancement) {
    if (rootAdvancement == null) {
      log.error("Unable to get advancements for root advancement null");
      return new HashSet<>();
    }
    Set<AdvancementEntry> advancements = advancementsMap.get(rootAdvancement.getId());
    if (advancements == null) {
      return new HashSet<>();
    }
    return advancements;
  }

  public static Set<AdvancementEntry> getSortedAdvancements(
      AdvancementEntry rootAdvancement, Comparator<AdvancementEntry> comparator) {
    Set<AdvancementEntry> advancements = getAdvancements(rootAdvancement);
    return advancements.isEmpty()
        ? advancements
        : advancements.stream()
            .sorted(comparator)
            .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public static Set<AdvancementEntry> getAdvancementsByTile(AdvancementEntry rootAdvancement) {
    return getSortedAdvancements(rootAdvancement, AdvancementEntry.sortByTitle());
  }

  public static Set<AdvancementEntry> getAdvancementsByStatus(AdvancementEntry rootAdvancement) {
    return getSortedAdvancements(rootAdvancement, AdvancementEntry.sortByStatus());
  }

  public static void updateAdvancementProgress(
      Advancement advancement, AdvancementProgress advancementProgress) {
    advancementProgressMap.put(advancement, advancementProgress);
    AdvancementEntry advancementEntry = getAdvancement(advancement);
    if (advancementEntry == null) {
      advancementEntry = getRootAdvancement(advancement);
      if (advancementEntry == null) {
        log.error(
            "Unable to find entry for advancement {} with progress {}",
            advancement,
            advancementProgress);
        return;
      }
    }
    advancementEntry.updateAdvancementProgress(advancementProgress);
    if (advancementProgress.isDone()) {
      TrackedAdvancementsManager.untrackAdvancement(advancement);
    }
  }

  public static AdvancementProgress getAdvancementProgress(Advancement advancement) {
    return advancementProgressMap.get(advancement);
  }

  public static AdvancementEntry getSelectedAdvancement() {
    if (selectedAdvancement == null && getSelectedRootAdvancement() != null) {
      Set<AdvancementEntry> possibleAdvancements = getAdvancements(getSelectedRootAdvancement());
      if (!possibleAdvancements.isEmpty() && possibleAdvancements.iterator().hasNext()) {
        selectedAdvancement = possibleAdvancements.iterator().next();
      }
    }
    return selectedAdvancement;
  }

  public static void setSelectedAdvancement(AdvancementEntry selectedAdvancement) {
    AdvancementsManager.selectedAdvancement = selectedAdvancement;
  }

  public static AdvancementEntry getSelectedRootAdvancement() {
    if (selectedRootAdvancement == null
        && rootAdvancements != null
        && rootAdvancements.iterator().hasNext()) {
      AdvancementEntry possibleRootAdvancement = rootAdvancements.iterator().next();
      if (possibleRootAdvancement != selectedRootAdvancement) {
        log.debug("Select root advancement: {}", selectedAdvancement);
        selectedRootAdvancement = possibleRootAdvancement;
        selectedAdvancement = null;
      }
    }
    return selectedRootAdvancement;
  }

  public static void setSelectedRootAdvancement(AdvancementEntry selectedRootAdvancement) {
    AdvancementsManager.selectedRootAdvancement = selectedRootAdvancement;
    if (selectedAdvancement != null
        && selectedRootAdvancement.getId() != selectedAdvancement.rootId) {
      selectedAdvancement = null;
    }
  }

  public static boolean hasAdvancements() {
    return hasAdvancements;
  }
}
