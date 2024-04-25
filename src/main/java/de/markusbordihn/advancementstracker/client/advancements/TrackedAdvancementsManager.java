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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.advancements.AdvancementHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.resources.ResourceLocation;

import de.markusbordihn.advancementstracker.Constants;
import de.markusbordihn.advancementstracker.client.gui.widget.AdvancementsTrackerWidget;
import de.markusbordihn.advancementstracker.config.ClientConfig;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class TrackedAdvancementsManager {

  private static Map<ResourceLocation, AdvancementEntry> trackedAdvancements = new HashMap<>();
  private static List<String> trackedAdvancementsDefault = new ArrayList<>();
  private static List<String> trackedAdvancementsLocal = new ArrayList<>();
  private static List<String> trackedAdvancementsRemote = new ArrayList<>();
  private static String serverId;

  protected TrackedAdvancementsManager() {}

  @SubscribeEvent
  public static void handleLevelEventLoad(LevelEvent.Load event) {
    // Ignore server side worlds.
    if (!event.getLevel().isClientSide()) {
      return;
    }

    Minecraft minecraft = Minecraft.getInstance();
    ServerData serverData = minecraft != null ? minecraft.getCurrentServer() : null;
    if (serverData != null) {
      serverId =
          String.format("%s:%s::", serverData.name.replaceAll("\\W", "_"), serverData.protocol);
    } else {
      serverId = null;
    }
    trackedAdvancements = new HashMap<>();
    AdvancementsTracker.log.info("Preparing tracked advancements ...");

    // Loading default (over config file) tracked advancements.
    trackedAdvancementsDefault = ClientConfig.CLIENT.trackedAdvancements.get();
    if (!trackedAdvancementsDefault.isEmpty()) {
      AdvancementsTracker.log.info("Loading default (config) tracked advancements: {}", trackedAdvancementsDefault);
    }

    // Loading local (user) tracked advancements.
    trackedAdvancementsLocal = ClientConfig.CLIENT.trackedAdvancementsLocal.get();
    if (!trackedAdvancementsLocal.isEmpty()) {
      AdvancementsTracker.log.info("Loading local (user) tracked advancements: {}", trackedAdvancementsLocal);
    }

    // Loading remote tracked advancements, if we are connected to a server.
    if (serverId != null) {
      trackedAdvancementsRemote = ClientConfig.CLIENT.trackedAdvancementsRemote.get();
      if (!trackedAdvancementsRemote.isEmpty()) {
        AdvancementsTracker.log.info("Loading remote ({}) tracked advancements: {} ...", serverId,
            trackedAdvancementsRemote);
      }
    }
    updateTrackerWidget();
  }

  public static void checkForTrackedAdvancement(AdvancementEntry advancement) {
    // Ignore advancements which are done.
    if (advancement.getProgress().isDone()) {
      return;
    }
    AdvancementEntry trackedAdvancement = null;

    // Check first for default tracked advancement
    if (!trackedAdvancementsDefault.isEmpty()) {
      for (String trackedAdvancementDefault : trackedAdvancementsDefault) {
        if (advancement.getIdString().equals(trackedAdvancementDefault)) {
          AdvancementsTracker.log.debug("Adding default tracked advancement {}", advancement);
          trackedAdvancement = advancement;
          break;
        }
      }
    }

    // Check for remote tracked advancement
    if (!trackedAdvancementsRemote.isEmpty() && serverId != null) {
      for (String cachedAdvancementEntry : trackedAdvancementsRemote) {
        if (!cachedAdvancementEntry.isEmpty() && !"".equals(cachedAdvancementEntry)
            && cachedAdvancementEntry.startsWith(serverId)
            && advancement.getIdString().equals(cachedAdvancementEntry.split("::", 2)[1])) {
          AdvancementsTracker.log.debug("Adding remote tracked advancement {}", advancement);
          trackedAdvancement = advancement;
          break;
        }
      }
    }

    // Check for local tracked advancement
    if (!trackedAdvancementsLocal.isEmpty() && serverId == null) {
      for (String cachedAdvancementEntry : trackedAdvancementsLocal) {
        if (advancement.getIdString().equals(cachedAdvancementEntry)) {
          AdvancementsTracker.log.debug("Adding local tracked advancement {}", advancement);
          trackedAdvancement = advancement;
          break;
        }
      }
    }

    if (trackedAdvancement != null) {
      trackAdvancement(trackedAdvancement, false);
    }

  }

  public static void toggleTrackedAdvancement(AdvancementEntry advancement) {
    if (advancement.getProgress().isDone()) {
      return;
    }
    if (advancement.isTracked()) {
      untrackAdvancement(advancement);
    } else {
      trackAdvancement(advancement);
    }
  }

  public static void trackAdvancement(AdvancementEntry advancement) {
    trackAdvancement(advancement, true);
  }

  public static void trackAdvancement(AdvancementEntry advancement, boolean autosave) {
    if (advancement.getProgress().isDone()) {
      AdvancementsTracker.log.warn("Advancement {} is already done, no need to track it.", advancement);
      return;
    }
    if (trackedAdvancements.containsKey(advancement.getId())) {
      AdvancementsTracker.log.warn("Advancement {} is already tracked.", advancement);
      return;
    }
    AdvancementsTracker.log.info("Track Advancement {}", advancement);
    trackedAdvancements.put(advancement.getId(), advancement);
    if (autosave) {
      saveTrackedAdvancements();
    }
    updateTrackerWidget();
  }

  private static void saveTrackedAdvancements() {
    saveTrackedAdvancementsRemote();
    saveTrackedAdvancementsLocal();
  }

  private static void saveTrackedAdvancementsRemote() {
    if (serverId == null) {
      return;
    }
    List<String> trackedAdvancementsToSave = new ArrayList<>();
    // Adding existing entries, but ignore entries for current server.
    for (String trackedAdvancementRemote : trackedAdvancementsRemote) {
      if (!trackedAdvancementRemote.isEmpty() && !"".equals(trackedAdvancementRemote)
          && !trackedAdvancementRemote.startsWith(serverId)) {
        trackedAdvancementsToSave.add(trackedAdvancementRemote);
      }
    }
    // Adding entries for current server.
    for (ResourceLocation trackedAdvancementEntry : trackedAdvancements.keySet()) {
      trackedAdvancementsToSave.add(serverId + trackedAdvancementEntry);
    }
    ClientConfig.CLIENT.trackedAdvancementsRemote
        .set(trackedAdvancementsToSave.stream().distinct().collect(Collectors.toList()));
    trackedAdvancementsRemote = ClientConfig.CLIENT.trackedAdvancementsRemote.get();
    ClientConfig.CLIENT.trackedAdvancements.save();
  }

  private static void saveTrackedAdvancementsLocal() {
    if (serverId != null) {
      return;
    }
    List<String> trackedAdvancementsToSave = new ArrayList<>();
    for (ResourceLocation trackedAdvancementEntry : trackedAdvancements.keySet()) {
      trackedAdvancementsToSave.add(trackedAdvancementEntry.toString());
    }
    ClientConfig.CLIENT.trackedAdvancementsLocal
        .set(trackedAdvancementsToSave.stream().distinct().collect(Collectors.toList()));
    trackedAdvancementsLocal = ClientConfig.CLIENT.trackedAdvancementsLocal.get();
    ClientConfig.CLIENT.trackedAdvancements.save();
  }

  public static void untrackAdvancement(AdvancementHolder advancementHolder) {
    untrackAdvancement(advancementHolder.id());
  }

  public static void untrackAdvancement(AdvancementEntry advancement) {
    untrackAdvancement(advancement.getId());
  }

  public static void untrackAdvancement(ResourceLocation advancementId) {
    if (trackedAdvancements.remove(advancementId) != null) {
      saveTrackedAdvancements();
      updateTrackerWidget();
    }
  }

  public static int numOfTrackedAdvancements() {
    return trackedAdvancements.size();
  }

  public static boolean hasTrackedAdvancement(AdvancementEntry advancementEntry) {
    ResourceLocation rootAdvancementId = advancementEntry.getId();
    for (AdvancementEntry trackedAdvancementEntry : trackedAdvancements.values()) {
      if (rootAdvancementId.equals(trackedAdvancementEntry.rootId)) {
        return true;
      }
    }
    return false;
  }

  public static boolean hasTrackedAdvancements() {
    return !trackedAdvancements.isEmpty();
  }

  public static boolean isTrackedAdvancement(ResourceLocation advancement) {
    return trackedAdvancements.containsKey(advancement);
  }

  public static Set<AdvancementEntry> getTrackedAdvancements() {
    return new HashSet<>(trackedAdvancements.values());
  }

  private static void updateTrackerWidget() {
    AdvancementsTrackerWidget.updateTrackedAdvancements();
  }

}
