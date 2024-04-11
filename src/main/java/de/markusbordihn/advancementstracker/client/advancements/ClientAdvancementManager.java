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
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;

import de.markusbordihn.advancementstracker.Constants;
import de.markusbordihn.advancementstracker.client.gui.screens.AdvancementsTrackerScreen;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class ClientAdvancementManager implements ClientAdvancements.Listener {

  private static final short ADD_LISTENER_TICK = 2;

  private static ClientAdvancementManager clientAdvancementManager;
  private static ClientAdvancements clientAdvancements;
  private static boolean hasListener = false;
  private static boolean needsReload = false;
  private static int listenerTicks = 0;

  protected ClientAdvancementManager() {}

  @SubscribeEvent
  public static void handleLevelEventLoad(LevelEvent.Load event) {
    // Ignore server side worlds.
    if (!event.getLevel().isClientSide()) {
      return;
    }
    reset();
  }

  @SubscribeEvent
  public static void handleClientTickEvent(TickEvent.ClientTickEvent event) {
    if (event.phase == TickEvent.Phase.END) {
      listenerTicks++;
      return;
    }

    if (listenerTicks >= ADD_LISTENER_TICK && !hasListener) {
      addListener();
      listenerTicks = 0;
    }

    // Other advancements screen will remove the event listener, for this reason we need to check
    // if we need to reload the advancements after such advancements screen was open.
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft != null) {
      if (minecraft.screen != null) {
        Screen screen = minecraft.screen;
        if (!needsReload && !(screen instanceof AdvancementsTrackerScreen)
            && (screen instanceof AdvancementsScreen
                || screen instanceof ClientAdvancements.Listener)) {
          AdvancementsTracker.log.debug("Need to reload advancements after screen {} is closed!", minecraft.screen);
          needsReload = true;
        }
      } else if (needsReload) {
        reset();
      }
    }
  }

  public static void reset() {
    AdvancementsTracker.log.debug("Resetting Client Advancement Manager ...");
    clientAdvancementManager = new ClientAdvancementManager();
    clientAdvancements = null;
    hasListener = false;
    listenerTicks = 0;
    needsReload = false;
  }

  public static void addListener() {
    if (clientAdvancements != null) {
      return;
    }
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft == null || minecraft.player == null || minecraft.player.connection == null
        || minecraft.player.connection.getAdvancements() == null || minecraft.player.connection
            .getAdvancements().getTree().nodes().isEmpty()) {
      return;
    }
    AdvancementsTracker.log.debug("Adding client advancement manager listener...");
    clientAdvancements = minecraft.player.connection.getAdvancements();
    minecraft.player.connection.getAdvancements().setListener(clientAdvancementManager);
    hasListener = true;
  }

  public static boolean isValidAdvancement(AdvancementHolder advancementHolder) {
    String advancementId = advancementHolder.id().toString();
    if (advancementId.startsWith("minecraft:recipes/")
        || advancementId.startsWith("smallships:recipes")) {
      return false;
    } else if (advancementHolder.value().display().isEmpty()) {
      AdvancementsTracker.log.debug("[Skip Advancement with no display information] {}", advancementId);
      return false;
    }
    return true;
  }

  @Override
  public void onUpdateAdvancementProgress(AdvancementNode advancementNode,
      AdvancementProgress advancementProgress) {
    AdvancementHolder advancementHolder = advancementNode.holder();
    if (isValidAdvancement(advancementHolder)) {
      AdvancementsTracker.log.debug("[Update Advancement Progress] {} with {}", advancementHolder.id(), advancementProgress);
      AdvancementsManager.updateAdvancementProgress(advancementHolder, advancementProgress);
    }
  }

  @Override
  public void onAddAdvancementRoot(AdvancementNode advancementNode) {
    AdvancementHolder advancementHolder = advancementNode.holder();
    if (isValidAdvancement(advancementHolder) && advancementNode.parent() == null) {
      AdvancementsTracker.log.debug("[Add Advancement Root] {}", advancementHolder.id());
      AdvancementsManager.addAdvancementRoot(advancementHolder);
    }
  }

  @Override
  public void onRemoveAdvancementRoot(AdvancementNode advancementNode) {
    // Not used.
    AdvancementsTracker.log.debug("[Remove Advancement Root] {}", advancementNode.holder().id());
  }

  @Override
  public void onAddAdvancementTask(AdvancementNode advancementNode) {
    AdvancementHolder advancementHolder = advancementNode.holder();
    if (isValidAdvancement(advancementHolder) && advancementNode.parent() != null) {
      AdvancementsTracker.log.debug("[Add Advancement Task] {}", advancementHolder.id());
      AdvancementsManager.addAdvancementTask(advancementHolder, advancementNode.parent());
    }
  }

  @Override
  public void onRemoveAdvancementTask(AdvancementNode advancementNode) {
    AdvancementsTracker.log.debug("[Remove Advancement Task] {}", advancementNode.holder().id());
  }

  @Override
  public void onAdvancementsCleared() {
    // Not used.
    AdvancementsTracker.log.debug("[Advancements Cleared] ...");
  }

  @Override
  public void onSelectedTabChanged(AdvancementHolder advancementHolder) {
    // Not used.
    AdvancementsTracker.log.debug("[Selected Tab Changed] {}", advancementHolder == null ? null : advancementHolder.id());
  }

}
