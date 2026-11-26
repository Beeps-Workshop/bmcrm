---
sidebar_position: 6
---

# Tuning Fork

A diagnostic tool that visualizes the mod's otherwise-invisible spatial rules — right-click with
it instead of your hand to inspect a block without opening its GUI.

## Crafting
```
  C
 SC
S
```
`S` = stick, `C` = copper ingot.

## Usage
- Right-click a **Resonator** or **Modulator** to toggle a wireframe of its area (scan box, Silk
  Touch footprint, Fortune "+", etc. — see each [Modulator](./Modulators/SilkTouch.md) for its
  shape).
- Right-click a **placed crystal** to check its status: it flashes the Resonator that owns it if
  claimed, tells you it's unlinked if not yet claimed, or tells you it's uninfused if it has no
  resource yet.

All of this is purely client-side feedback — it doesn't change how anything works, it just shows
you what's already happening.
