---
sidebar_position: 5
---

# Crystal Crucible

Melts crystals down into Liquid Resonance, then spends that fluid turning ordinary items into
[resonance ingredients](../Types/ResonanceIngredients.md).

## Crafting
```
G G
SCS
SBS
```
`G` = light weighted pressure plate, `S` = copper ingot, `C` = cauldron, `B` = blast furnace.

## Slots
The Crucible has 4 slots: crystal, input, output, and fuel, plus an internal tank holding up to
4000 mB of Liquid Resonance.

## Melting
Any crystal holding resonance (see [The Crystal: Resonance](./PlacedCrystal.md#resonance)) can be
melted. Melting takes 200 ticks (10 seconds) no matter how much the crystal holds, consumes the
crystal, and adds everything it held to the tank. Crystals holding nothing are not accepted.

A melt is all-or-nothing: if the tank doesn't have room for the crystal's full charge, the
Crucible waits instead of starting.

## Input slot
The input slot is served instantly and doesn't need fuel:
- an **empty bucket** is filled with 1000 mB from the tank
- a **Liquid Resonance bucket** is emptied into the tank
- an item with a [resonance infusion recipe](../Types/ResonanceIngredients.md) is converted, as
  long as the tank holds enough Liquid Resonance

## Automation
- Top: accepts crystals, input items and fuel
- Sides: accept input items and fuel
- Bottom: extracts from the output slot

Pipes can fill and drain the tank from any side.

## Fuel
See [Fuel & power](./Intro.md#fuel--power) — the Crucible burns solid fuel or RF/FE exactly like
the Crystal Former and Infuser. Only melting uses fuel.
