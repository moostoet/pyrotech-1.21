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

## Credits and license

Pyrotech was created by [codetaylor](https://github.com/codetaylor).
This repository is a fork of
[codetaylor/pyrotech-1.12](https://github.com/codetaylor/pyrotech-1.12)
and reuses its assets and gameplay design.

Licensed under the Apache License 2.0. See [LICENSE](LICENSE).
