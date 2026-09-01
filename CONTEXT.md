# Pyrotech glossary

- **Module**: one of the top-level feature packages of Pyrotech. The eight gameplay
  modules are core, tech, storage, tool, ignition, hunting, bucket, and worldgen.
  The patreon and plugin packages are modules too, but not gameplay modules, and
  they are out of scope for the port.
- **Hoisting**: moving one module's behavior from the 1.12 code to the 1.21 code by
  rewriting it against modern idioms. Hoisting is a rewrite, not a copy.
- **Hoisting unit**: one ticket-sized chunk of the port. Each gameplay module is one
  hoisting unit, except tech, which splits into four: basic, machine, bloomery, and
  refractory. Eleven units in total.
- **Faithful gameplay**: a ported mechanic behaves the same as in 1.12. Same recipes,
  same progression, same balance, even where the code differs completely.
- **Upstream**: codetaylor/pyrotech-1.12, the original mod. It is preserved unchanged
  on this repository's `1.12` branch.
- **Igniter**: a hand-held item from the ignition module that lights Pyrotech blocks such as
  the campfire, the pit kiln, and the bloomery. Only igniters light them. Vanilla flint and steel
  and fire charges are not igniters.
- **Ignition hook**: the core-owned question "can this position start a pit burn, and if so,
  start it". Refractory supplies the answer; igniters, the powered igniter block, and spreading
  fire ask it. It exists so that core and ignition never depend on refractory.
- **Tweak**: a core config toggle that changes vanilla behavior to protect Pyrotech's
  progression: no wool from sheep, sticks from leaves, iron ore instead of ingots in loot, a
  shovel to pick up wood chips, and the two village tweaks.
- **Village tweaks**: the two tweaks that strip vanilla crafting tables, vanilla furnaces,
  including the blast furnace and smoker, and vanilla campfires from every newly generated chunk,
  whatever placed them. The crafting table and the campfire disappear; the furnaces become
  cobblestone. The furnace tweak also keeps a furnace out of village chest loot. Chunks generated
  before Pyrotech joined a world are left alone.
- **Gate**: a step in Pyrotech's early game that vanilla gives away but Pyrotech makes the player
  earn: the 3x3 grid, smelting, cooked food, planks, sticks, stone tools, torches, coal, bone meal,
  stone slabs, and the rest of what the removal list covers. Each gate has one Pyrotech route past
  it.
- **Progression skip**: a vanilla block, recipe, or drop that lets a player past a Pyrotech gate
  without the Pyrotech route. The vanilla furnace in a village is one; a vanilla recipe added
  after 1.12 that outputs a gated item is another.
- **Removal list**: the vanilla recipes Pyrotech drops so that its own route past each gate is
  the only route. It holds the 1.12 lists plus every recipe vanilla added since that skips the
  same gates. A user datapack can put any of them back.
