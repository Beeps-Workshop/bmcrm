---
sidebar_position: 3
---

# Crystal Infuser

Infuses a blank crystal with a resource, turning it into a crystal that a Resonator can actually
work for that resource.

## Slots
The Infuser has 3 slots: material, crystal, and fuel.

## Infusing
The Infuser feeds 1 unit of material into the crystal every 10 ticks; a crystal needs 32 units
total to finish infusing. Progress is tracked on the crystal item itself, shown as a durability-bar
style indicator, so you can pull a partially-infused crystal out and finish it later.

Once a crystal has taken its first bit of material, its resource target locks — only the same
material will continue feeding it.

## Material → resource detection
Material detection is tag-based, so not every mod's items are guaranteed to work:
1. If a datapack explicitly overrides the material, that takes priority.
2. Otherwise, the material's item tags are checked in order: `c:ingots` → `c:gems` → `c:dusts` →
   `c:raw_materials`. Whichever tag matches determines the resource, as long as a corresponding
   `c:ores/<material>` block actually exists and isn't on the ore blacklist (see
   [Configuration](../Configuration.md)).

## Fuel
See [Fuel & power](./Intro.md#fuel--power) — the Infuser burns solid fuel or RF/FE exactly like
the Crystal Former.
