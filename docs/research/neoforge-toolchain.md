# NeoForge 1.21.1 toolchain and template

Research for issue #3 (NeoForge 1.21.1 toolchain and template). Researched on 2026-09-01.
All versions below come from primary sources. Each section links its sources.

## Pinned versions

| Component | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.249 |
| ModDevGradle plugin (`net.neoforged.moddev`) | 2.0.144 |
| Java | 21 (Temurin or any vendor, via Gradle toolchains) |
| Gradle | 9.2.1 (wrapper) |
| Parchment mappings (optional) | 1.21.1 / 2024.11.17 |
| Template | [NeoForgeMDKs/MDK-1.21.1-ModDevGradle](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle), branch `main` |

## NeoForge version: 21.1.249

The 21.1.x line targets Minecraft 1.21.1. The newest release on that line is 21.1.249.
The line contains no beta builds, so 21.1.249 is a stable release.
Source: the [NeoForged maven metadata](https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml) for `net.neoforged:neoforge`.
The official template also pins `neo_version=21.1.249` in its [gradle.properties](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/blob/main/gradle.properties).

## Gradle plugin: ModDevGradle 2, not NeoGradle

Use ModDevGradle (plugin id `net.neoforged.moddev`). Three reasons, each with a source:

- The official 1.21.1 template uses ModDevGradle. NeoForged maintains a NeoGradle variant too, but ModDevGradle is the active default. Source: the [NeoForgeMDKs org](https://github.com/orgs/NeoForgeMDKs/repositories), which stopped publishing NeoGradle MDKs after 1.21.9.
- ModDevGradle 2 is the stable current line since January 2, 2025. The NeoForged team tells all 1.0.x users to update to 2.0.x. NeoGradle continues to be supported, but is not where new work lands. Source: the [ModDevGradle 2 stable release post](https://neoforged.net/news/moddevgradle2/).
- New NeoForge snapshot lines gain support in ModDevGradle first. Source: the [26.1 snapshots post](https://neoforged.net/news/26.1snapshots/), which says only ModDevGradle supports them.

Pin plugin version 2.0.144. That is what the template pins in its [build.gradle](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/blob/main/build.gradle).
The newest release on the [Gradle plugin portal](https://plugins.gradle.org/plugin/net.neoforged.moddev) is 2.0.146 (2026-08-31). Bumping later is safe, but 2.0.144 is the reproducible baseline.

## Template: NeoForgeMDKs/MDK-1.21.1-ModDevGradle

NeoForged publishes one MDK repo per Minecraft version and Gradle plugin, under the [NeoForgeMDKs org](https://github.com/orgs/NeoForgeMDKs/repositories).
For this port, use [MDK-1.21.1-ModDevGradle](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle) on branch `main`.
It is a GitHub template repo. Its [README](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/blob/main/README.md) says to create a new repository from it and open it in an IDE.

The template ships these pieces (from its file tree):

- `build.gradle` with the ModDevGradle plugin, run configurations, and datagen wiring.
- `gradle.properties` with all versions and mod identity properties.
- `src/main/templates/META-INF/neoforge.mods.toml`. A `generateModMetadata` task expands `${mod_id}` style properties from `gradle.properties` into it at build time.
- Example sources: `ExampleMod.java` (main entry, `@Mod(MODID)`), `ExampleModClient.java` (client entry, `@Mod(value = MODID, dist = Dist.CLIENT)`), and `Config.java`.
- One static asset: `assets/examplemod/lang/en_us.json`.

## Java 21 and Gradle 9.2.1

Java 21 is the target. The template sets `java.toolchain.languageVersion = JavaLanguageVersion.of(21)`.
Its comment states the reason: "Mojang ships Java 21 to end users in 1.21.1, so mods should target Java 21."
Source: the template [build.gradle](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/blob/main/build.gradle).
The template's `settings.gradle` applies the Foojay toolchain resolver (version 1.0.0). Gradle then downloads a matching JDK automatically.

Gradle 9.2.1 is the wrapper version. Source: the template's [gradle-wrapper.properties](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/blob/main/gradle/wrapper/gradle-wrapper.properties).
Always run builds through `./gradlew`, so the wrapper version is the only Gradle version that matters.

## Datagen wiring

ModDevGradle declares runs in a `neoForge { runs { ... } }` block. Every run has a type: `client`, `server`, `data`, or `gameTestServer`.
Source: the [ModDevGradle README](https://github.com/neoforged/ModDevGradle#readme).

The template's `data` run is wired like this (from its [build.gradle](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/blob/main/build.gradle)):

```groovy
data {
    data()
    programArguments.addAll '--mod', project.mod_id, '--all',
        '--output', file('src/generated/resources/').getAbsolutePath(),
        '--existing', file('src/main/resources/').getAbsolutePath()
}
```

The conventions that follow from this:

- Generated files land in `src/generated/resources`. That folder is added to the main resources: `sourceSets.main.resources { srcDir('src/generated/resources') }`.
- The build excludes `src/generated/**/.cache` from the jar. Add that `.cache` folder to `.gitignore`. Commit the generated JSON itself.
- `--existing src/main/resources` lets providers reference the static files (models, textures). That fits our split: recipes and loot tables from datagen, everything else static.
- The Gradle task is `./gradlew runData` (ModDevGradle creates a `run<Name>` task per run).

The code entry point on 1.21.1 is a single `GatherDataEvent` on the mod event bus.
Class: `net.neoforged.neoforge.data.event.GatherDataEvent`. The client and server event split exists only on newer Minecraft versions, not on 1.21.1.
The event exposes `getGenerator()`, `getExistingFileHelper()`, `getLookupProvider()`, `includeServer()`, and `includeClient()`. It also has `addProvider(...)` and `createProvider(...)` helpers.
Source: the [GatherDataEvent source on the NeoForge 1.21.1 branch](https://github.com/neoforged/NeoForge/blob/1.21.1/src/main/java/net/neoforged/neoforge/data/event/GatherDataEvent.java).

Minimal skeleton:

```java
@EventBusSubscriber(modid = Pyrotech.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class PyrotechDatagen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        // event.addProvider(...) for recipes and loot tables per module.
    }
}
```

## Source layout for eight modules

Keep one Gradle project and one jar. Use one Java package per gameplay module.
A Gradle multi-project adds real cost (per-module metadata, cross-module wiring in the `mods` block) and buys nothing for a single-jar mod.
The template itself assumes one source set: its `mods` block binds the one mod id to `sourceSets.main`. Source: the template [build.gradle](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/blob/main/build.gradle).

Suggested layout under `src/main/java`:

```
com/moostoet/pyrotech/
  Pyrotech.java            main entry, @Mod, wires module init
  PyrotechClient.java      client entry, dist = Dist.CLIENT
  core/                    shared registries, base classes, helpers
  datagen/                 GatherDataEvent subscriber and providers
  module/
    <module1>/             one package per gameplay module (eight total)
    <module2>/
    ...
```

Each module package owns its blocks, items, block entities, and menus.
Each module exposes one `init(IEventBus modBus)` style hook. `Pyrotech.java` calls the eight hooks in order.
Datagen providers live in `datagen/`, grouped per module, so generated recipes and loot tables stay traceable to a module.

Static resources stay in `src/main/resources` (`assets/pyrotech/...` for models, blockstates, sounds, lang). Generated data lands in `src/generated/resources` (`data/pyrotech/recipe/...`, `data/pyrotech/loot_table/...`).

## Bootstrap steps

These steps produce a booting empty mod with mod id `pyrotech` and package `com.moostoet.pyrotech`.

1. Create the project from the template. Either use GitHub's "Use this template" on [NeoForgeMDKs/MDK-1.21.1-ModDevGradle](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle), or clone it and delete its git history:

   ```bash
   git clone --depth 1 https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle pyrotech
   cd pyrotech
   rm -rf .git TEMPLATE_LICENSE.txt
   ```

2. Edit `gradle.properties`. Set the mod identity and confirm the versions:

   ```properties
   mod_id=pyrotech
   mod_name=Pyrotech
   mod_group_id=com.moostoet.pyrotech
   mod_version=0.1.0
   mod_license=MIT
   minecraft_version=1.21.1
   minecraft_version_range=[1.21.1]
   neo_version=21.1.249
   ```

3. Confirm `build.gradle` pins `id 'net.neoforged.moddev' version '2.0.144'`. Leave the rest of the build script as the template ships it.

4. Move the sources. Rename `src/main/java/com/example/examplemod` to `src/main/java/com/moostoet/pyrotech`. Rename `ExampleMod.java` to `Pyrotech.java` and `ExampleModClient.java` to `PyrotechClient.java`. Update the package line and set `MOD_ID = "pyrotech"` in `Pyrotech.java`. Strip the example block, item, and creative tab code so the mod is empty. Delete or keep `Config.java` (it is a working config example).

5. Rename the asset namespace. Move `src/main/resources/assets/examplemod` to `src/main/resources/assets/pyrotech`. Replace the example lang entries.

6. Review `src/main/templates/META-INF/neoforge.mods.toml`. The `${mod_id}`, `${mod_version}`, and version-range properties expand from `gradle.properties` automatically. Only prose fields (description, authors, issue tracker URL) need manual edits.

7. Add the datagen subscriber from the section above as `com/moostoet/pyrotech/datagen/PyrotechDatagen.java`, with no providers yet.

8. Verify datagen runs:

   ```bash
   ./gradlew runData
   ```

   The run must exit successfully. With no providers it writes nothing. Add `src/generated/**/.cache` to `.gitignore`.

9. Verify the mod boots:

   ```bash
   ./gradlew runClient
   ```

   The client must start, and the Mods screen must list Pyrotech. For a headless check, `./gradlew runServer` must reach "Done" (the first server run needs `eula=true` in `run/eula.txt`).

10. Verify the jar builds:

    ```bash
    ./gradlew build
    ```

    The output is `build/libs/pyrotech-0.1.0.jar`.

## Sources

- [NeoForged maven metadata for net.neoforged:neoforge](https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml)
- [NeoForgeMDKs organization repository list](https://github.com/orgs/NeoForgeMDKs/repositories)
- [MDK-1.21.1-ModDevGradle template: build.gradle, gradle.properties, gradle-wrapper.properties, settings.gradle, README](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle)
- [ModDevGradle repository and README](https://github.com/neoforged/ModDevGradle)
- [ModDevGradle on the Gradle plugin portal](https://plugins.gradle.org/plugin/net.neoforged.moddev)
- [ModDevGradle 2 stable release post, NeoForged blog](https://neoforged.net/news/moddevgradle2/)
- [NeoForge for 26.1 snapshots post, NeoForged blog](https://neoforged.net/news/26.1snapshots/)
- [GatherDataEvent source, NeoForge branch 1.21.1](https://github.com/neoforged/NeoForge/blob/1.21.1/src/main/java/net/neoforged/neoforge/data/event/GatherDataEvent.java)

## Re-check notes

Two facts from a second research pass, kept here for later re-checks:

- The [maven latest-version API](https://maven.neoforged.net/api/maven/latest/version/releases/net/neoforged/neoforge?filter=21.1.) uses a prefix match on `filter`. Query with a trailing dot (`filter=21.1.`), or it also matches the 21.11.x line for a different Minecraft version.
- `runClient` needs a display. Under WSL2 that means WSLg or an X server. `runServer` is the headless check.
