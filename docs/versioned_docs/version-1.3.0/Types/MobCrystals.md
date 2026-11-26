---
sidebar_position: 3
---

# Mob Crystals

A blank crystal can be attuned to a mob instead of a material. A Resonator working a mob crystal
rolls that mob's loot table, so it can generate anything the mob can drop — a zombie crystal
gives rotten flesh, but also the occasional iron ingot, carrot or potato.

Mob crystals aren't made in the [Crystal Infuser](../Machines/CrystalInfuser.md). There are two
ways to attune one, and both need a **single** blank crystal, not a stack.

## From a spawner
Right-click a spawner with a blank crystal. The crystal is attuned to the spawner's mob straight
away, and the spawner is left empty (it can be set again with a spawn egg).

## By killing mobs
Hold a blank crystal in your off hand and kill the mob you want. The first kill locks the crystal
to that mob; kills of other mobs don't count after that. Once you've killed `mobCrystalKills`
(16 by default) of them, the crystal is attuned. Progress shows on the crystal like an infusion.

## Modulators
Mob crystals aren't affected by [Silk Touch](../Machines/Modulators/SilkTouch.md) or
[Fortune](../Machines/Modulators/Fortune.md) modulators.
[Auto-Smelt](../Machines/Modulators/AutoSmelt.md) still smelts their drops.

## Which mobs work
- Bosses are blocked by the `mobCrystalBlacklist` config option (Wither, Warden and Ender Dragon by
  default) — see [Configuration](../Configuration.md#mobcrystalblacklist).
- Mobs that don't drop anything are excluded through the `bmcrm:no_drops` entity type tag. This is
  best effort, so a modded mob without drops may still show up.
- Non-mob entities (villagers, iron golems, armor stands, boats and so on) can't be used.
