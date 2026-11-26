---
sidebar_position: 6
---

# The Crystal

Once formed (and ideally infused), a crystal can be placed as a block — this is the form a
Resonator actually works.

## Placement
Crystals are face-attached blocks, like a wall torch — they need a solid surface to stick to and
pop off if that support is removed. A placed crystal keeps its tier and resource, so breaking and
re-placing it doesn't lose progress.

## Ownership
A placed crystal tracks which Resonator has claimed it. This is what the Resonator's Tuning Fork
preview visualizes, and what determines which Resonator's work cycle actually rolls it — see
[Resonator: Crystal ownership](./Resonator.md#crystal-ownership).

## Resonance
Every time a Resonator works a crystal, the crystal stores 1 mB of resonance, whether or not the
roll produced anything. It keeps storing until it reaches its tier's
[resonance capacity](../Types/CrystalTiers.md), and keeps what it has stored when broken and
re-placed. The tooltip shows how much it currently holds.

Melting a crystal in the [Crystal Crucible](./CrystalCrucible.md) turns its stored resonance into
Liquid Resonance — but uses up the crystal.
