---
sidebar_position: 2
---

# Crystal Former

Forms a blank crystal from a base material and a catalyst. The catalyst determines the crystal's
tier — see [Crystal Tiers](../Types/CrystalTiers.md) for the full base/catalyst/tier table.

## Slots
The Former has 3 slots: base, catalyst, and output.

## Forming
Each tick, the Former checks its base + catalyst pair against the known crystal tiers. If they
match a tier, progress advances; forming takes 2400 ticks (2 minutes) and
consumes one base and one catalyst, producing a blank crystal of the matched tier (tinted to that
tier's color).

If the pair stops matching a valid tier mid-way, progress resets to 0.
