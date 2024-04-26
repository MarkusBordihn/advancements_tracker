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
import net.minecraft.advancements.Advancement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(Dist.CLIENT)
public class AdvancementsEventManager {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);
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
  public static void handleAdvancementEvent(AdvancementEvent advancementEvent) {
    Advancement advancement = advancementEvent.getAdvancement();
    if (ClientAdvancementManager.isValidAdvancement(advancement)) {
      log.debug("[Advancement Event] {}", advancement);
      String advancementId = advancement.getId().toString();
      Advancement rootAdvancement = advancement.getParent();
      if (rootAdvancement == null) {
        if (advancementId.contains("/root") || advancementId.contains(":root")) {
          ClientAdvancementManager.reset();
        }
        AdvancementsManager.addAdvancementRoot(advancement);
      } else {
        while (rootAdvancement != null && rootAdvancement.getParent() != null) {
          rootAdvancement = rootAdvancement.getParent();
        }
        AdvancementsManager.addAdvancementRoot(rootAdvancement);
        AdvancementsManager.addAdvancementTask(advancement);
      }

      // Make sure that we are covering changes which are not catch by the advancements events.
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft != null) {
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer != null
            && localPlayer.connection != null
            && localPlayer.connection.getAdvancements() != null
            && !localPlayer
                .connection
                .getAdvancements()
                .getAdvancements()
                .getAllAdvancements()
                .isEmpty()) {
          int possibleNumberOfAdvancements =
              localPlayer
                  .connection
                  .getAdvancements()
                  .getAdvancements()
                  .getAllAdvancements()
                  .size();
          if (possibleNumberOfAdvancements > numberOfAdvancements) {
            log.debug(
                "Force sync of advancements because it seems we are missing some {} vs. {}",
                possibleNumberOfAdvancements,
                numberOfAdvancements);
            ClientAdvancementManager.reset();
            numberOfAdvancements = possibleNumberOfAdvancements;
          }
        }
      }
    }
  }

  public static void reset() {
    log.debug("Resetting number of advancements ...");
    numberOfAdvancements = 0;
  }
}
