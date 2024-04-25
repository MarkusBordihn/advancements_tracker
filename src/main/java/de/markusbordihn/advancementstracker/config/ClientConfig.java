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

package de.markusbordihn.advancementstracker.config;

import de.markusbordihn.advancementstracker.AdvancementsTracker;
import de.markusbordihn.advancementstracker.Constants;
import de.markusbordihn.advancementstracker.client.gui.widget.AdvancementsTrackerWidget;
import de.markusbordihn.advancementstracker.utils.gui.PositionManager.BasePosition;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientConfig {

  public static final Config CLIENT;
  static final ModConfigSpec clientSpec;

  static {
    com.electronwill.nightconfig.core.Config.setInsertionOrderPreserved(true);
    final Pair<Config, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Config::new);
    clientSpec = specPair.getRight();
    CLIENT = specPair.getLeft();
    AdvancementsTracker.log.info("{} Client config ...", Constants.LOG_REGISTER_PREFIX);
    ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, clientSpec);
  }

  private ClientConfig() {}

  @SubscribeEvent
  public static void onConfigReloading(final ModConfigEvent.Reloading configEvent) {
    if (configEvent.getConfig().getSpec() == ClientConfig.clientSpec) {
      AdvancementsTrackerWidget.reloadConfig();
    }
  }

  @SubscribeEvent
  public static void onConfigLoading(final ModConfigEvent.Loading configEvent) {
    if (configEvent.getConfig().getSpec() == ClientConfig.clientSpec) {
      AdvancementsTrackerWidget.reloadConfig();
    }
  }

  public static class Config {

    public final ModConfigSpec.BooleanValue overviewEnabled;

    public final ModConfigSpec.BooleanValue widgetEnabled;
    public final ModConfigSpec.BooleanValue widgetVisible;
    public final ModConfigSpec.EnumValue<BasePosition> widgetPosition;
    public final ModConfigSpec.IntValue widgetHeight;
    public final ModConfigSpec.IntValue widgetWidth;
    public final ModConfigSpec.IntValue widgetTop;
    public final ModConfigSpec.IntValue widgetLeft;

    public final ModConfigSpec.ConfigValue<String> logLevel;

    public final ModConfigSpec.ConfigValue<List<String>> trackedAdvancements;
    public final ModConfigSpec.ConfigValue<List<String>> trackedAdvancementsRemote;
    public final ModConfigSpec.ConfigValue<List<String>> trackedAdvancementsLocal;

    Config(ModConfigSpec.Builder builder) {
      builder.comment("Advancements Tracker (Client configuration)");

      builder.push("general");
      trackedAdvancements =
          builder
              .comment("List of default tracked advancements, mostly used by mod packs.")
              .define("trackedAdvancements", new ArrayList<>(List.of("")));
      builder.pop();

      builder.push("Advancements Tracker: Overview");
      overviewEnabled =
          builder
              .comment("Enable/Disable the advancements overview screen.")
              .define("overviewEnabled", true);
      builder.pop();

      builder.push("Advancements Tracker: Widget");
      widgetEnabled =
          builder
              .comment("Enable/Disable the advancements tracker widget.")
              .define("widgetEnabled", true);
      widgetVisible =
          builder
              .comment(
                  "Shows the widget automatically. If this is set to false the widget will be only visible after pressing the defined hot-keys.")
              .define("widgetVisible", true);
      widgetPosition =
          builder
              .comment("Defines the base position of the widget, default is MIDDLE_RIGHT")
              .defineEnum("widgetPosition", BasePosition.MIDDLE_RIGHT);
      widgetHeight =
          builder
              .comment(
                  "Defines the max. height of the widget. Default is 0 which mean use the max. available height.")
              .defineInRange("widgetHeight", 0, 0, 600);
      widgetWidth =
          builder
              .comment("Defines the max.width of the widget.")
              .defineInRange("widgetWidth", 135, 120, 600);
      widgetTop =
          builder
              .comment("Defines the top position relative to the widget position.")
              .defineInRange("widgetTop", 0, -400, 400);
      widgetLeft =
          builder
              .comment("Defines the left position relative to the widget position.")
              .defineInRange("widgetLeft", 0, -400, 400);
      builder.pop();

      builder.push("Debug");
      logLevel =
          builder
              .comment("Changed the default log level to get more output.")
              .define("logLevel", "info");
      builder.pop();

      builder.push("cache");
      trackedAdvancementsRemote =
          builder.define("trackedAdvancementsRemote", new ArrayList<>(List.of("")));
      trackedAdvancementsLocal =
          builder.define("trackedAdvancementsLocal", new ArrayList<>(List.of("")));
      builder.pop();
    }
  }
}
