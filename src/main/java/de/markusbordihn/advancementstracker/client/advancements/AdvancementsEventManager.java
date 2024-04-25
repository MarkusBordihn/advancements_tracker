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
import net.minecraft.advancements.AdvancementTree;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent.AdvancementEarnEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent.AdvancementProgressEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import de.markusbordihn.advancementstracker.Constants;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class AdvancementsEventManager {

  private static int numberOfAdvancements = 0;

  protected AdvancementsEventManager() {}

  @SubscribeEvent
  public static void handleLevelEventLoad(LevelEvent.Load event) {
    // Ignore server side worlds.
    if (!event.getLevel().isClientSide()) {
      return;
    }
    reset();
  }

  @SubscribeEvent
  public static void handleAdvancementEarnEvent(AdvancementEarnEvent advancementEvent) {
    handleAdvancementEvent(advancementEvent);
  }

  @SubscribeEvent
  public static void handleAdvancementProgressEvent(AdvancementProgressEvent advancementEvent) {
    handleAdvancementEvent(advancementEvent);
  }


  private static void handleAdvancementEvent(AdvancementEvent advancementEvent) {
    //Note: These events are only fired on the logical server
    AdvancementTree advancementTree = ServerLifecycleHooks.getCurrentServer().getAdvancements().tree();

    AdvancementHolder advancementHolder = advancementEvent.getAdvancement();

    if (ClientAdvancementManager.isValidAdvancement(advancementHolder)) {
      AdvancementsTracker.log.debug("[Advancement Event] {}", advancementHolder);
      String advancementId = advancementHolder.id().toString();
      AdvancementNode rootAdvancement = advancementHolder.value().parent().map(advancementTree::get).orElse(null);
      if (rootAdvancement == null) {
        if (advancementId.contains("/root") || advancementId.contains(":root")) {
          ClientAdvancementManager.reset();
        }
        AdvancementsManager.addAdvancementRoot(advancementHolder);
      } else {
        AdvancementsManager.addAdvancementTask(advancementHolder, rootAdvancement);
      }

      // Make sure that we are covering changes which are not catch by the advancements events.
      int possibleNumberOfAdvancements = advancementTree.nodes().size();
      if (possibleNumberOfAdvancements > numberOfAdvancements) {
        AdvancementsTracker.log.debug("Force sync of advancements because it seems we are missing some {} vs. {}",
              possibleNumberOfAdvancements, numberOfAdvancements);
        ClientAdvancementManager.reset();
        numberOfAdvancements = possibleNumberOfAdvancements;
      }
    }
  }

  public static void reset() {
    AdvancementsTracker.log.debug("Resetting number of advancements ...");
    numberOfAdvancements = 0;
  }

}
