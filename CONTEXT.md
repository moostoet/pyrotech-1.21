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
  shovel to pick up wood chips, and no crafting tables or furnaces in generated structures.
