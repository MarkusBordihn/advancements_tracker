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

import de.markusbordihn.advancementstracker.AdvancementsTracker;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;

import de.markusbordihn.advancementstracker.Constants;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class AdvancementsManager {

  private static AdvancementEntry selectedAdvancement;
  private static AdvancementEntry selectedRootAdvancement;
  private static Map<Advancement, AdvancementProgress> advancementProgressMap = new HashMap<>();
  private static Map<ResourceLocation, Set<AdvancementEntry>> advancementsMap = new HashMap<>();
  private static Map<ResourceLocation, AdvancementEntry> rootAdvancements = new HashMap<>();
  private static Set<ResourceLocation> advancementsIndex = new HashSet<>();
  private static boolean hasAdvancements = false;

  protected AdvancementsManager() {}

  @SubscribeEvent
  public static void handleLevelEventLoad(LevelEvent.Load event) {
    // Ignore server side worlds.
    if (!event.getLevel().isClientSide()) {
      return;
    }
    reset();
  }

  public static void reset() {
    AdvancementsTracker.log.debug("Reset Advancements Manager ...");
    advancementProgressMap = new HashMap<>();
    advancementsIndex = new HashSet<>();
    advancementsMap = new HashMap<>();
    hasAdvancements = false;
    rootAdvancements = new HashMap<>();
    selectedAdvancement = null;
    selectedRootAdvancement = null;
  }

  public static void addAdvancementRoot(AdvancementHolder advancementHolder) {
    ResourceLocation advancementId = advancementHolder.id();
    if (hasAdvancement(advancementId)) {
      return;
    }
    AdvancementProgress advancementProgress = getAdvancementProgress(advancementHolder.value());
    AdvancementEntry advancementEntry = new AdvancementEntry(advancementHolder, advancementProgress);
    rootAdvancements.put(advancementId, advancementEntry);
    advancementsIndex.add(advancementId);
    AdvancementsTracker.log.debug("Added Root Advancement: {}", advancementEntry);
  }

  public static void addAdvancementTask(AdvancementHolder advancementHolder, @Nullable AdvancementNode parent) {
    ResourceLocation advancementId = advancementHolder.id();
    int rootLevel = 0;
    // Try to add root advancement, if this is a child advancement.
    while (parent != null && parent.parent() != null) {
      parent = parent.parent();
      rootLevel++;
    }
    if (parent != null) {
      addAdvancementRoot(parent.holder());
    }

    // Skip rest, if the advancement is already known.
    if (hasAdvancement(advancementId)) {
      return;
    }

    // Get advancements stats and store the advancement data.
    AdvancementProgress advancementProgress = getAdvancementProgress(advancementHolder.value());
    AdvancementEntry advancementEntry = new AdvancementEntry(advancementHolder, advancementProgress, parent, rootLevel);
    advancementsMap.computeIfAbsent(advancementEntry.rootId, k -> new HashSet<>())
          .add(advancementEntry);
    advancementsIndex.add(advancementId);
    if (!hasAdvancements) {
      hasAdvancements = true;
    }
    AdvancementsTracker.log.debug("Added Advancement Task: {}", advancementEntry);
    TrackedAdvancementsManager.checkForTrackedAdvancement(advancementEntry);
  }

  public static boolean hasAdvancement(AdvancementHolder advancementHolder) {
    return hasAdvancement(advancementHolder.id());
  }

  public static boolean hasAdvancement(ResourceLocation advancementId) {
    return advancementsIndex.contains(advancementId);
  }

  public static boolean hasRootAdvancement(AdvancementHolder advancementHolder) {
    return rootAdvancements.containsKey(advancementHolder.id());
  }

  public static AdvancementEntry getRootAdvancement(AdvancementHolder advancementHolder) {
    return rootAdvancements.get(advancementHolder.id());
  }

  public static Set<AdvancementEntry> getRootAdvancements() {
    return new HashSet<>(rootAdvancements.values());
  }

  public static Set<AdvancementEntry> getSortedRootAdvancements(Comparator<AdvancementEntry> comparator) {
    return rootAdvancements.values().stream().sorted(comparator)
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

  public static AdvancementEntry getAdvancement(AdvancementHolder advancementHolder) {
    return getAdvancement(advancementHolder.id());
  }

  public static AdvancementEntry getAdvancement(ResourceLocation id) {
    for (Set<AdvancementEntry> advancementEntries : advancementsMap.values()) {
      for (AdvancementEntry advancementEntry : advancementEntries) {
        if (id.equals(advancementEntry.getId())) {
          return advancementEntry;
        }
      }
    }
    return null;
  }

  public static Set<AdvancementEntry> getAdvancements(AdvancementEntry rootAdvancement) {
    if (rootAdvancement == null) {
      AdvancementsTracker.log.error("Unable to get advancements for root advancement {}", rootAdvancement);
      return new HashSet<>();
    }
    Set<AdvancementEntry> advancements = advancementsMap.get(rootAdvancement.getId());
    if (advancements == null) {
      return new HashSet<>();
    }
    return advancements;
  }

  public static Set<AdvancementEntry> getSortedAdvancements(AdvancementEntry rootAdvancement,
      Comparator<AdvancementEntry> comparator) {
    Set<AdvancementEntry> advancements = getAdvancements(rootAdvancement);
    return advancements.isEmpty() ? advancements
        : advancements.stream().sorted(comparator)
            .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public static Set<AdvancementEntry> getAdvancementsByTile(AdvancementEntry rootAdvancement) {
    return getSortedAdvancements(rootAdvancement, AdvancementEntry.sortByTitle());
  }

  public static Set<AdvancementEntry> getAdvancementsByStatus(AdvancementEntry rootAdvancement) {
    return getSortedAdvancements(rootAdvancement, AdvancementEntry.sortByStatus());
  }

  public static void updateAdvancementProgress(AdvancementHolder advancementHolder,
      AdvancementProgress advancementProgress) {
    advancementProgressMap.put(advancementHolder.value(), advancementProgress);
    AdvancementEntry advancementEntry = getAdvancement(advancementHolder);
    if (advancementEntry == null) {
      advancementEntry = getRootAdvancement(advancementHolder);
      if (advancementEntry == null) {
        AdvancementsTracker.log.error("Unable to find entry for advancement {} with progress {}", advancementHolder.id(), advancementProgress);
        return;
      }
    }
    advancementEntry.updateAdvancementProgress(advancementProgress);
    if (advancementProgress.isDone()) {
      TrackedAdvancementsManager.untrackAdvancement(advancementHolder);
    }
  }

  public static AdvancementProgress getAdvancementProgress(Advancement advancement) {
    return advancementProgressMap.get(advancement);
  }

  public static AdvancementEntry getSelectedAdvancement() {
    if (selectedAdvancement == null) {
      AdvancementEntry selectedRoot = getSelectedRootAdvancement();
      if (selectedRoot != null) {
        Set<AdvancementEntry> possibleAdvancements = getAdvancements(selectedRoot);
        if (!possibleAdvancements.isEmpty() && possibleAdvancements.iterator().hasNext()) {
          selectedAdvancement = possibleAdvancements.iterator().next();
        }
      }
    }
    return selectedAdvancement;
  }

  public static void setSelectedAdvancement(AdvancementEntry selectedAdvancement) {
    AdvancementsManager.selectedAdvancement = selectedAdvancement;
  }

  public static AdvancementEntry getSelectedRootAdvancement() {
    if (selectedRootAdvancement == null && rootAdvancements != null) {
      //Just use a find any as it is an unsorted map
      Optional<AdvancementEntry> possibleRootAdvancement = rootAdvancements.values().stream().findAny();
      if (possibleRootAdvancement.isPresent()) {
        AdvancementsTracker.log.debug("Select root advancement: {}", selectedAdvancement);
        selectedRootAdvancement = possibleRootAdvancement.get();
        selectedAdvancement = null;
      }
    }
    return selectedRootAdvancement;
  }

  public static void setSelectedRootAdvancement(AdvancementEntry selectedRootAdvancement) {
    AdvancementsManager.selectedRootAdvancement = selectedRootAdvancement;
    if (!selectedRootAdvancement.equals(selectedAdvancement)) {
      selectedAdvancement = null;
    }
  }

  public static boolean hasAdvancements() {
    return hasAdvancements;
  }

}
