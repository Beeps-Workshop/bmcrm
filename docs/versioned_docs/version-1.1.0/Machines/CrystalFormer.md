---
sidebar_position: 2
---

# Crystal Former

Forms a blank crystal from a base material and a catalyst. The catalyst determines the crystal's
tier — see [Crystal Tiers](../Types/CrystalTiers.md) for the full base/catalyst/tier table.

## Slots
The Former has 4 slots: base, catalyst, output, and fuel.

## Forming
Each tick, the Former checks its base + catalyst pair against the known crystal tiers. If they
match a tier and fuel is available, progress advances; forming takes 2400 ticks (2 minutes) and
consumes one base and one catalyst, producing a blank crystal of the matched tier (tinted to that
tier's color).

If the pair stops matching a valid tier mid-way, progress resets to 0. If fuel simply runs out
while the pair is still valid, progress holds where it was rather than resetting.

## Fuel
See [Fuel & power](./Intro.md#fuel--power) — the Former burns solid fuel or RF/FE exactly like the
Crystal Infuser.
