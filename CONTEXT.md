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
  and fire charges are not igniters. The torches and the oil lamp are the one exception: vanilla
  flint and steel lights them too, as it did in 1.12.
- **Powered igniter**: the ignition block, in stone and refractory brick, that lights whatever sits
  behind it for as long as the block in front of it carries a redstone signal. It lights the same
  things a hand-held igniter does, with no use time and no cost.
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
- **Lamp fuel**: a fluid the oil lamp burns, at a rate in millibuckets per minute. Pyroberry wine
  and lamp oil are the 1.12 fuels. A datapack can add more.
- **Douse**: putting out a lit torch, oil lamp, or campfire before it burns out: a bucket of water
  on a torch or campfire, an empty hand on the lamp, or rain on a torch. A doused torch keeps its
  remaining burn time. A fiber torch that has once been lit never drops itself again.
- **Pit burn**: turning a connected heap of fuel blocks (log piles, a full pile of wood chips, coal
  blocks, or blocks of wood tar) into charcoal, charcoal flakes, or coal coke by lighting it inside
  an enclosure of solid, non-flammable blocks. Every connected fuel block burns as an active pile,
  tar seeps downward into a tar collector, and a share of the fuel fails to ash instead.
- **Refractory burn**: a pit burn whose enclosure is built only of refractory blocks. Coal coke
  needs one, and every other pit burn fails far less often inside one.
- **Active pile**: the burning block a fuel block becomes for the length of a pit burn. It carries
  its recipe, its output so far, and the tar it has made, and collapses into pit ash when done.
  Breaking it early spills the output and ends that block's burn.
- **Pit ash**: the block an active pile collapses into when its burn finishes. It holds the burn's
  output until a player digs it up. It has no item and is only ever placed by a finished burn.
- **Refractory block**: a block that counts as a wall of a refractory burn: refractory bricks,
  refractory glass, the double refractory slab, the brick tar collector and drain, the brick
  powered igniter, and any block a datapack adds. A closed refractory door counts too.
- **Tool level**: the whole number, 0 to 4, that a tool brings to Pyrotech's machines. It sets the
  chops an axe needs on the chopping block, the uses a shovel needs on the compacting bin, the hits
  a hammer or pickaxe saves on the anvil, and a hammer's power on a bloom. Crude tools are 0; bone,
  flint, stone, and gold 1; iron and obsidian 2; diamond 3; netherite 4. A datapack can set the
  level of any tool. A level past the end of a machine's table counts as its highest.
- **Inherited recipe**: a recipe a higher tier of a machine gets by copying a lower tier's recipe,
  sometimes faster or with a lower failure chance. Seven chains inherit: pit kiln to stone kiln to
  brick kiln, crude drying rack to drying rack to stone oven to brick oven, chopping block to stone
  sawmill to brick sawmill, compacting bin to mechanical compacting bin, granite to ironclad to
  obsidian anvil, stone to brick crucible, and bloomery to wither forge. The copies ship with the
  mod; a datapack recipe added to a lower tier is not copied upward.
- **Bloom**: the block a bloomery or wither forge yields from ore. Its output is hammered out of it,
  on the ground or on an anvil, until its integrity is spent. A bloom remembers the recipe that made
  it, so the anvil knows what it yields and which anvils may work it.
- **Duration multiplier**: a per-machine number every recipe time in that machine is multiplied by,
  1 by default. A server sets it for all its players; it is not part of any recipe.
