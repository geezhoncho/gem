# Magi: Djinn RPG — Technical Design (Forge 1.20.1)

Version 0.3.0. Target environment: **Soulrend v8.4** (Forge 47.x, Epic Fight 20.14.17, Iron's Spells 'n Spellbooks 3.15.6, Apotheosis 7.4.8 + Apothic Attributes, GeckoLib 4.8.4, TerraBlender, Sinytra Connector). The mod must also run without any optional mod.

## 0. Ground rules

1. **Ports & adapters.** Core code defines small interfaces with self-sufficient defaults; `compat.<mod>` packages implement them when that mod is present. Core never imports compat. A failing compat module disables itself instead of crashing launch.
2. **Data over code.** Djinn, dungeons, set bonuses, loot, worldgen and Epic Fight mappings are JSON. Java implements behaviour only.
3. **Server authoritative, no idle ticking.** Cooldowns are deadlines, energy regenerates lazily, sync happens on change.
4. **Integrate, don't compete.** One mana pool (Iron's), one affix system (Apotheosis), one combat system (Epic Fight).
5. **Save compatibility.** Registry ids and NBT keys are never renamed. New fields are optional on load. Network changes bump `MagiNetwork.PROTOCOL`.
6. **Art standard.** Every item ships a detailed, stylised Persian/Arabian + Magi-anime 3D model (see §18). No flat sprites.

## 1. Architecture

```
content  : items, weapons, armor, entities, boss scripts, spells, dungeon rooms, worldgen data
domain   : djinn, boss framework, dungeon generator, encounters, progression
core     : registry, config, networking, capability/SavedData, data loaders
compat   : irons | epicfight | apotheosis      (optional, isolated)
client   : rendering, model predicates, GUI, HUD, sound (reached only via client bus / DistExecutor)
tools    : model_gen.py (generates all item models, palettes, armor textures, previews)
```

## 2. Packages (`com.yourname.magi`)

Present: `MagiMod`, `config`, `capability`, `djinn`, `networking`, `client`, `armor`, `weapon`, `item`, `registry`, `command`, `compat.{irons,epicfight,apotheosis}`.
Planned: `spell` (P3), `world`/`biome` (P4), `dungeon`/`dimension` (P5), `boss`/`entity`/`entity.ai` (P6), `gui` (P8), `loot`.

## 3. Registries

`DeferredRegister` per type (`MagiItems`, `MagiWeapons`, `MagiCreativeTabs`; later Blocks, Entities, Menus, Sounds, Particles, Features, Structures). Weapons are registered as tier x archetype in a loop; armor sets via one `armorSet(...)` call each. Code-defined behaviours that datapacks reference (Djinn abilities, boss attacks, room logic) get custom Forge registries from P3.

## 4. Player & world state

| Scope | Holder | Contents |
|---|---|---|
| Player | capability `PlayerDjinnData` | owned Djinn + level/xp, equipped, energy stamp, cooldown deadlines, logout health |
| Server | `SavedData` `DungeonInstances` (P5) | cell allocations, seeds, room states, participants |
| Server | `SavedData` `RewardLedger` (P6) | encounter → per-player reward claims (anti-duplication) |
| Entity | boss entity + `SynchedEntityData` (P6) | phase, enrage, mechanic flags |

Capability data is copied on `Clone` (death and End return), re-applied on login/respawn/dimension change/reload.

## 5. Djinn system

- `DjinnDefinition` (JSON codec, `data/<ns>/djinn/*.json`): element, colour, max level, max energy, attribute bonuses (`base + per_level*(level-1)`), ability ids. Attributes of absent mods are skipped silently.
- `DjinnManager`: reload listener, atomic swap. Definitions are **synced to clients** on join and `/reload` (tooltips now, Djinn menu in P8).
- `DjinnEquipmentHandler`: the only mutator. Grant, revoke, equip, set level, login/logout. Applies transient modifiers with deterministic UUIDs and restores logout health after bonuses are back.
- Abilities (P3): keybind → C2S packet with an ability *slot* → server validates equipped Djinn, cooldown, energy → runs → syncs. C2S equip requests are rate-limited.
- **Mana vs energy:** Iron's mana pays for spells. Djinn Energy gates non-spell abilities. Without Iron's, spells fall back to Djinn Energy. Never two mana bars.
- Multiple Djinn: `maxEquipped` (default 1); extra slots share a global ability cooldown penalty.

## 6. Boss framework (P6)

- `MagiBossEntity` + `BossBrain` state machine; vanilla goals only for movement/targeting.
- Attacks are timelines: windup → telegraph → commit → active → recovery. Damage only at commit; heavy attacks telegraph ≥ 12 ticks.
- `TelegraphShape` (circle, ring, cone, line, rect, sweep) sent once per attack to trackers; server resolves hits geometrically; lingering hazards checked every ≥ 5 ticks.
- `EncounterScaler`: participants snapshotted at engage. 1 player: base mechanics. 2–3: added mechanics. 4+: raid mechanics. HP scaling small and configurable.
- **Models: GeckoLib** (already in Soulrend) becomes a required dependency from P6; bosses need real animation.
- Blue Djinn (Talmir, gravity): P1 melee + pulls; P2 large circles/rings; P3 arena collapses into gravity wells; P4 telegraphed arena-wide ultimate with safe pockets; final enrage.

## 7. Dungeons (P5)

- `DungeonDefinition` JSON: theme, tier, floors, room pools by role, mob/loot tables, boss id, dimension theme.
- `DungeonGraph` (pure Java, unit-testable): seeded graph — start → main path with branches → secrets → boss; key placement; difficulty ramp.
- Rooms are **built by code** from parametric templates (no hand-built NBT required); optional `.nbt` rooms can be added later.
- `RoomLogic` registry for puzzles/traps/encounters; rooms activate only when a player is inside.

## 8. Pocket dimensions (P5)

One datapack dimension per **theme** (`magi:pocket_lightning`, `_strength`, `_hellfire`, `_life`, `_fire`, `_gravity`) with its own sky/fog/light; void chunk generator; each run gets a **cell** inside it (allocator in SavedData, freed and cleared in batches). Per-run dynamic dimensions are avoided (unsafe at runtime).

## 9. Procedural generation

Seed = hash(world seed, entrance pos, run counter). Graph generation is instant and pure; block placement is budgeted per tick on the main thread; async only for pure computation.

## 10. Iron's Spells integration (P3)

**Implemented (0.3.0):** the Djinn Arts engine (`magic` package) runs every technique as a server timeline. Costs go through
`ManaProvider`; `compat.irons.IronsMana` spends Iron's mana via its public `MagicData` API looked up by reflection (no
compile dependency; any mismatch falls back to Djinn Energy). Damage reads Iron's `spell_power`, `<school>_spell_power`
and `cooldown_reduction` attributes by id. Requirements (Djinn equipped, sword in hand, Djinn level, full energy) are
checked server-side in `ArtCaster`.

**Next (3b):** compile against Iron's API (`code.redspace.io` maven, `irons_spellbooks:<ver>:api`) and register each art
as an Iron's spell (`DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, ...)`, `DefaultConfig`, `onCast` delegating
to the same art), so they also work from scrolls, spellbooks and Iron's spell wheel.

Original plan:

`ManaProvider` port (default: Djinn Energy). `compat.irons` implements it on Iron's player magic data and registers Magi spells as Iron's spells so they inherit mana, cooldowns, scaling, casting animations, scrolls. **P3 starts with a research spike against Iron's 3.15.6 sources/docs; nothing is guessed.**

## 11. Epic Fight integration

Datapack weapon capabilities in `data/magi/capabilities/weapons/<item>.json` — **confirmed working in-game (v0.1)**. Types: scimitar/saber → `epicfight:sword`, dagger → `epicfight:dagger`, spear → `epicfight:spear`, axe → `epicfight:axe`, bow → `epicfight:bow`. Next: test curved-blade types for the scimitar; chain weapons need a custom category (research first).

## 12. Apotheosis integration

Items extend vanilla classes (`SwordItem`, `AxeItem`, `BowItem`, `ArmorItem`) so Apotheosis categorises them for affixes. Loot tables contain base items; Apotheosis' own rules add rarity/affixes. No competing affix system.

## 13. Networking

Channel `magi:main`, protocol `"2"`. S2C: `SyncDjinnData` (player state), `SyncDjinnDefinitions`. C2S: `EquipDjinn` (validated + rate-limited). Future: telegraphs to trackers only; no per-tick packets.

## 14. Multiplayer

Server owns all state; instances have participants; rewards are computed server-side from contribution; reconnects resync fully. Dedicated-server safe: client classes only via client bus / DistExecutor.

## 15. Save data

Capability NBT keys: `Owned`, `Equipped`, `Energy`, `EnergyStamp`, `Cooldowns`, `StoredHealth` (all optional on load; invalid ids dropped, never crash).

## 16. World generation (P4)

Datapack biomes (Golden, Oasis, Ancient, Cursed, Djinn desert) added through **TerraBlender** regions (present in Soulrend; becomes a dependency in P4). Structure spacing configured to coexist with Yung's Better Desert Temples, Dungeons Arise, Dungeon Crawl, Cataclysm and Alex's Caves.

## 17. Loot

Loot tables per tier/room role; global loot modifiers for boss drops; Djinn Cores are personal rewards via the ledger. Materials (Djinn Essence, Ancient Gold, Magical Sand, Elemental Shards, Dungeon Steel, Spirit Crystal, Djinn Core) each gate a crafting tier; weapon repair ingredients switch to them when they exist.

## 18. Equipment & art pipeline

- Weapons: 6 archetypes x tiers (Desert Iron, Steel, Dungeon Steel now; Elemental, Djinn, Legendary later). Distinct speed/reach/damage + Epic Fight numbers.
- Armor: `MagiArmorMaterial` + `MagiArmorItem`; 30 sets in P7, each a material line + bonus data; data-driven 2/3/4-piece set bonuses.
- **3D models (`tools/model_gen.py`)**: every item is a multi-cuboid vanilla JSON model coloured from a 64x64 palette texture (8 shades x 16 materials, 4x4 swatches so mipmaps never bleed). Weapons are authored upright with the grip at y≈1.64 and share `magi:item/handheld_3d`, whose hand transforms equal vanilla's with z reduced by 45°. Bows use `magi:item/bow_3d` with 3 draw states. Armor and cores use `magi:item/prop_3d`. Djinn Cores switch model per Djinn via the `magi:djinn` item predicate. The script also paints the worn-armor textures and renders preview sheets. **Run it after any art change.**

## 19. Performance

No idle ticking; small state machines; one packet per telegraph; budgeted placement; pooled projectiles, capped summons, particle budgets; occupied-room-only logic; cached definitions swapped atomically.

## 20. Configuration

`magi-common.toml`: captureChance, energyRegen, maxEquipped, boss healthMultiplier, mana/spell multipliers, dungeonFrequency/spacing, lootQuality, armorBonusMultiplier. Content numbers live in data, not config.

## 21. Milestones

| Phase | Content | Status |
|---|---|---|
| 1 | Foundation: registries, config, capability, Djinn data, network, compat framework | Done, tested in Soulrend |
| 2a | 18 weapons + Epic Fight mappings | Done, tested in Soulrend |
| Revamp (0.2.0) | 3D models for all items, health fix, definition sync, tooltips, commands | Done, tested |
| 3a (0.3.0) | Djinn Arts engine + Baal benchmark (6 arts, HUD, keys, Extreme Magic) | Built, needs test |
| 3b | Register arts as Iron's spells (scrolls/spellbooks); other five Djinn's arts | Next |
| 4 | Desert biomes, bandit camps, mobs + AI | |
| 5 | Dungeons + pocket dimensions + puzzles | |
| 6 | Boss framework + six Djinn bosses | |
| 7 | 30 armor sets, more tiers, set bonuses, Apotheosis tuning | |
| 8 | Djinn menu, boss UI, dungeon HUD | |

## 22. Testing

Each phase: build → dedicated server boot → client boot → in-game checklist (`docs/TESTING.md`) in a copy of the Soulrend profile → also boot without optional mods. Pure logic (dungeon graph) gets unit tests when it exists.

## 23. Open uncertainties

| Area | Status |
|---|---|
| Forge 47 APIs used so far | Resolved: compiled and ran in Soulrend |
| Epic Fight weapon JSON path/format | Resolved: working in game |
| 3D hand transforms (`handheld_3d`, `bow_3d`) | Derived from vanilla's transforms; needs an in-game look, especially with Epic Fight |
| `BowItem.customArrow` hook (bow damage bonus) | Unverified; compiles either way |
| Iron's mana API (`MagicData` get/set mana) | Used via reflection with fallback; confirm the bridge log line in game |
| Iron's spell registration API | Docs confirmed (registry key, DefaultConfig, onCast); needs a compile against Iron's maven (3b) |
| `RenderType.entityTranslucentEmissive`, item `render_type` translucent | Believed correct for 1.20.1; first build will confirm |
| Epic Fight curved-blade/chain categories | Research before use |
| Apotheosis loot hooks | Not needed so far |

## 24. Dependencies

Required: Forge 47.x, Minecraft 1.20.1. Optional (`mandatory=false`, `ordering=AFTER`): Iron's Spells, Epic Fight, Apotheosis. Planned required: GeckoLib (P6), TerraBlender (P4) — both already in Soulrend. Compile-time optional dependencies are `compileOnly`, never bundled.
