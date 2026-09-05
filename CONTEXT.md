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
- **Campfire cook list**: the foods a Pyrotech campfire cooks. It is every item whose furnace
  recipe yields a food, except bread and cookie, plus any recipe written for the campfire itself.
  The vanilla campfire's own cooking recipes play no part. A campfire cooks at its own pace, not
  the furnace's, and food left in a lit campfire too long burns.
- **Campfire effects**: the five effects a lit campfire grants at night to a player resting beside
  it with no monster near. Comfort and resting come at once. Resting grows through three levels,
  and at the third grants well rested. Eating to a full stomach while comfortable grants well fed.
  Once comfort, well rested, and well fed are all present, focused follows. Comfort lets a player
  eat when full and makes food count for more; resting heals; well rested adds absorption; well
  fed halves exhaustion; focused banks an experience bonus that pays out on the next experience
  gained. Comfort and resting last while the player stays by the fire, focused until its bonus is
  spent, and well fed and well rested five minutes each.
- **Compost value**: the points an item adds to the compost bin. Every sixteen points yield one
  output item, four mulch by default. Any food without a value of its own gets one from its hunger
  and saturation, between 1 and 8. A datapack can set the value and the output of any item.
- **Wear stage**: how worn an anvil or a chopping block is. Anvils pass through four stages and
  chopping blocks through six before they break, each stage after a set number of hits or chops.
  A block broken off keeps its stage as an item and goes back down at that stage. A server can
  turn wear off.
- **Sealed barrel**: a barrel with its lid on. It takes no more items or fluid, rain no longer fills
  it, and its recipe runs. A sealed barrel keeps its contents when picked up and stacks one to a
  slot.
- **Rack climbing**: a player beside two or more drying racks stacked one above the other climbs
  them like a ladder by walking into the stack, holds on by sneaking, and takes no fall damage
  against them. Crude racks do not climb. A server can turn it off.
- **Airflow**: the number, from 0 up, that sets how fast a bloomery, wither forge, or stone
  combustion worker runs. An open front face gives 1 and a solid one 0, a bellows adds a bonus that
  fades every tick, and stored fuel chokes it. A partial slag heap in front counts as half open and
  a full one as closed. A device reads its own airflow; nothing pushes it from outside.
- **Hammer power**: the fraction of a hammer hit a bloom counts. It comes from the hammer's tool
  level and efficiency, the player's strength, weakness, and mining fatigue, and the distance from
  the bloom. Below 1 a hit counts for less, above 1 for more.
- **Integrity**: the number of items left in a bloom. A bloom starts with one random yield per
  input item. Each finished hammering cycle takes one, unless fortune saves it, and yields the
  recipe's output or a failure item.
- **Slag heap**: the eight-level pile a bloomery or wither forge builds in front of itself, one slag
  per level, molten for five minutes after the last addition. A heap of a metal's slag blooms
  again; a heap of plain slag fires into slag glass.
- **Render type**: the layer a block model draws on in 1.21 (solid, cutout, cutout mipped, or
  translucent), declared as `render_type` in the model JSON. It replaces the 1.12 `getBlockLayer()`
  override. A model inherits the field from its parent, so a base model shared by blocks on
  different layers leaves the field to the per-block wrappers.
- **Hot fluid**: a fluid whose temperature is 450 or more; lava is the vanilla one. A stone tank,
  stone faucet, or stone tar collector that takes one breaks and spills it; the brick versions
  hold it. The check runs only on a real fill, never on a simulated probe.
- **Large stack**: a slot holding more than a vanilla stack, up to 640 in the stash and 2560 in
  the durable rock bag. Vanilla saves a stack with a count cap of 99, so these slots save the
  item and its count separately through the shared `LargeStackHandler`.
- **Tank group**: a column of the same tank block stacked and connected, sharing one fluid that
  settles downward. Fills go in from the bottom up and drains come out from the top down. The
  connection state is the `connection` blockstate property.
- **Auto-pickup**: an open rock bag in a player's hand or hotbar taking a picked-up whitelisted
  item before the inventory does. Four `COMMON` toggles choose the locations; the main
  inventory is off by default.
- **Active tool**: a redstone or quartz tool in its boosted state. A redstone tool activates by
  chance when it takes damage or when its holder stands near redstone ore, and stays active for
  ten seconds of game time; while active it mines twice as fast, usually skips damage, and the
  sword hits twice as hard. A quartz tool is active whenever it is in the Nether, mines three
  times as fast, and the sword hits three times as hard. The active texture shows the state.
- **Repair kit**: a four-use bone or flint item that, crafted with a hammer and a damaged durable
  tool of its material, restores a quarter of that tool's durability. The hammer takes four
  damage and the kit one; a tool at full durability does not match the recipe.
- **Cog**: the consumable gear that drives a mechanical hopper, compactor, mulcher, trip hammer, or
  mechanical bellows. Eight kinds, from wooden to diamond. A cog has durability, loses some on every
  work cycle, and its kind sets how much work one cycle does. Being a cog at all is membership in
  the cog data map. A redstone signal stops the machine rather than speeding it.
- **Sawmill blade**: the consumable blade a sawmill needs to cut. Seven kinds. The blade decides
  which recipes the sawmill can run, how fast, how many items come out, and how many wood chips
  spill. A stone sawmill accepts only stone, flint, and bone.
- **Combustion worker**: a Pyrotech machine that burns furnace fuel to run recipes: the eight stone
  and brick tier machines, and in a looser sense the campfire and the pit kiln. It holds a fuel
  slot, keeps a burn time, and airflow makes both the burn and the recipe run faster in step.
- **Keep heat**: a brick tier machine stays lit when it runs out of work, where a stone tier machine
  goes out fifty ticks later. A machine that has gone out but still has burn time left shows the
  dormant look.
- **Machine tier**: stone or brick. The brick tier of a machine takes the stone tier's recipes
  unchanged, doubles the value of its fuel, holds twice the fuel, keeps heat, and reads airflow at
  one and a half times. Only the kiln's failure chance changes with the tier.
- **Oven cook list**: the foods a Pyrotech oven cooks. It is every item whose furnace recipe yields
  a food, minus its own blacklist tag, which ships empty. It is derived the way the campfire cook
  list is, but it keeps no exceptions of its own, because the campfire's bread and cookie are
  early-game gating rather than an oven limit.
- **Carcass**: the block a Pyrotech-killed animal leaves instead of its drops. The kill spawns a carcass item holding the captured drops, and a player places it and works it with a knife to get the hides, pelts, meat, bone shards, and lard back out.
- **Knife efficiency**: the progress one knife use adds at a carcass or a butcher's block, per knife material. A carcass and a butcher's block have separate numbers for the same knife.
- **Butchering transform**: the swap applied to an item as it comes out of a carcass, chosen by which knife is held. A butcher's knife doubles meat and ruins pelts, a hunter's knife doubles pelts and taints meat.
- **Tannin**: the fluid brewed in a barrel from four leaves and a bucket of water. Soaking a washed hide in it makes a tanned hide, the step before leather.
- **Sharp tool**: any axe, sword, or knife, the set that can whittle a marshmallow stick. In 1.21 it is the `#pyrotech:sharp_tools` tag.
- **Worldgen data**: the three datapack layers that replace the eleven 1.12 chunk generators. A
  configured feature says what to place, a placed feature says where and how often, and a
  `neoforge:add_features` biome modifier says which biomes and which generation step. Worldgen's
  datagen writes all of it into `src/generated/resources`, one biome modifier file per generator.
- **Worldgen toggle**: the per-generator on and off switch kept from the 1.12 config. It is a
  `COMMON` boolean read by a Pyrotech `ICondition` attached to that generator's biome modifier
  entry, so a false toggle makes the entry fail to load and the generator never runs. Already
  generated chunks do not change either way.
- **Cave-floor cluster**: the feature behind dense redstone and dense quartz. It looks in a 9 by 9
  by 9 cube for air over a floor block, places a large, then small, then rocks variant in that
  order, and at half chance turns the block underneath into the vanilla ore. Redstone makes one
  attempt on stone or deepslate, quartz makes up to twenty on netherrack.
- **Rock ground**: the block tag `#pyrotech:rock_placeable_on` that the rock patch tests one block
  below the position it wants. It holds `#minecraft:dirt`, `#minecraft:base_stone_overworld` and
  `#minecraft:terracotta`, mirroring the 1.12 GROUND, GRASS and ROCK material check. Sand and
  gravel are out. The rock block itself only asks for a solid face below, as in 1.12.
