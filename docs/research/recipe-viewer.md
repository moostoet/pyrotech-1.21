# Recipe viewer research: JEI or EMI first

Research date: 2026-09-01. All claims below come from primary sources. Each claim links to its source.

## Question

Pyrotech is being ported to NeoForge 1.21.1. It has many custom machine recipe types (kiln, chopping block, drying rack, anvil, soaking pot, compacting bin, and more). Which recipe viewer should the port target first, JEI (Just Enough Items) or EMI?

## Recommendation

Target JEI first. Ship one JEI plugin with one recipe category per machine.

Three facts drive this choice. First, a JEI plugin also reaches EMI users. EMI bridges JEI plugins through its built-in JEMI layer, and the TooManyRecipeViewers mod covers EMI setups without JEI. The reverse is not true. An EMI-only plugin gives JEI users nothing. Second, the largest NeoForge 1.21.1 content pack, All the Mods 10, ships JEI and not EMI. Third, JEI already supports every Minecraft version through 26.2, while EMI supports nothing newer than 1.21.1. A JEI plugin has a clear path when Pyrotech later moves past 1.21.1.

A native EMI plugin is a good second step, not the first. The JEMI bridge has known fidelity gaps (details below), and a native EMI plugin fixes those for EMI users.

## JEI on NeoForge 1.21.1

### Availability

JEI supports NeoForge 1.21.1 and is actively updated for it. The Modrinth API lists 50 NeoForge builds for 1.21.1. The newest is 19.51.0.417, published on 2026-08-30 ([Modrinth API, JEI versions for NeoForge 1.21.1](https://api.modrinth.com/v2/project/jei/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D)). Modrinth flags these 1.21.1 builds as beta. JEI has 73.8 million downloads on [Modrinth](https://modrinth.com/mod/jei) and 617 million on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/jei).

The maven at [maven.blamejared.com/mezz/jei](https://maven.blamejared.com/mezz/jei/) publishes `jei-1.21.1-neoforge` and `jei-1.21.1-neoforge-api`. The 1.21.1 artifacts run from 19.8.4.110 (August 2024) to 19.51.0.417 (August 2026). The [Getting Started wiki page for 1.21 and 1.21.1](https://github.com/mezz/JustEnoughItems/wiki/Getting-Started-%5BMinecraft-1.21-and-1.21.1%5D) gives the dependency pattern: `compileOnly("mezz.jei:jei-1.21.1-neoforge-api:...")` plus `runtimeOnly("mezz.jei:jei-1.21.1-neoforge:...")`.

### API surface for custom categories

The [Creating Plugins wiki page for 1.21 and 1.21.1](https://github.com/mezz/JustEnoughItems/wiki/Creating-Plugins-%5BMinecraft-1.21-and-1.21.1%5D) documents the plugin model. A plugin is a class annotated with `@JeiPlugin` that implements `IModPlugin` and returns a unique id from `getPluginUid()`. JEI calls the plugin in stages: `registerCategories`, `registerRecipes`, `registerRecipeCatalysts`, `registerGuiHandlers`, and `registerRecipeTransferHandlers`. The page also covers recipe click areas and recipe transfer.

Each category implements `IRecipeCategory<T>` for one `RecipeType<T>`. The [interface source on the 1.21.1 branch](https://github.com/mezz/JustEnoughItems/blob/1.21.1/CommonApi/src/main/java/mezz/jei/api/recipe/category/IRecipeCategory.java) requires four methods: `getRecipeType`, `getTitle`, `getIcon`, and `setRecipe`. Optional defaults cover `getWidth`, `getHeight`, `draw`, `getTooltip`, and `createRecipeExtras`. The old `getBackground` method is deprecated since 19.19.0 in favor of width and height. `IGuiHelper` builds drawables for icons and backgrounds.

### Documentation quality

The [JEI wiki](https://github.com/mezz/JustEnoughItems/wiki) keeps separate Getting Started and Creating Plugins guides per version band, including a dedicated pair for 1.21 and 1.21.1. The wiki tells developers to consult the javadocs for exact signatures. The maven ships a `-sources.jar` with the API (javadoc comments inline), but no separate javadoc jar ([maven directory for jei-1.21.1-common-api 19.51.0.417](https://maven.blamejared.com/mezz/jei/jei-1.21.1-common-api/19.51.0.417/)). The docs are good for the exact target version. There is no dedicated breaking-changes page in the wiki sidebar.

## EMI on NeoForge 1.21.1

### Availability

EMI supports NeoForge 1.21.1 with stable releases. The Modrinth API lists 16 NeoForge builds for 1.21.1, from 1.1.12 (August 2024) to 1.1.24, published on 2026-05-13 ([Modrinth API, EMI versions for NeoForge 1.21.1](https://api.modrinth.com/v2/project/emi/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D)). EMI has 27.5 million downloads on [Modrinth](https://modrinth.com/mod/emi) and 46.7 million on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/emi).

One important limit: 1.21.1 is the newest Minecraft version EMI supports at all. The [Modrinth project data](https://api.modrinth.com/v2/project/emi) lists game versions from 1.18.2 through 1.21.1 and nothing newer. The [EMI GitHub branches](https://github.com/emilyploszaj/emi/branches) also stop at a 1.21 branch, with no 1.21.4 or 26.x branch.

The [EMI README on the 1.21 branch](https://github.com/emilyploszaj/emi/blob/1.21/README.md) gives the dependency: `compileOnly "dev.emi:emi-neoforge:${emi_version}:api"` from the maven at `https://repo.sleeping.town/`.

### API surface for custom categories

The [EMI Getting Started Guide](https://github.com/emilyploszaj/emi/wiki/Getting-Started-Guide) documents the plugin model. A plugin implements `EmiPlugin` and carries the `@EmiEntrypoint` annotation on Forge-family loaders. In `register(EmiRegistry)`, the plugin adds an `EmiRecipeCategory` per machine, adds recipes, and calls `addWorkstation` to tie machines to categories. Recipes implement `EmiRecipe` (or extend `BasicEmiRecipe`). Stacks use `EmiStack` and `EmiIngredient`. Layout uses `WidgetHolder` methods.

The [EmiRecipe interface source](https://github.com/emilyploszaj/emi/blob/1.21/xplat/src/main/java/dev/emi/emi/api/recipe/EmiRecipe.java) requires `getCategory`, `getId`, `getInputs`, `getOutputs`, `getDisplayWidth`, `getDisplayHeight`, and `addWidgets`. The declared inputs and outputs feed EMI's recipe tree and craftable filtering. Defaults cover `getCatalysts`, `supportsRecipeTree`, and `hideCraftable`.

### Documentation quality

EMI's developer docs live in the [GitHub wiki](https://github.com/emilyploszaj/emi/wiki) (10 pages). The Getting Started Guide is the main developer document and walks the full flow. The API source carries javadoc comments. Coverage is thinner than JEI's wiki, with one guide instead of per-topic pages, but the guide matches the current API.

## JEI plugins inside EMI

EMI can consume JEI plugins on NeoForge 1.21.1. The mechanism is built into EMI itself. The bridge code lives in the `dev.emi.emi.jemi` package inside EMI's own source tree ([package listing on the 1.21 branch](https://github.com/emilyploszaj/emi/tree/1.21/xplat/src/main/java/dev/emi/emi/jemi)). The EMI Modrinth page states the setup: "For runtime JEI compat, all you need to do is install JEI alongside EMI and they will work together to share recipes" ([EMI project body via the Modrinth API](https://api.modrinth.com/v2/project/emi)). So a mod that ships only a JEI plugin gets its custom categories inside EMI, as long as the player installs both viewers.

A second option removes the JEI install requirement. [TooManyRecipeViewers (TMRV)](https://modrinth.com/mod/tmrv) by Nolij is "a compatibility layer for running JEI plugins with EMI without having to install JEI". It supports NeoForge 1.21.1 and has 821,857 downloads. Its source is at [github.com/Nolij/TooManyRecipeViewers](https://github.com/Nolij/TooManyRecipeViewers). TMRV documents its own limits. It reads only the JEI blacklist config. Recipe manager plugins mostly fail extraction. Runtime registry changes throw after `onRuntimeAvailable`.

The built-in JEMI bridge has known fidelity gaps, tracked in EMI's own issue tracker:

- Cycling item slots misbehave ([issue #1244, JEMI struggles with cycling slots](https://github.com/emilyploszaj/emi/issues/1244), open, August 2026).
- Fluid subtype data was skipped, so potion-style variants went missing ([issue #1240, fix JEI subtype data handling in JEMI](https://github.com/emilyploszaj/emi/issues/1240), open, August 2026).
- Some bridged recipes crash rendering ([issue #1245, EMI fails to render recipes sometimes](https://github.com/emilyploszaj/emi/issues/1245), open, August 2026).
- JEI's custom input handling (`IJeiGuiEventListener`) does not work through the bridge, and bridged recipes get an extra border ([issue #826, mod's JEI plugin has issues with EMI](https://github.com/emilyploszaj/emi/issues/826)).
- Some JEI category types never appear, for example anvil-style categories ([issue #339, Quark's anvil registry changes don't appear](https://github.com/emilyploszaj/emi/issues/339), open since 2023).

Conclusion for Pyrotech: bridged categories work for plain item-in, item-out layouts. Pyrotech's recipes are mostly that shape. Interactive widgets, cycling focus stacks, and fluid variants are the risk areas.

## Adoption in current 1.21.1 packs

All the Mods 10 is the flagship NeoForge 1.21.1 kitchen-sink pack. Its official repository shows it ships JEI and not EMI. The [config directory](https://github.com/AllTheMods/ATM-10/tree/main/config) contains a `jei` folder and no EMI config. The [changelog for pack version 8.1](https://github.com/AllTheMods/ATM-10/blob/main/changelogs/CHANGELOG-ATM10-8.0-8.1.md) updates "Just Enough Items (19.44.0.401)" to "(19.50.0.414)" and never mentions EMI.

Both viewers see heavy standalone use on 1.21.1. JEI's newest two NeoForge 1.21.1 builds each passed 30,000 Modrinth downloads within days of their late-August 2026 release ([Modrinth API](https://api.modrinth.com/v2/project/jei/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D)). EMI's NeoForge 1.21.1 builds 1.1.22 and 1.1.24 each passed one million Modrinth downloads ([Modrinth API](https://api.modrinth.com/v2/project/emi/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D)). The top NeoForge 1.21.1 packs on Modrinth by downloads are Pixelmon, performance, and Cobblemon packs ([Modrinth search API](https://api.modrinth.com/v2/search?facets=%5B%5B%22project_type%3Amodpack%22%5D%2C%5B%22versions%3A1.21.1%22%5D%2C%5B%22categories%3Aneoforge%22%5D%5D&index=downloads)). Those give little signal for a tech mod. One of them does show the both-installed setup: the Cobblemon Official Modpack for NeoForge lists both JEI and EMI as dependencies of its current 1.21.1 release ([Modrinth API, Cobblemon pack versions](https://api.modrinth.com/v2/project/cobblemon-neoforge/version?game_versions=%5B%221.21.1%22%5D)).

## API stability

JEI changes its API with each Minecraft version. Each Minecraft version gets its own artifact family and major version, for example 19.x for 1.21.1 and 27.x for 1.21.11 ([maven listing](https://maven.blamejared.com/mezz/jei/)). The wiki keeps separate plugin guides per version band for the same reason ([wiki home](https://github.com/mezz/JustEnoughItems/wiki)). Within one Minecraft version, JEI is stable. The 1.21.1 line stayed on 19.x from August 2024 through August 2026 while receiving updates.

EMI holds one API line across Minecraft versions. Release 1.1.24 shipped simultaneously for 1.19.2, 1.20.1, and 1.21.1 ([EMI GitHub releases](https://github.com/emilyploszaj/emi/releases)). Every NeoForge 1.21.1 build is a 1.1.x version ([Modrinth API](https://api.modrinth.com/v2/project/emi/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D)). That is less churn per Minecraft version than JEI. The trade-off is pace. EMI supports no Minecraft version newer than 1.21.1 as of this research date, while JEI already covers 26.2.

## Integration surface Pyrotech needs (JEI first)

Dependency setup. Add the maven at `https://maven.blamejared.com/`. Use `compileOnly` on `mezz.jei:jei-1.21.1-neoforge-api` and `runtimeOnly` on `mezz.jei:jei-1.21.1-neoforge`, current version 19.51.0.417 ([Getting Started wiki page](https://github.com/mezz/JustEnoughItems/wiki/Getting-Started-%5BMinecraft-1.21-and-1.21.1%5D)).

Plugin class. One class annotated `@JeiPlugin` that implements `IModPlugin`. Return `ResourceLocation` id `pyrotech:jei` from `getPluginUid()`. Keep all JEI imports inside one plugin package. The wiki advises isolating JEI API references so the mod runs without JEI ([Creating Plugins wiki page](https://github.com/mezz/JustEnoughItems/wiki/Creating-Plugins-%5BMinecraft-1.21-and-1.21.1%5D)).

Recipe types and categories. Create one `RecipeType<T>` and one `IRecipeCategory<T>` per machine: kiln, chopping block, drying rack, anvil, soaking pot, compacting bin, and the rest. Each category implements `getRecipeType`, `getTitle`, `getIcon` (a 16 by 16 drawable from `IGuiHelper`), and `setRecipe` (place input and output slots with `IRecipeLayoutBuilder`). Override `draw` for progress arrows and burn indicators. Override `getTooltip` for extra recipe info like campfire-only or tier requirements.

Registration. In `registerCategories`, add all categories through `IRecipeCategoryRegistration` and grab `IGuiHelper` from `getJeiHelpers()`. In `registerRecipes`, load each machine's recipes from the `RecipeManager` and call `addRecipes(type, list)`. In `registerRecipeCatalysts`, call `addRecipeCatalyst` with each machine's block item and its recipe type. Catalysts make the machine appear as the workstation for its category.

Click areas and recipe transfer. These need `registerGuiHandlers` and `registerRecipeTransferHandlers`. They only apply to machines with container screens. Most Pyrotech machines are in-world blocks without a GUI. Skip both at first. Add a click area handler later for any machine that gets a real screen.

EMI as step two. Later, add a native EMI plugin behind `@EmiEntrypoint`. Reuse the same recipe data. Add one `EmiRecipeCategory` per machine, register recipes with real `getInputs` and `getOutputs`, and call `addWorkstation` for each machine ([EMI Getting Started Guide](https://github.com/emilyploszaj/emi/wiki/Getting-Started-Guide)). That upgrades EMI users from bridged categories to native ones, with a working recipe tree and craftable filtering.

## Claims I could not verify

- I could not verify that "many 1.21.1 packs ship both viewers" from primary sources. Of the packs I could inspect, All the Mods 10 ships JEI only, and the Cobblemon Official Modpack ships both.
- CurseForge pack manifests are not published in a fetchable form for most packs. Pack adoption evidence beyond All the Mods 10 is therefore thin.
- The original JEMI standalone repository is gone. The old `github.com/emilyploszaj/jemi` URL returns 404. The bridge now demonstrably lives inside EMI's source tree, so this does not change the conclusion.
