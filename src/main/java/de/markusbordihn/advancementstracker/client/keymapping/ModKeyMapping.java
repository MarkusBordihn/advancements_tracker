/**
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

package de.markusbordihn.advancementstracker.client.keymapping;

import de.markusbordihn.advancementstracker.AdvancementsTracker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;


import de.markusbordihn.advancementstracker.Constants;
import de.markusbordihn.advancementstracker.client.gui.screens.AdvancementsTrackerScreen;
import de.markusbordihn.advancementstracker.client.gui.widget.AdvancementsTrackerWidget;
import de.markusbordihn.advancementstracker.config.ClientConfig;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class ModKeyMapping {

  protected ModKeyMapping() {}

  public static final KeyMapping KEY_SHOW_WIDGET = new KeyMapping(
      Constants.KEY_PREFIX + "show_widget", KeyConflictContext.IN_GAME, KeyModifier.ALT,
      InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_L), Constants.KEY_PREFIX + "category");

  public static final KeyMapping KEY_SHOW_OVERVIEW = new KeyMapping(
      Constants.KEY_PREFIX + "show_overview", KeyConflictContext.IN_GAME, KeyModifier.CONTROL,
      InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_L), Constants.KEY_PREFIX + "category");

  @SubscribeEvent
  public static void handleKeyboardKeyPressedEvent(InputEvent.Key event) {
    if (ModKeyMapping.KEY_SHOW_WIDGET.isDown() && Boolean.TRUE.equals(ClientConfig.CLIENT.widgetEnabled.get())) {
      AdvancementsTracker.log.debug("Show/hide Advancements Widget ...");
      AdvancementsTrackerWidget.toggleVisibility();
    } else if (ModKeyMapping.KEY_SHOW_OVERVIEW.isDown()
        && Boolean.TRUE.equals(ClientConfig.CLIENT.overviewEnabled.get())) {
      AdvancementsTracker.log.debug("Show/hide Advancements Overview ...");
      AdvancementsTrackerScreen.toggleVisibility();
    }
  }

  public static void registerKeyMapping(RegisterKeyMappingsEvent event) {
    AdvancementsTracker.log.info("{} Key Mapping ...", Constants.LOG_REGISTER_PREFIX);

    event.register(ModKeyMapping.KEY_SHOW_WIDGET);
    event.register(ModKeyMapping.KEY_SHOW_OVERVIEW);
  }
}
