# Test checklist — v0.3.0 (Baal benchmark)

Use a COPY of your Soulrend profile and a test world: Extreme Magic carves a real crater.
Delete older `magi-*.jar` files first. Client and server must both run 0.3.0.

## Setup
1. `/magi djinn grant @s magi:baal` (auto-equips). A gold-framed panel appears bottom-left: Baal Lv1, energy bar, selected art, Lightning Dash.
2. Keys (Options > Controls > "Magi: Djinn RPG"): **G** cast selected art, **H** next art, **J** Lightning Dash. Rebind any that clash with other mods.

## Each art (stand near a few mobs; `/summon minecraft:husk` works)
3. Lightning Spear — instant piercing bolt along your aim.
4. Chain Lightning — aim at a mob: lightning hops to nearby mobs, weaker each jump.
5. Thunder Cage — ring of lightning pillars where you aim; mobs inside are shocked, slowed and can't walk out.
6. Bararaq Saiqa — try first with NO sword (should refuse), then with any sword: lightning strikes your blade, you charge ~1.5 s, then a huge blast fires where you look and the sky flashes.
7. Lightning Dash (J) — fast dash that shocks what you pass through; uses Djinn Energy.
8. Extreme Magic: `/magi djinn setlevel @s magi:baal 5`, wait for a full energy bar, hold a sword, aim at open ground 20–60 blocks away:
   magic circle in the sky, warning ring on the ground, lightning storm with marked strikes, a colossal blue blade falls, impact, crater.

## Integration
9. With Iron's Spells: casting drains your Iron's mana bar; with too little mana you get "Not enough mana". (The bar may update a moment later.)
10. Iron's lightning spell power gear should increase damage.
11. Claims (Open Parties and Claims): the crater must not break blocks inside someone else's claim.
12. Config `config/magi-common.toml` > `magic`: `allowBlockDamage=false` or `craterRadius=0` disables the crater.

## Report back
- What looked great, what looked off (screenshots or a short video of Saiqa and Extreme Magic help most).
- Any lag spike during Extreme Magic (the pack has `spark`: `/spark profiler` around the cast).
- Anything in `logs/latest.log` mentioning `magi`, especially "Iron's Spells mana bridge".
