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
import de.markusbordihn.advancementstracker.client.gui.screens.AdvancementsTrackerScreen;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(Dist.CLIENT)
public class ClientAdvancementManager implements ClientAdvancements.Listener {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private static final short ADD_LISTENER_TICK = 2;

  private static ClientAdvancementManager clientAdvancementManager;
  private static ClientAdvancements clientAdvancements;
  private static boolean hasListener = false;
  private static boolean needsReload = false;
  private static int listenerTicks = 0;

  protected ClientAdvancementManager() {}

  @SubscribeEvent
  public static void handleWorldEventLoad(WorldEvent.Load event) {
    // Ignore server side worlds.
    if (!event.getWorld().isClientSide()) {
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
        if (!needsReload
            && !(screen instanceof AdvancementsTrackerScreen)
            && (screen instanceof ClientAdvancements.Listener)) {
          log.debug("Need to reload advancements after screen {} is closed!", minecraft.screen);
          needsReload = true;
        }
      } else if (needsReload) {
        reset();
      }
    }
  }

  public static void reset() {
    log.debug("Resetting Client Advancement Manager ...");
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
    if (minecraft == null
        || minecraft.player == null
        || minecraft.player.connection == null
        || minecraft.player.connection.getAdvancements() == null
        || minecraft
            .player
            .connection
            .getAdvancements()
            .getAdvancements()
            .getAllAdvancements()
            .isEmpty()) {
      return;
    }
    log.debug("Adding client advancement manager listener...");
    clientAdvancements = minecraft.player.connection.getAdvancements();
    minecraft.player.connection.getAdvancements().setListener(clientAdvancementManager);
    hasListener = true;
  }

  public static boolean isValidAdvancement(Advancement advancement) {
    String advancementId = advancement.getId().toString();
    if (advancementId.startsWith("minecraft:recipes/")
        || advancementId.startsWith("smallships:recipes")) {
      return false;
    } else if (advancement.getDisplay() == null) {
      log.debug("[Skip Advancement with no display information] {}", advancement);
      return false;
    }
    return true;
  }

  @Override
  public void onUpdateAdvancementProgress(
      Advancement advancement, AdvancementProgress advancementProgress) {
    if (isValidAdvancement(advancement)) {
      log.debug("[Update Advancement Progress] {} with {}", advancement, advancementProgress);
      AdvancementsManager.updateAdvancementProgress(advancement, advancementProgress);
    }
  }

  @Override
  public void onAddAdvancementRoot(Advancement advancement) {
    if (isValidAdvancement(advancement) && advancement.getParent() == null) {
      log.debug("[Add Advancement Root] {}", advancement);
      AdvancementsManager.addAdvancementRoot(advancement);
    }
  }

  @Override
  public void onRemoveAdvancementRoot(Advancement advancement) {
    // Not used.
    log.debug("[Remove Advancement Root] {}", advancement);
  }

  @Override
  public void onAddAdvancementTask(Advancement advancement) {
    if (isValidAdvancement(advancement) && advancement.getParent() != null) {
      log.debug("[Add Advancement Task] {}", advancement);
      AdvancementsManager.addAdvancementTask(advancement);
    }
  }

  @Override
  public void onRemoveAdvancementTask(Advancement advancement) {
    log.debug("[Remove Advancement Task] {}", advancement);
  }

  @Override
  public void onAdvancementsCleared() {
    // Not used.
    log.debug("[Advancements Cleared] ...");
  }

  @Override
  public void onSelectedTabChanged(Advancement advancement) {
    // Not used.
    log.debug("[Selected Tab Changed] {}", advancement);
  }
}
