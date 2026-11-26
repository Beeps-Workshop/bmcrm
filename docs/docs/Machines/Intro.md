---
sidebar_position: 1
---

# About Machines

BMCRM's whole loop runs through three machines, used in order:

1. **Crystal Former** – forms a blank crystal from a base material + a catalyst.
2. **Crystal Infuser** – infuses a resource material into that blank crystal.
3. **Resonator** – places crystals to work, periodically rolling them for their resource.

## Order of operations
A crystal has to be formed, then infused, before a Resonator can do anything useful with it — an
un-infused blank crystal has no resource to roll for.

## Fuel & power
The Crystal Former and Crystal Infuser both burn fuel like a furnace: fuel only ignites once
there's actual work queued, and burns down every tick regardless. Both also accept RF/FE as
"wireless fuel" — energy fills a small internal buffer (`machineEnergyCapacity`) at a rate capped
by `machineEnergyMaxReceive`, and once full is spent as a single lump of burn time
(`machineEnergyBurnTicks`), preferred over solid fuel. See [Configuration](../Configuration.md)
for these values.

## Modulators
[Modulators](./Modulators/SilkTouch.md) are separate blocks placed near a Resonator that change
how it rolls or processes its crystals — see the Modulators section for each one.
