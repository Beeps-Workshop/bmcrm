---
sidebar_position: 5
---

# Datapacks

Crystal tiers, Former and Crucible recipes, and material detection can all be changed with a
datapack.

## Crystal tiers
Tiers live in `data/<namespace>/bmcrm/crystal_tier/<name>.json`. BMCRM's own tiers are in the
`bmcrm` namespace (`glass`, `amethyst`, `prismarine`, `echo`, `shulker`), so a file with the same
path overrides one.

```json
{
  "level": 1,
  "color": "#C6E2E9",
  "roll_chance": 0.05,
  "base_ingredient": { "tag": "minecraft:sand" },
  "catalyst_ingredient": { "tag": "c:glass_blocks/cheap" },
  "resonance_capacity": 250
}
```

| Field                 | Required | Default   | Description                                              |
|-----------------------|----------|-----------|----------------------------------------------------------|
| `level`               | yes      |           | Tier number, used for ordering                           |
| `base_ingredient`     | yes      |           | Base slot ingredient in the Crystal Former               |
| `catalyst_ingredient` | yes      |           | Catalyst slot ingredient in the Crystal Former           |
| `roll_chance`         | no       | `0.05`    | Chance per work cycle to generate, `0.0`–`1.0`           |
| `resonance_capacity`  | no       | `0`       | Most resonance (mB) a crystal of this tier can store     |
| `color`               | no       | `#FFFFFF` | Tint of the crystal                                      |
| `hidden`              | no       | `false`   | Hide the tier from JEI, Ponder and the creative tab      |

A tier file that fails to parse is logged and skipped instead of stopping the world from loading.
An empty JSON object does **not** remove a tier — to remove one, override it with:

```json
{ "neoforge:conditions": [{ "type": "neoforge:false" }] }
```

## Forming recipes
The Crystal Former can make other items with `bmcrm:forming` recipes:

```json
{
  "type": "bmcrm:forming",
  "base_ingredient": { "item": "minecraft:copper_ingot" },
  "catalyst_ingredient": { "tag": "c:gems/amethyst" },
  "result": { "id": "bmcrm:resonating_gem", "count": 2 },
  "duration": 600
}
```

`duration` is in ticks and defaults to `2400` (2 minutes). Crystal tiers are checked first, so a
recipe using the same base + catalyst as a tier can never be made; these are listed as a warning
in the log when the server starts.

## Resonance infusion recipes
The items the [Crystal Crucible](./Machines/CrystalCrucible.md) makes come from
`bmcrm:resonance_infusion` recipes:

```json
{
  "type": "bmcrm:resonance_infusion",
  "ingredient": { "item": "minecraft:lapis_lazuli" },
  "resonance": 25,
  "result": { "id": "bmcrm:activated_lapis_lazuli", "count": 1 }
}
```

`resonance` is the Liquid Resonance cost in mB.

## Infusion overrides
If a material isn't detected correctly by the [Crystal Infuser](./Machines/CrystalInfuser.md#material--resource-detection),
the `bmcrm:infusion_override` data map sets its resource directly. It goes in
`data/bmcrm/data_maps/item/infusion_override.json`:

```json
{
  "values": {
    "minecraft:raw_iron": { "type": "ore_tag", "tag": "c:ores/iron" },
    "#c:gems/quartz": { "type": "block", "block": "minecraft:nether_quartz_ore" }
  }
}
```

The resource `type` is one of:
- `ore_tag` — `tag`: a block tag of ores to pick from
- `block` — `block`: a single block id
- `entity` — `entity`: a mob id (see [Mob Crystals](./Types/MobCrystals.md))

## Mob crystals
Mobs in the `bmcrm:no_drops` entity type tag can't be turned into
[mob crystals](./Types/MobCrystals.md). Add to it for modded mobs that don't drop anything.
