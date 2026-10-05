# Void Monster — Minecraft 26.3 / NeoForge port

Target: **Minecraft 26.3**, **NeoForge 26.3.0.26-beta**, **Java 25**, **Gradle 9.7.1**.

## Accepted source boundary

The active port lives in `src/main/java/reika/voidmonster`. The original root-level Java files,
including `World/`, `Entity/`, `Render/`, and the two `ModInterface/` integrations, remain
pristine 1.7.10 references. `build.gradle` compiles the entire modern source directory; it has
no exclusions that hide incomplete active code. The root build includes this module, and
TestInstance loads it alongside the seven family mods.

The core gameplay/client port is implemented and builds. **This does not mark every optional
1.7.10 integration as ported.** The pending external-mod contracts are recorded below rather
than replaced with empty implementations. In-game visual comparison remains a separate
validation step; client startup alone does not establish visual parity.

## Implemented behavior

| Area | Modern implementation and preserved source behavior |
| --- | --- |
| Bootstrap | NeoForge mod lifecycle, common TOML config/reload, entity attributes, typed payload registration, DragonAPI mod/donation registration, client-only bootstrap. |
| World generation | Level pre-tick, eight-tick cadence, flat-world veto except the Nether, one registered monster per level, API dimension overrides, eligible-player selection, seasonal ghost chance, Nether flags. |
| Ambient sounds | Server player pre-tick, 1/400 chance, darkness/sky/height checks, biome/dimension API exclusions, original volume/stacking/pitch, targeted sound packets. |
| Movement | Nearest active bait before player targeting; original speed/distance curves, vertical motion, ghost phasing, multipoint sight checks, below-world recovery, nearest-space escape, debris attraction. |
| Combat | Difficulty-scaled health/attack, health/LP/armor drain, reflection and hit cooldown, healing immunity, fire/blindness, ghost regeneration and actual healing-splash expulsion, player-only ordinary damage, turret callbacks. |
| Lifetime | Forced persistence, immunity to automatic despawn and void death, saved Nether/ghost/persistence flags, saved difficulty boosts, lightning on death, 20,000 XP for normal monsters, no ghost loot or XP. |
| Public API | Dimension rules, ambient exclusions, custom drops, nearest monster, tick hooks, player-look event, cancellable eat-light event. Ordinary torches/glowstone retain their cancellation exception. |
| Loot | Datagen owns all eleven original item entries, their minimum difficulty/count formulas, and the original vanilla-only random enchanted books. API/config additions are the only runtime drop lists. |
| Custom drops | Original `.drops` format and example items; vanilla DFU migrates old metadata, numeric enchantments, durability and display/custom NBT to 26.3 components. Native stacks use a companion JSON file. |
| Rendering | Native extracted entity/model states and submit collectors, portal-textured shell, scrolling healing armor, original 32-frame flare, original growth atlas and six-face overlays, black colliding death motes. |
| Fog/distortion | Original height/color curves, proximity visibility ramp, fog/sky color, camera far plane, original radial warp/desaturation GLSL with aspect correction and a per-frame std140 focus buffer. |
| Network | Nether/ghost/healing state uses SynchedEntityData. Periodic verification uses dimension names and UUIDs to avoid removing a replacement entity with a reused numeric ID. |
| RotaryCraft | Optional isolated entity adapter implements radar jamming and the full target/turret API. The base entity can load when RotaryCraft is absent. |

The authored default loot is diamond 2–8 at difficulty 0.8; ghast tear 1 at 0.6; glistering
melon 2–5 at 0; emerald 2–6 at 0.4; ender pearl 1–3 at 0.2; ender eye 1–3 at 0.25;
fire charge 2–8 at 0; nether wart 8–22 at 0; nether star 1–2 at 1; obsidian 6–16 at
0.6; gunpowder 8–12 at 0.3. Counts preserve `min + (int)(difficulty * randomRange)`;
the minimum is not multiplied. Books preserve `1 + 2 * ceil(max(1, 1 + difficulty))`.
There are no invented recipes or new item/block registrations.

## Deliberate migrations and fixes

- Dimension/biome APIs now accept `ResourceKey` values instead of integer IDs. The End is
  mapped in the default blacklist. Original default **-112** is recorded in config comments;
  its replacement dimension name is unresolved and has not been invented.
- Old absolute Y coordinates are relative to `level.getMinY()` or `getMaxY()`: overworld
  spawning is ten blocks below the minimum, Nether spawning is five above the maximum,
  and recovery/near-floor fog/growth/cave checks follow the dimension's build height.
- Nether-like behavior uses 26.3's `EnvironmentAttributes.WATER_EVAPORATES` rather than
  the removed ultraWarm accessor.
- Original hardcoded spawn admission for Overworld/Nether/Deep Dark remains distinct from
  normal dimension admission used by entity ticks, preserving the source's behavior.
- Cooldowns use level-local absolute **long** game-time deadlines. The old implementation
  checked an integer-keyed map with the world object and stored a duration as an absolute
  timestamp. Extending a cooldown never shortens it; unload removes its entry.
- Empty/fake/new-player candidate lists return no candidate instead of calling nextInt(0).
  Healing's random bound is also valid at very high difficulty.
- The drain falloff's `fullDist-2` numerator was always zero. Constant drain preserves that
  behavior while removing the NaN at the exact six-block boundary.
- Original growth atlas `frame%10 / 5` relied on repeating texture coordinates. Modulo five
  samples the same five-column atlas without an out-of-range UV.
- Default Forge-style runtime drops were replaced by a reloadable loot table with registered
  condition/count/entry codecs, using **26.3** `SingleEntryContainerBase` and ContextIntProvider.
- A healing-effect mixin replaces the absent DragonAPI SplashPotionEvent; the real vanilla
  ThrownSplashPotion dispatch is covered by a server test. A client Camera mixin applies the
  original far-plane constraint before projection/frustum extraction.
- The original crystal transforms were checked against Mojang's **1.7.10 bytecode** using
  Forge's original SRG mappings. The preRender callback precedes RenderLiving's -1.5078125
  model-origin translation; nested cube scales are 0.875/0.875; shell rotation/bob are
  innerRotation*3/0. The regular core retains the literal legacy limb-amount/age arguments.
  The distortion's local `(0,0.5,0)` focus in that shell matrix is 1.5 above the entity,
  not 0.5 above it. Death rotation is included in the projected focus.
- Client death particles implement the retained EntityBlurFX behavior locally because the
  DragonAPI particle implementation is currently commented out. They use the original
  fade texture, random speed/size/lifetime, sine size curve and colliding motion.
- Native energy armor uses 26.3 ItemAccess, EnergyHandler and transactions. Incremental
  durability, UnbreakableArmor, and the legacy server PvP condition remain active.
- The optional Rotary entity factory is loaded by name only after mod presence is known.
  A direct subclass reference caused the JVM to resolve absent interfaces before executing
  the presence check; the standalone server test caught this.

## Custom-drop component format

The original Lua parser treats every brace as a block delimiter. JSON embedded directly in
a quoted Lua value is therefore not a valid format. Use a companion file inside
`config/Reika/Void Monster_CustomDrops/`:

```text
customDrop3 = {
    type = "customDrop3"
    item_stack_file = "fortune_axe.json"
    min = 1
    max = 1
    required_difficulty = 1.5
}
```

```json
{"id":"minecraft:diamond_axe","count":1,"components":{"minecraft:enchantments":{"minecraft:fortune":2}}}
```

Legacy `item = "minecraft:dye:15"` and `item_nbt` remain supported through the actual vanilla
data-fixer chain. `delegate:` lookups retain DragonAPI's delegate contract. Renamed external
mod registry IDs still need their modern names; no speculative ID mapping is introduced.
The Lua database already owns the anonymous root: do not wrap the named entries in another
pair of braces. DragonAPI's generated examples now use this same parseable form.

## Optional dependency gates — not complete

| Gate | Preserved behavior / dependency needed |
| --- | --- |
| MYSTCRAFT-PORT | Full original VoidMystPages source retained. Register the authored No Void Monster page, veto age spawning when it exists, and reject monster linking when a modern age/page/LinkEvent API lands. |
| FORESTRY-PORT | Full original VoidMonsterBee source retained. Species registration, traits, products, conditions and effects require a modern Forestry bee API. |
| THAUMCRAFT-PORT | Original aspect allocation retained in bootstrap notes. Temporary warp, warping/void-metal armor and weapon behavior use presence-gated DragonAPI bridge contracts; aspects/bridges still need their target APIs. |
| BLOODMAGIC-PORT | Presence-gated username SoulNetwork drain retained until the modern LP bridge is supplied. |
| MFR-PORT | Safari Net and AutoSpawner class blacklists retained in bootstrap notes, pending the actual capture/spawner APIs. |
| ENDERIO-PORT | Soul-vessel capture blacklist retained in bootstrap notes, pending the modern capture API. |
| ROTARY-PORT | Radar/turret integration works with current RotaryCraft. ItemVoidMetalRailgunAmmo itself has not landed; its exact nested ammo type check and quarter-health impact behavior remain written. |
| Other armor bridges | IC2 EU, Mekanism gas and ModularPowersuits special energy branches remain in the original helper source. Native FE and ordinary armor work; these absent external API contracts are not claimed ported. |

An installed optional mod with a missing required bridge fails explicitly rather than silently
pretending that its authored behavior ran. The default required runtime dependency remains
DragonAPI; RotaryCraft is optional.

## Validation — 2026-10-04

- `:VoidMonster:compileJava`: complete modern source set compiles against local 26.3 jars.
- `:VoidMonster:runServerData` and `:VoidMonster:runClientData`: pass. Generated loot, damage
  types/tags, language, sounds, post-chain JSON and test arena are included in resources.
- `:VoidMonster:test`: passes; world-policy/cooldown contract checks **22/22**.
- `:VoidMonster:runGameTest`: **9/9** required server contracts pass without RotaryCraft.
- `:VoidMonster:runGameTest -PvoidMonsterWithRotary`: **9/9** pass with RotaryCraft, including
  selection and operation of the radar adapter.
- Contracts cover capped/reflected damage, ghost regeneration, actual healing splash dispatch,
  persisted flags/difficulty/health, all authored loot/books, ghost loot suppression, canceled
  lights/mandatory glowstone consumption, original NBT conversion, and optional class loading.
- `:VoidMonster:build` and `:TestInstance:compileJava`: pass. The jar contains the modern
  classes and all eleven checked resource/metadata paths.
- Isolated `:VoidMonster:runClient` startup reached LWJGL graphics and OpenAL/resource
  initialization without VoidMonster loading errors. This is **not** an in-world visual
  comparison or a proof that every effect has executed on the GPU.
- Generate resources in a separate invocation before the first GameTest run: an already
  scheduled processResources task cannot copy files produced later in the same task graph.

Run logs are in the root `build/` directory, including `voidmonster-datagen.log`,
`voidmonster-tested-standalone.log`, `voidmonster-tested-rotary.log`,
`voidmonster-final-validation.log`, and `voidmonster-client-startup.log`.

## Resource provenance

Original flare/growth atlases, aura OGG, minimap icon and version properties were recovered
byte-for-byte from Reika's [Void Monster V33a release](https://www.curseforge.com/minecraft/mc-mods/void-monster/files/4721208).
The death fade sprite and legacy shader helper formulas came from the official
[DragonAPI V33b release](https://www.curseforge.com/minecraft/mc-mods/dragonapi/files/4722480).
Recovered assets are explicitly unignored under src/main/resources and retain the original
owner's copyright/license. The downloaded reference jars and extracted source/bytecode are
disposable build artifacts, not shipped legacy dependencies.

## 2026-10-04 — Nether singleton fix and ChromatiCraft integration

The reported four natural Nether monsters came from the tick-populated entity cache: a monster
above the build ceiling can be loaded without ever ticking, leaving the generator's cache empty.
Server queries now use the level's actual loaded entity lookup, and `spawnIfAbsent` checks it
immediately before insertion. The regression puts a live, unticked monster above the ceiling
and rejects four further spawn attempts. This prevents new natural duplicates; existing entities
and explicitly created ritual monsters are preserved.

Both nine-test server suites pass after this fix (`build/voidmonster-final-standalone-9.log` and
`build/voidmonster-final-rotary-9.log`). JUnit runs the 22 world-policy/cooldown assertions. The
standalone suite also parsed the three-entry legacy custom-drop fixture, including its modern
component companion; the temporary fixture was removed after validation. The lethal drain resets
26.3 LivingEntity's `damageCooldownTime`, distinct from Entity's generic invulnerability counter.

ChromatiCraft's active 26.3 module now implements the original corruption essence, bait, death fog,
altar, Nether trap/Overworld destruction ritual and their native client/data integration. See
[ChromatiCraft/PORTING.md](../ChromatiCraft/PORTING.md)'s compatibility entry for scope and tests.
