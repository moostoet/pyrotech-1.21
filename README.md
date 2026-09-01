# Pyrotech

A port of [Pyrotech](https://github.com/codetaylor/pyrotech-1.12) by
[codetaylor](https://github.com/codetaylor) from Minecraft 1.12.2 to
Minecraft 1.21.1 on [NeoForge](https://neoforged.net/).

Pyrotech is a primitive technology mod. It slows down the early game with
fire, stone, and hand tools before granting access to modern machines.

## Status

The port is in progress. The mod boots, but holds no gameplay content yet.
The gameplay modules are ported one at a time.

The goal is faithful gameplay with modern code. Every mechanic behaves as it
does in 1.12.2. The code is rewritten against vanilla and NeoForge idioms.

The original 1.12.2 source is preserved unchanged on the
[`1.12` branch](https://github.com/moostoet/pyrotech-1.21/tree/1.12).
The domain glossary lives in [CONTEXT.md](CONTEXT.md).

## Toolchain

- Minecraft 1.21.1 on NeoForge 21.1.249
- ModDevGradle 2.0.144
- Java 21, Gradle 9.2.1 (wrapper)

Common commands:

- `./gradlew runClient` starts a development client.
- `./gradlew runServer` starts a development server.
- `./gradlew runData` runs datagen into `src/generated/resources`.
- `./gradlew build` builds the jar into `build/libs`.

## Running the client on Windows from WSL

WSLg has issues running the Minecraft client, so graphical runs go through
Windows-side Java while the repo stays on the WSL filesystem:

- `./gradlew-win` (run from WSL) starts the client on the Windows desktop.
  It accepts any Gradle arguments (`./gradlew-win build`) and defaults to
  `runClient`.
- It works by invoking `gradlew.bat` through PowerShell interop inside
  `W:\home\...`, where `W:` is a persistent network drive mapped to
  `\\wsl.localhost\Ubuntu-24.04` (the script re-maps it if it dropped).
- The Windows-side Gradle keeps its per-project cache under
  `%USERPROFILE%\pyrotech-1.21\gradle-project-cache` because the
  `\\wsl.localhost` share does not support the file locking and watching
  Gradle needs.
- For the same reason the game directory of Windows runs (worlds, options,
  logs) is `%USERPROFILE%\pyrotech-1.21\run` instead of `run/`: creating a
  world locks `session.lock`, which fails on the share and shows up in-game
  as "Failed to copy packs". From WSL that directory is reachable under
  `/mnt/c/Users/<you>/pyrotech-1.21/run`.
- All of these live in the profile root rather than `%LOCALAPPDATA%` on purpose:
  when the launching terminal belongs to a packaged (Store) app such as the
  Claude desktop app, Windows silently redirects AppData writes into that
  app's `Packages\...\LocalCache` folder, which would split caches and
  worlds between terminals.
- The Windows build also writes its `build/` directory to
  `%USERPROFILE%\pyrotech-1.21\build`, so the Minecraft recompile and all
  outputs stay on NTFS. Do not switch that recompile to the Eclipse compiler
  (`neoFormRuntime.useEclipseCompiler`) as a workaround for share problems:
  javac cannot read some class files ECJ produces (for example
  `ItemDisplayContext`), which breaks `compileJava` with "bad class file"
  errors.

Linux and Windows builds use separate caches, daemons and build directories,
so they do not interfere with each other.

## Credits and license

Pyrotech was created by [codetaylor](https://github.com/codetaylor).
This repository is a fork of
[codetaylor/pyrotech-1.12](https://github.com/codetaylor/pyrotech-1.12)
and reuses its assets and gameplay design.

Licensed under the Apache License 2.0. See [LICENSE](LICENSE).
