# Prototype: campfire interaction and tile sync

This branch answers wayfinder ticket 8 (Athenaeum replacement: interaction and tile sync).
It is throwaway code. Nothing here merges to main. Only the patterns do.

The prototype is a working campfire on the 1.21 template. It uses the migrated
1.12 campfire assets. All code lives in
`src/main/java/com/moostoet/pyrotech/prototype/campfire/`.

## How to run it

```bash
./gradlew runClient
```

Create a world, then try this sequence:

1. Take a Campfire from the Functional Blocks creative tab, or use `/give @s pyrotech:campfire`.
2. Place it on solid ground. You see the unlit tinder model.
3. Right-click it with oak logs a few times. The logs stack up on the fire (rendered from the real stacks, so different wood types keep their texture).
4. Sneak and scroll up with logs in hand: adds a log. Sneak and scroll down: removes one.
5. Right-click with flint and steel. The fire lights, glows brighter with more logs, and burns through them.
6. Right-click the top with raw porkchop. It floats over the fire and cooks (vanilla campfire recipes drive this). More logs cook faster.
7. Right-click the top with an empty hand to take the food. Leave cooked food on too long and it burns to charcoal (stand-in for burned food).
8. Click a side with an empty hand while lit: you pull a log and sometimes take burn damage.
9. Let it burn out completely: it collapses to an ash pile. Right-click with a shovel to scoop ash (drops bone meal as a stand-in for pit ash).
10. Break it at any point: contents drop.

## Pattern 1: what replaces the interaction framework

The answer: no framework. Vanilla 1.21 split the 1.12 `onBlockActivated` into
`useItemOn` (held item) and `useWithoutItem` (empty hand), and gives both the
full `BlockHitResult`. That covers most of what athenaeum's interaction system
was built to add.

| athenaeum (1.12) | this prototype (1.21) |
|---|---|
| `IInteraction[]` array, priority ordered | plain if-chain in `useItemOn`/`useWithoutItem`, same order (`CampfireBlock`) |
| interaction bounds + custom ray tracer | `BlockHitResult.getDirection()` (face dispatch); `getLocation()` is there for grid blocks like the worktable |
| `InteractionItemStack` insert/extract | small methods on the block entity (`insertFood`, `extractFoodTo`, `addLogFrom`, ...) backed by `ItemStackHandler` |
| item validation per interaction | `isItemValid`/`insertItem` overrides on the handlers, shared by hand and hopper |
| TESR solid pass (render stored items) | `BlockEntityRenderer` reading the synced handlers (`CampfireRenderer`) |
| mouse wheel packet + client hook | one payload record (`CampfireScrollPayload`) + one `MouseScrollingEvent` handler |
| `hasCapability`/`getCapability` | `RegisterCapabilitiesEvent.registerBlockEntity` with sided handlers |

What this costs per block: the dispatch if-chain is written per block instead of
composed from interaction objects. For the campfire that is about 60 lines. The
shared parts (hit-position helpers, a common BER base if wanted) can grow into a
small `library/interaction` package once two or three blocks exist, extracted
from real duplication instead of ported up front.

Not prototyped, same pattern applies:

- The additive "ghost preview" render pass (translucent held item shown in the
  slot you aim at). A BER can read `Minecraft.getInstance().hitResult` and draw
  it. Skipped for time, no unknown mechanism.
- Extinguishing by fluid (dousing). That is fluid capability work and belongs
  with the bucket module decisions.

## Pattern 2: what replaces the tile sync service

Two answers, and the split matters.

First: most of what 1.12 synced stops being block entity data at all. The 1.12
tile synced `active`, `dead`, and `ashLevel` through the service because 1.12
block metadata could not hold them. In 1.21 they are real blockstate properties
(`variant`, `ash`), which the converted blockstate JSON already expects. The
game syncs, relights, and re-renders blockstate changes on its own.

Second: what genuinely remains dynamic (the three item handlers) syncs through
`SyncedBlockEntity`, about 50 lines of vanilla idiom:

| athenaeum (1.12) | this prototype (1.21) |
|---|---|
| `TileDataService` ticked per server tick | none, vanilla handles delivery |
| register `ITileData[]` per field | `saveSynced`/`loadSynced` write one compound tag |
| per-field delta packets to the dimension | full snapshot in `ClientboundBlockEntityDataPacket`, sent to chunk trackers via `sendBlockUpdated` |
| `onTileDataUpdate()` + dirty checks | `onSyncedDataUpdate()` hook (campfire uses it to relight) |
| observers (`ObservableStackHandler`) | `onContentsChanged` on `ItemStackHandler`, calling `sync()` |

The trade: athenaeum sent minimal deltas, batched once per tick, to the whole
dimension. This pattern sends the whole synced snapshot on every change, only to
players tracking the chunk. Pyrotech tiles are small (a few stacks and ints), so
snapshots are cheap; the chunk-scoped audience is strictly better than
dimension-wide. If a hot tile ever changes many times per tick, a one-line
"dirty flag, sync in tick" batches it. No service needed.

One NeoForge extension carries the last 1.12 behavior: fuel-scaled light.
`IBlockExtension.getLightEmission(state, level, pos)` lets the lit campfire
glow brighter with more logs, with `LightEngine.checkBlock` called on fuel
changes (server side in the handler observer, client side in
`onSyncedDataUpdate`).

## Deliberate prototype shortcuts

These are stand-ins, not proposals:

- Pit ash drops as bone meal, burned food becomes charcoal (core module items do not exist yet).
- Vanilla `campfire_cooking` recipes drive cooking (the real recipe types are ticket 9).
- Fuel is `#minecraft:logs_that_burn` (1.12 used a config list).
- No config values. Burn time, ash chance, and damage chances are constants.
- No rain extinguishing, no campfire comfort/resting effects, no soaking pot checks.
- Ash accrues per consumed log instead of per tick at zero burn time.

## Open questions for the resolution

1. Is "no framework, per-block dispatch plus a small shared library grown on demand" acceptable, or do you want a structured interaction abstraction from day one?
2. Sneak+scroll log stacking is kept. Confirm it counts as gameplay (it is an input affordance, not a mechanic).
3. The ghost preview pass: keep it in the port, or drop it as 1.12 polish?
4. Full-snapshot sync per change instead of per-field deltas: fine at Pyrotech scale, or do you want the dirty-flag batching from the start?
5. `variant=item` exists only to satisfy the converted blockstate JSON. During hoisting, do we regenerate cleaned blockstates (drop the unused variant and its baked log models) or keep the converted files untouched?
