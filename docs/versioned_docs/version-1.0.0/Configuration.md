---
sidebar_position: 4
---

# Configuration

BMCRM ships a common config file at `config/bmcrm-common.toml`, generated the first time the mod
loads. Please take care when editing this file, as it may become corrupted if you use invalid TOML
formatting.

### blacklistedOres
```toml
blacklistedOres = ["minecraft:nether_gold_ore"]
```
Ore block ids that crystals should never generate from, and that won't be offered as crystals.
Use this for any ores you do not want to be farmable via Resonators. The default blacklist includes Nether Gold Ore, which is an exception to how ores work normally.

### rainEfficiency
```toml
rainEfficiency = 0.5
```
Roll-chance multiplier for a crystal exposed to rain (or whose Resonator is rained on). `1.0`
disables the penalty entirely; `0.5` halves the effective roll chance. Range: `0.0`–`1.0`.

### mobEfficiency
```toml
mobEfficiency = 0.5
```
Roll-chance multiplier while a hostile mob is near the Resonator. `1.0` disables the penalty
entirely; `0.5` halves the effective roll chance. Range: `0.0`–`1.0`.

### mobDisruptionRadius
```toml
mobDisruptionRadius = 6
```
How many blocks around the Resonator are scanned for hostile mobs. Range: `0`–`32`.

### maxFortuneLevel
```toml
maxFortuneLevel = 5
```
Cap on the effective Fortune level from stacked Fortune modulators (`0` = uncapped). Range:
`0`–`100`.

### workIntervalTicks
```toml
workIntervalTicks = 400
```
Ticks between Resonator work cycles (20 ticks = 1 second). Higher values mean slower generation.
Range: `20`–`24000`.

### machineEnergyCapacity
```toml
machineEnergyCapacity = 1000
```
Small internal RF/FE buffer on the Crystal Former and Infuser. When it fills, it's spent as
fuel (see `machineEnergyBurnTicks`).

### machineEnergyBurnTicks
```toml
machineEnergyBurnTicks = 200
```
Burn time (in ticks) a machine gains each time it converts a full energy buffer into fuel — the
RF-powered equivalent of igniting a solid fuel item.

### machineEnergyMaxReceive
```toml
machineEnergyMaxReceive = 256
```
Max RF/FE a machine accepts per tick from an adjacent cable (`0` = no incoming-rate limit).
