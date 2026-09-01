# Pyrotech glossary

- **Module**: one of the top-level feature packages of Pyrotech. The eight gameplay
  modules are core, tech, storage, tool, ignition, hunting, bucket, and worldgen.
  The patreon and plugin packages are modules too, but not gameplay modules, and
  they are out of scope for the port.
- **Hoisting**: moving one module's behavior from the 1.12 code to the 1.21 code by
  rewriting it against modern idioms. Hoisting is a rewrite, not a copy.
- **Faithful gameplay**: a ported mechanic behaves the same as in 1.12. Same recipes,
  same progression, same balance, even where the code differs completely.
- **Upstream**: codetaylor/pyrotech-1.12, the original mod. It is preserved unchanged
  on this repository's `1.12` branch.
