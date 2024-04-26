# Advancements Tracker and Overview (1.19.2)

[![Advancements Tracker and Overview Downloads](http://cf.way2muchnoise.eu/full_453074_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/advancements-tracker)
[![Advancements Tracker and Overview Versions](http://cf.way2muchnoise.eu/versions/Minecraft_453074_all.svg)](https://www.curseforge.com/minecraft/mc-mods/advancements-tracker)

Advancements Tracker is a Minecraft Forge Client Mod which give you an overlay which shows you the
current process of your selected advancements to easier follow them.
Additional you will get a better overview of Advancements with sorting and filter options.
This mod will make achieving the advancements more easier, interesting and meaningful.

## 🚀Features

- Client side only, but works with multiplayer servers.
- List Overview of advancements with sorting and filter.
- Displaying of completed and missing criteria to make it easier to complete an advancement.
- Easy way to track specific advancements and their process.
- Most criteria will be translated to more meaningful names.

### 📜Advancements Overview

The overview could be open with **CTRL + L** and displays all current advancements and allow the
tracking of single advancements.

![Advancement Overview][overview_example]

### 🔎 Advancement Details

By clicking on the advancements inside the overview you will see the details about the completed and
missing criteria and a list of rewards, if any.

![Advancement Details][advancement_criteria]

### 🎯Advancements Widget

The widget could be show/hide with **ALT + L** and displays the currently tracked advancements and
their progress.

![Advancement Widget][widget_example]

### 💾Storing tracked advancements

The tracked advancements are stored directly in the client settings file and could be easily
adjusted.
They are stored per server, so you can have different tracked advancements for each single server.
Local games (single-player) however sharing the same tracked advancements.

## 🌐 Criteria Translation Support

This mods adds support for criteria translation instead of having cryptic requirements like 1, 2, 3.
The translated text is added over language files and you will see a corresponding message in the
client log for untranslated advancements like:
`Unable to translate summoned (advancement.minecraft.nether.summon_wither.summoned) to a more meaningful name.`

Example **en_us.json** entry for the former line:
`"advancement.minecraft.nether.summon_wither.summoned": "Summon a Wither",`

You can add the translation directly into your mods, mod packs or in this mod over a PR.

## 🗄️Setting file

The setting file allows you to disable / enable the overview or widget screen.
You could also adjust the position and size of the widget overlay.

## 🧳Mod pack Support

You could define tracked advancements over the settings file which should be displayed by default in
a specific order.
This is helpful for mod packs so that the user gets first an tutorial advancement / tasks and later
other pre-defined tasks.
If you are defining a pre-defined order you could disable the overview screen to make sure that the
user is not un-tracking them by mistake.

## 🙋FAQ

### Is this a server side / client side mod❓

Its a pure client side mode, but will not harm if it is installed on the server.

### Does it work with data packs❓

If the data pack is not using a custom advancement system it should work out of the box.
I recommending the [Expanded Advancement and Recipe Pack][platys_advancement_and_recipe_pack] data
pack which adds a plenty of advancements and recipes.

### Do you plan to support 12.x, 13.x, 14.x, 15.x or a Fabric/Rift version❓

Unfortunately not because of my time constrains and missing knowledge about the api / engine
mechanics in these versions.

### How can I adjust the position and size of the widget❓

You can adjust the position in the config, most of the settings are updated live.
Other settings may need a client restart before showing up correctly.

### Is this mod compatible with Better Advancements❓

This mod works great together with Better Advancements mod, but let me know if you get any issues.

## ⭐Recommended additional Mods

- [Better Advancements][better_advancements] to improve the UI and UX for the advancements system in
  minecraft

## Version Status Overview 🛠️

| Version        | Status                |
|----------------|-----------------------|
| Fabric Version | ❌ Not planned         |
| Forge 1.16.5   | ⚠️ Maintenance only   |
| Forge 1.17.1   | ❌ Not planned         |
| Forge 1.18.1   | ❌ Not planned         |
| Forge 1.18.2   | ⚠️ Maintenance only   |
| Forge 1.19     | ⚠️ Deprecated         |
| Forge 1.19.1   | ⚠️ Deprecated         |
| Forge 1.19.2   | ✔️ Active development |

## License

The MIT [LICENSE.md](LICENSE.md) applies only to the code in this repository. Images, models and
other assets are explicitly excluded.

[better_advancements]: https://www.curseforge.com/minecraft/mc-mods/better-advancements

[platys_advancement_and_recipe_pack]: https://www.curseforge.com/minecraft/customization/platys-advancement-and-recipe-pack

[logo]: logo.gif

[overview_example]: example/overview_example.png

[advancement_criteria]: example/advancement_criteria.png

[widget_example]: example/widget_example.png
