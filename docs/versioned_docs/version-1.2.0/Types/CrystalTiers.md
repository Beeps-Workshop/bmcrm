---
sidebar_position: 1
---

# Crystal Tiers

A crystal's tier is determined by the base + catalyst pair used in the
[Crystal Former](../Machines/CrystalFormer.md), and sets the crystal's roll chance in the
[Resonator](../Machines/Resonator.md).

| Tier | Base             | Catalyst       | Roll chance | Resonance capacity | Color     |
|------|------------------|----------------|-------------|--------------------|-----------|
| 1    | Sand             | Glass          | 4%          | 250 mB             | `#C6E2E9` |
| 2    | Tinted Glass     | Amethyst Shard | 8%          | 500 mB             | `#B57EDC` |
| 3    | Shining Crystals | Sea Lantern    | 12%         | 1000 mB            | `#5AC8B8` |
| 4    | Pulsing Pearl    | Echo Shard     | 16%         | 2000 mB            | `#2E9AA6` |
| 5    | Resonating Gem   | Shulker Shell  | 20%         | 4000 mB            | `#A45FC0` |

Shining Crystals, Pulsing Pearls and Resonating Gems are
[resonance ingredients](./ResonanceIngredients.md) made in the Crystal Crucible. Any glass in the
`c:glass_blocks/cheap` tag works as the tier 1 catalyst.

Resonance capacity is the most Liquid Resonance a crystal of that tier can store — see
[The Crystal: Resonance](../Machines/PlacedCrystal.md#resonance).

Higher tiers cost rarer catalysts but roll noticeably more often — tier 5 rolls 5× as often as
tier 1.

:::info
Roll chance here is the base per-cycle chance before the Resonator's
[rain/mob penalties](../Machines/Resonator.md#work-cycle) are applied.
:::
