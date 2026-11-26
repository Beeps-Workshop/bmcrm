---
sidebar_position: 4
---

# Resonator

Puts nearby crystals to work, periodically rolling each one for its resource.

## Scan range
The Resonator scans a box 5 blocks out horizontally, 5 up and 2 down. It rescans automatically
whenever a crystal or modulator is placed or broken anywhere (not just nearby), and otherwise
falls back to a periodic rescan every 200 ticks (10 seconds) as a safety net.

## Work cycle
Every `workIntervalTicks` (400 by default, i.e. 20 seconds — see
[Configuration](../Configuration.md)), each crystal the Resonator has claimed rolls its tier's
chance to generate its resource. That roll chance is reduced:
- to `rainEfficiency` (0.5 by default) if the crystal or the Resonator is rained on
- to `mobEfficiency` (0.5 by default) if a hostile mob is within `mobDisruptionRadius` (6 blocks
  by default) of the Resonator

On a successful roll, the Resonator picks a random block from the resource's ore tag (skipping any
[blacklisted ores](../Configuration.md#blacklistedores)), breaks it with a synthetic Netherite
Pickaxe — Silk Touch if a [Silk Touch modulator](./Modulators/SilkTouch.md) covers it, otherwise
Fortune equal to the number of overlapping
[Fortune modulators](./Modulators/Fortune.md) (capped by `maxFortuneLevel`) — auto-smelts the
result if an [Auto-Smelt modulator](./Modulators/AutoSmelt.md) applies, and drops the output into
a 15-slot internal buffer that a hopper (or comparator) can read from.

## Crystal ownership
An unclaimed crystal is claimed by the first Resonator whose scan reaches it. If two live
Resonators end up both resonating the same crystal, the crystal is destroyed and drops as an item
— so keep Resonators spaced out if you're chaining multiple crystals.

## Feedback
Successful cycles are mostly cosmetic flair: a shockwave ring, a staggered "pling" per crystal,
note-block chimes, and a burst of colored dust (red if the result was auto-smelted). None of this
affects the roll itself — it's just so you can tell when something happened.
