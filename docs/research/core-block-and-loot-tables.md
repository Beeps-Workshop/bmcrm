# Research: Accumulator core block + crystals + loot tables (1.21.1 / NeoForge 21.1.x)

> Research notes gathered during scaffolding. Verify API identifiers against the
> Parchment-mapped workspace before relying on exact names.

## 0. Recommended architecture (summary)

- **One `AccumulatorBlock`** (`Block implements EntityBlock`) + **`AccumulatorBlockEntity`** + a server-side **`BlockEntityTicker`**. The ticker does all work and early-returns on the client.
- **Neighborhood scan** modeled on the vanilla enchanting table: a precomputed list of `BlockPos` offsets, scanned only every N ticks (not every tick), guarded by chunk-load checks, results cached in the BE.
- **Each crystal contributes a `ResourceKey<LootTable>`.** On each work interval the accumulator resolves those tables from `server.reloadableRegistries()`, rolls each with a `LootParams` built for a no-attacker context, and pushes results into an internal `ItemStackHandler` exposed as an `IItemHandler` capability.
- **Persistence via NBT** (`saveAdditional`/`loadAdditional` with `HolderLookup.Provider`). DataComponents are for item/drop-facing data, not live machine state. Sync to client only what you render.
- **Registration** via `DeferredRegister` for `Block`, `Item` (BlockItem), `BlockEntityType`, optional `MenuType`; capabilities via `RegisterCapabilitiesEvent`.

## 1. Core block + ticking

Block implements `EntityBlock`, provides `newBlockEntity`, returns a ticker from `getTicker` using the vanilla `createTickerHelper` idiom, server-only:

```java
public class AccumulatorBlock extends Block implements EntityBlock {
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AccumulatorBlockEntity(pos, state);
    }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBE.ACCUMULATOR.get(), AccumulatorBlockEntity::serverTick);
    }
    @Nullable @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> type, BlockEntityType<E> check, BlockEntityTicker<? super E> ticker) {
        return check == type ? (BlockEntityTicker<A>) ticker : null;
    }
}
```

`serverTick` runs **every tick** — gate heavy work behind counters (`tickCounter % workInterval`, `tickCounter % RESCAN_INTERVAL`).

Save/load uses the 1.21 `HolderLookup.Provider` overloads; call `setChanged()` after mutating persistent state. Keep internal machine state (progress, cached crystal list, counters) in NBT; only implement the implicit-component hooks (`applyImplicitComponents` / `collectImplicitComponents` / `removeComponentsFromTag`) if you want inventory to survive block pickup.

## 2. Enchanting-table-style neighborhood scan

Vanilla logic lives in `EnchantmentMenu#slotsChanged` (not a BE ticker): a static `List<BlockPos> BOOKSHELF_OFFSETS` (ring 2 blocks out laterally, at Y and Y+1) + a per-offset predicate `EnchantingTableBlock.isValidBookShelf` that checks the block at the offset advertises `state.getEnchantPowerBonus(level, pos)` **and** the intervening block (one step toward the table) is empty. Vanilla caps power at 15.

Adapt as a tick-driven radius scan with caching:

```java
private static final int RADIUS = 4;
private static final int RESCAN_INTERVAL = 40; // 2s @ 20 TPS

private void rescanCrystals(Level level, BlockPos center) {
    List<ResourceKey<LootTable>> tables = new ArrayList<>();
    for (BlockPos p : BlockPos.betweenClosed(center.offset(-RADIUS,-RADIUS,-RADIUS),
                                             center.offset( RADIUS, RADIUS, RADIUS))) {
        if (!level.isLoaded(p)) continue;                 // chunk-load safety
        if (level.getBlockState(p).getBlock() instanceof CrystalBlock crystal) {
            tables.add(crystal.getLootTableKey(level.getBlockState(p)));
        }
    }
    this.cachedTables = tables;
    recomputeModifiers();
}
```

- `BlockPos.betweenClosed` reuses a mutable cursor — `.immutable()` before storing.
- Scan on an interval, not every tick (R=4 → 729 `getBlockState` calls).
- Always guard reads with `level.isLoaded(pos)`; reading unloaded chunks from a tick can force generation.
- Optional optimization: event-driven cache invalidation from crystal `neighborChanged`/`onPlace`/`onRemove`.
- Cache only lightweight data (keys, counts, modifier values) — never `BlockState`/`BlockEntity` refs.

## 3. Loot tables as roll source

In 1.21 loot tables are a datapack **registry** (`Registries.LOOT_TABLE`) → reference via `ResourceKey<LootTable>`:

```java
public static final ResourceKey<LootTable> CRYSTAL_IRON =
    ResourceKey.create(Registries.LOOT_TABLE,
        ResourceLocation.fromNamespaceAndPath(MODID, "gen/crystal_iron"));
```

Resolve + roll on the server:

```java
LootParams params = new LootParams.Builder(serverLevel)
    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
    .withLuck(this.luck)
    .create(LootContextParamSets.CHEST);       // no entity required

for (ResourceKey<LootTable> key : cachedTables) {
    LootTable table = server.reloadableRegistries().getLootTable(key); // EMPTY if missing
    for (ItemStack st : table.getRandomItems(params)) insertOutput(st); // applies Global Loot Modifiers
}
```

- `getRandomItems` applies Global Loot Modifiers; `getRandomItemsRaw` bypasses them.
- **Cache keys, not tables** — datapack `/reload` replaces the reloadable registry; resolve fresh each cycle (cheap map lookup).

Data-driven JSON (path is **singular** `loot_table` in 1.21):
`src/main/resources/data/<modid>/loot_table/gen/crystal_iron.json`

```json
{
  "type": "minecraft:chest",
  "pools": [{
    "rolls": 1, "bonus_rolls": 0.0,
    "entries": [
      { "type": "minecraft:item", "name": "minecraft:raw_iron", "weight": 10, "quality": 1 },
      { "type": "minecraft:item", "name": "minecraft:iron_nugget", "weight": 3 }
    ]
  }]
}
```

Top-level `"type"` must match the `LootContextParamSet` passed at roll time.

## 4. Combining tables + modifiers

- **Simple aggregation (recommended):** roll each crystal's table, concatenate `List<ItemStack>`, insert. Cross-crystal weighting is emergent.
- **Weighted single pool:** one table whose entries are `minecraft:loot_table` references with weights; roll once.

Modifier levers (composable):
1. **Luck / `bonus_rolls`** — `.withLuck(luck)`; pool rolls = `rolls + bonus_rolls*luck`, entry weight = `weight + quality*luck`. Cleanest "better loot."
2. **Extra whole rolls** — loop `getRandomItems` N times. Coarse, table-agnostic.
3. **Faster cadence** — lower `workInterval`. No loot change.

Compute `luck`/`extraRolls`/`workInterval` once in `recomputeModifiers()` after each rescan.

Output: `ItemStackHandler`; insert via `ItemHandlerHelper.insertItemStacked(handler, stack, false)`; non-empty remainder ⇒ full (pause/void). Override `onContentsChanged` → `setChanged()`.

## 5. Registration boilerplate (21.1.x)

```java
public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
public static final DeferredRegister.Items  ITEMS  = DeferredRegister.createItems(MODID);
public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
    DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
public static final DeferredRegister<MenuType<?>> MENUS =
    DeferredRegister.create(Registries.MENU, MODID);

public static final DeferredBlock<Block> ACCUMULATOR =
    BLOCKS.registerBlock("accumulator", AccumulatorBlock::new,
        BlockBehaviour.Properties.of().strength(3.5f).requiresCorrectToolForDrops());
public static final DeferredItem<BlockItem> ACCUMULATOR_ITEM =
    ITEMS.registerSimpleBlockItem("accumulator", ACCUMULATOR);
public static final Supplier<BlockEntityType<AccumulatorBlockEntity>> ACCUMULATOR_BE =
    BLOCK_ENTITIES.register("accumulator",
        () -> BlockEntityType.Builder.of(AccumulatorBlockEntity::new, ACCUMULATOR.get()).build(null));
```

Capabilities (`RegisterCapabilitiesEvent`, mod bus):

```java
event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBE.ACCUMULATOR_BE.get(),
    (be, side) -> be.getOutputHandler());
```

## 6. Pitfalls

1. **Server-only ticking** — return `null` from `getTicker` on client; loot rolling needs `ServerLevel`.
2. **Never cache a `LootTable`** across `/reload`; cache keys, resolve fresh.
3. **Loot context with no attacker** — use `LootContextParamSets.CHEST` (needs only `ORIGIN`, supports luck) or `EMPTY`. Avoid `BLOCK`/`ENTITY` (require tool/entity). Use `withOptionalParameter` for maybe-missing values.
4. **Param-set / `type` mismatch** crashes at load/roll — keep JSON `"type"` and `LootContextParamSets.X` in lockstep.
5. **Client/server sync** — nothing syncs by default. If you render state, implement `getUpdateTag`/`handleUpdateTag` + `getUpdatePacket`/`onDataPacket`, trigger via `level.sendBlockUpdated(...)`. Sync only rendered state; GUIs read via `Menu`/`ContainerData`.
6. **Chunk loading during scan** — guard with `level.isLoaded(pos)`.
7. **Global Loot Modifiers** run under `getRandomItems` (usually desirable).
8. **`setChanged()` discipline** — call exactly when persistent fields change.

## Open design decisions

1. Per-crystal table vs. composite weighted table (recommend per-crystal first).
2. Rescan cadence: fixed interval vs. event-driven invalidation (interval first).
3. Range shape: cube vs. shell vs. enchanting-style offset list with blocked checks.
4. Modifier delivery: placed modifier blocks vs. an upgrade inventory in the BE.
5. Loot param set / JSON type: CHEST vs. EMPTY vs. a custom `LootContextParamSet`.
6. Roll scaling: luck-driven vs. explicit extra rolls vs. faster cadence.
7. Output pressure / energy: FE cost? pause vs. void on full.
8. Balance cap: max effective crystal count / diminishing returns (vanilla caps at 15).
9. Persist cached tables vs. recompute on load (recompute is self-healing).

## Sources

- NeoForge 1.21.1 — [Block Entities](https://docs.neoforged.net/docs/1.21.1/blockentities/), [Loot Tables](https://docs.neoforged.net/docs/1.21.1/resources/server/loottables/), [Loot Conditions](https://docs.neoforged.net/docs/1.21.1/resources/server/loottables/lootconditions/), [Loot Functions](https://docs.neoforged.net/docs/1.21.1/resources/server/loottables/lootfunctions/)
- NeoForge — [Capabilities](https://docs.neoforged.net/docs/1.20.6/datastorage/capabilities/), [capability rework](https://neoforged.net/news/20.3capability-rework/), [Enchantments](https://docs.neoforged.net/docs/resources/server/enchantments/)
- [Minecraft Wiki — Enchanting table mechanics](https://minecraft.wiki/w/Enchanting_table_mechanics)
- [1.20.6 → 1.21 Migration Primer](https://docs.neoforged.net/primer/docs/1.21/) (loot tables became a registry; `loot_tables` → `loot_table`)
- Community source worth reading: McJty tutorials (mcjty.eu), Mekanism / Modern Industrialization / Industrial Foregoing on GitHub.
