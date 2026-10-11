# Bundled performance engines — attribution and licenses

This distribution embeds **unmodified third-party Fabric mods as separate nested
JARs**, not source ports or independently invented algorithms. Each module retains
its own Fabric mod ID, entrypoints, mixin configuration, authorship, and upstream
license. Each nested JAR can be removed or independently replaced with its original
upstream distribution, but the performance stack as shipped is tested only as a unit.

| Subsystem | Project / Version | Modrinth artifact | License | Source |
| --- | --- | --- | --- | --- |
| Chunk generation, scheduling and chunk I/O | C2ME 0.4.2-alpha.0.87+26.3 | `VSNURh3q:cfKRtp0m` | MIT (distributed modules only; proprietary OpenCL excluded) | https://github.com/RelativityMC/C2ME-fabric |
| Block and sky lighting | ScalableLux 0.3.0-alpha.0.6+26.3 | `Ps1zyz6x:g4eqNSKd` | LGPL-3.0-only | https://github.com/RelativityMC/ScalableLux |
| Entity ticking, collisions and game logic | Lithium 0.26.2 for 26.3 | `gvQqBUqZ:xS0Q8LSi` | LGPL-3.0-only | https://github.com/CaffeineMC/lithium |
| Immediate rendering (client only) | ImmediatelyFast 1.17.1+26.3 Fabric | `5ZwdcRci:3MP9UR23` | LGPL-3.0-or-later | https://github.com/RaphiMC/ImmediatelyFast |
| Block entity rendering (client only) | Optimized Block Entities 1.1.50 (26.3) | `AtOSAunf:nmIzJcIn` | LGPL-3.0-or-later | https://github.com/maDU59/OptimisedBlockEntities |
| Memory footprint and allocation | FerriteCore 9.0.0 Fabric (26.3-compatible) | `uXXizFIs:d5ddUdiB` | MIT | https://github.com/malte0811/FerriteCore |
| Rust-assisted entity queries and cramming (experimental upstream release) | Ferrite 0.7.4-alpha+26.3 | `c2DHmFvV:UhO60CKq` | MIT | https://github.com/VoiceLessQ/Ferrite |

Official licenses: https://www.gnu.org/licenses/lgpl-3.0.html and
https://opensource.org/license/mit .

Download exact upstream JARs, source and matching license documents from the
linked source repositories and Modrinth release pages. LGPL libraries retain
their ability to be inspected, replaced and independently modified: the binary
modules are deliberately **not shaded, obfuscated, merged or relinked** into
our Java/Rust code. Copies of any license files shipped within the upstream
archives remain inside those archives.

## Operational notes

- The C2ME monorepo also has a proprietary All Rights Reserved OpenCL
  acceleration module. **The downloaded distribution used here does not contain
  that module** (verified by examining its actual nested JAR manifest), and our
  bundle-validation script forbids OpenCL module paths. Do not add that module
  without separate written redistribution permission.
- Sodium is **not** bundled. Its PolyForm Shield license imposes separate
  conditions, and this project does not assume blanket redistribution rights.
- C2ME, ScalableLux and Ferrite 26.3 artifacts are **alpha releases**.
  Packaging them is not a guarantee of compatibility or unchanged worldgen
  results. We do not claim lossless behavior without matched-world smoke,
  chunk-hash, light-level and entity simulation tests.
- Mods and their configurations remain discoverable independently by Fabric
  Loader. Their optimization defaults remain the authors' defaults; we do not
  reduce client render distance, simulation distance, graphics quality,
  lighting or entity tick frequency automatically.
- This bundle is a distribution/integration, not a rewrite of Sodium, Lithium,
  C2ME, ScalableLux, ImmediatelyFast or Ferrite; speedups belong to the
  corresponding upstream algorithms and original developers.
