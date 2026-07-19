# Research: Inter-mod compatibility & auto-registering ore crystals (1.21.1 / NeoForge 21.1.x)

> Research notes gathered during scaffolding.

## Verdict

**"Register a new dedicated ore-crystal Block+Item for every ore in every installed mod" is NOT robustly possible.** Block/Item registries **freeze** right after `RegisterEvent` (mod construction), long before the game knows about worlds, datapacks, or reliable tag contents. Late registration throws (`Registry is already frozen`) or breaks registry sync (client/server ID-mismatch kicks).

**What works instead:**
1. **Discovery** — after datapack load, enumerate ores via the **common `c:` tag conventions** (`c:ores`, `c:raw_materials/*`, `c:storage_blocks/*`). Zero per-mod code.
2. **One generic "crystal"** whose material identity lives in **data** (a `DataComponent` on the item + a field on the placed `BlockEntity`), not the registry. One registered block/item = unlimited materials.
3. **Data-driven loot** — the accumulator rolls datapack loot tables; ship/generate tables for known materials and let packs add more.
4. **Explicit opt-in API (IMC + JSON hooks)** so code-mods and pack authors register materials you'd otherwise miss.

⇒ You "gain content from whatever modpack you're in" via **tag-scanning + one data-driven generic crystal + an IMC/API escape hatch** — not dynamic block registration.

## Recommended architecture

- **Startup (`RegisterEvent`):** register exactly one of each — `CrystalBlock` + `BlockEntityType`, `CrystalItem` (BlockItem), `DataComponentType<ResourceLocation>` (`crystal_material`), the accumulator, and optionally a **datapack `Registry<CrystalMaterial>`** via `DataPackRegistryEvent.NewRegistry`.
- **Datapack load / `TagsUpdatedEvent`:** tags are now populated. Scan `c:ores` / `c:raw_materials/*` / `c:storage_blocks/*` → derive material set; merge IMC-registered materials; build an in-memory `MaterialCatalog` (materialId → display, icon item, loot table). Rebuild on every `TagsUpdatedEvent` (client + server).
- **Runtime:** player obtains a generic crystal with `crystal_material` set (e.g. `mekanism:osmium`); placed BE copies the id; accumulator scans neighbors, reads each `materialId`, resolves via catalog to a `ResourceKey<LootTable>`, rolls it.

**Modeling materials+loot — pick one as primary:**
- **A — Datapack `Registry<CrystalMaterial>`:** JSON at `data/<ns>/crystal_material/iron.json` = `{ resultItem, lootTable, tier }`. Fully data-driven, auto-synced, deterministic. Cleanest/tunable.
- **B — Pure tag-derived:** synthesize the material list from `c:raw_materials/<x>` and roll a generic loot table parameterized by the result item. Max auto-coverage, no per-material tuning.
- **Recommended: hybrid** — tag scan proposes materials, datapack-registry entries (shipped + author-authored) refine/override, IMC injects code-mod materials.

## Key APIs (21.1.x)

**Registration/lifecycle:** `DeferredRegister(.Blocks/.Items/.DataComponents)`, `RegisterEvent`, `DeferredHolder`, `BuiltInRegistries`, `Registries`, `neoforge.registries.datamaps` (Data Maps — attach data to *existing* registry entries via datapack), `DataPackRegistryEvent.NewRegistry`.

**Tags:** `TagKey<T>`, `ItemTags`/`BlockTags`, `net.neoforged.neoforge.common.Tags.Items`/`Tags.Blocks` (`ORES`, `RAW_MATERIALS`, `RAW_MATERIALS_IRON/GOLD/COPPER`, `STORAGE_BLOCKS`, …), `BuiltInRegistries.ITEM.getTag(TagKey)` / `.getOrCreateTag(...)`, `HolderLookup.Provider`, `net.neoforged.neoforge.event.TagsUpdatedEvent`.

**Data components:** `DataComponentType<T>`, `DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID)` + `registerComponentType(...)`, `ItemStack#get/set/update`, `Codec` / `StreamCodec` / `ByteBufCodecs`.

**Loot/datagen:** `LootTable`/`LootParams`/`LootContext`, `LootContextParamSets`/`LootContextParams`, `LootTableProvider`, `ResourceKey<LootTable>`.

**Soft-dep/IMC:** `neoforge.mods.toml` `[[dependencies.<modid>]]` (`type="optional"`, `ordering="AFTER"`), `ModList.get().isLoaded("mekanism")`, `InterModComms`, `InterModEnqueueEvent`/`InterModProcessEvent`, `@Mod`, `FMLLoader`.

## Code snippets

Reading a common ore tag after tags load:

```java
@SubscribeEvent
public void onTagsUpdated(TagsUpdatedEvent event) {
    BuiltInRegistries.ITEM.getTag(Tags.Items.ORES).ifPresent(holders -> {
        for (Holder<Item> h : holders) {
            ResourceLocation id = h.unwrapKey().orElseThrow().location();
            catalog.proposeMaterialFromOre(id, h.value());
        }
    });
    for (Holder<Item> raw : BuiltInRegistries.ITEM.getOrCreateTag(Tags.Items.RAW_MATERIALS)) {
        catalog.proposeMaterialFromRaw(raw);
    }
}
```

> Do **not** read tags in `FMLCommonSetupEvent`/`RegisterEvent` — not loaded yet. Use `TagsUpdatedEvent` / `ServerAboutToStartEvent` / `OnDatapackSyncEvent`.

Soft-dep guard (never hard-classload an absent mod):

```java
public static final boolean MEKANISM = ModList.get().isLoaded("mekanism");
if (MEKANISM) MekanismCompat.init(); // Mekanism-typed code isolated in that class only
```

IMC — let other mods register materials:

```java
// sender
InterModComms.sendTo("beepsresourcegenerators", "register_material",
    () -> new MaterialMessage(ResourceLocation.parse("mekanism:osmium"),
                              ResourceLocation.parse("mekanism:raw_osmium"),
                              ResourceLocation.parse("beepsresourcegenerators:crystal/osmium")));
// receiver
@SubscribeEvent public void processIMC(InterModProcessEvent event) {
    event.getIMCStream(m -> m.method().equals("register_material"))
         .map(m -> (MaterialMessage) m.messageSupplier().get())
         .forEach(catalog::registerExplicit);
}
```

> IMC runs during startup before tags/worlds exist — collect declarations, reconcile into the catalog at `TagsUpdatedEvent`.

Generic crystal via DataComponent (one item, unlimited materials):

```java
public static final DeferredRegister.DataComponents COMPONENTS =
    DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
public static final Supplier<DataComponentType<ResourceLocation>> CRYSTAL_MATERIAL =
    COMPONENTS.registerComponentType("crystal_material", b -> b
        .persistent(ResourceLocation.CODEC)
        .networkSynchronized(ResourceLocation.STREAM_CODEC));

ItemStack stack = new ItemStack(ModItems.CRYSTAL.get());
stack.set(CRYSTAL_MATERIAL.get(), ResourceLocation.parse("mekanism:osmium"));
```

Optional dependency in `neoforge.mods.toml`:

```toml
[[dependencies.beepsresourcegenerators]]
    modId = "mekanism"
    type = "optional"
    ordering = "AFTER"      # their tags/registries exist before ours resolve
    versionRange = ""
    side = "BOTH"
```

## Reliability of the tag approach

The `c:` convention is defined by NeoForge (`Tags`) and is the biggest zero-hardcoding lever, but only as good as each mod's compliance:
- Well-behaved mods (Mekanism, AE2, Create, Thermal, Industrial Foregoing, Modern Industrialization) populate `c:ores`/`c:raw_materials/*`/`c:storage_blocks/*`.
- **Namespace drift:** older/Fabric content sometimes still uses legacy `forge:ores`. Read both `c:` and legacy `forge:`; treat missing tags as "not offered." (The "Common Tags Fix" datapack backfills many.)
- Derive the **material key from the sub-tag folder** (`c:raw_materials/osmium` → `osmium`), not by string-munging item ids; fall back to id parsing only as last resort.
- Tags are datapack-synced to the client — the catalog stays deterministic if you rebuild on `TagsUpdatedEvent` on both sides and never bake material identity into a registry id.

## Determinism in modpacks

- **No dynamic-ID risk:** you register a fixed small set (one crystal block/item + components) regardless of pack contents — the core payoff of the generic-crystal design. Per-material blocks would desync/kick clients missing a mod.
- **Material identity is data** (a `ResourceLocation` in a component/BE + JSON) — content-addressed, stable across machines.
- Datapack registries (Option A) are synced and validated on join; bad entries fail loudly.
- **Escape hatch:** everything is JSON, so **KubeJS**/**CraftTweaker** can add/override materials + loot with no Java; KubeJS can add tag entries at runtime that your scan then picks up. Document a small "add a material" JSON schema for free pack support.

## Open design decisions

1. Catalog source of truth: tag-scan vs. datapack registry vs. **hybrid (recommended)**.
2. Material-key derivation: commit to `c:raw_materials/<name>`; define fallback for ores lacking a raw form (gems/redstone-likes).
3. Loot generation: per-material datagen tables (moddable) vs. one runtime-parameterized generic table (covers unknown mods). Note: fully *runtime-synthesized* loot tables aren't a first-class API — roll programmatically if going fully dynamic.
4. Data Maps vs. custom registry for per-ore yield/tier.
5. Balance: auto-adding every modded ore can trivialize progression — gate via tier tags / config allow-deny lists / cost scaling.
6. Client rendering of the generic crystal: tint/model-override by material component (`BlockColor` + item property or baked-model override).
7. IMC vs. a published `-api` jar (interface guarded by `ModList.isLoaded`) for deep integrations.

## Sources

- NeoForge — [Registries](https://docs.neoforged.net/docs/concepts/registries/), [Tags (1.21.1)](https://docs.neoforged.net/docs/1.21.1/resources/server/tags/), [Data Components](https://docs.neoforged.net/docs/items/datacomponents/), [Mod files / dependencies (1.21.1)](https://docs.neoforged.net/docs/1.21.1/gettingstarted/modfiles/), [Events](https://docs.neoforged.net/docs/concepts/events/), [Registry rework blog](https://neoforged.net/news/20.2registry-rework/)
- [`Tags.Items` javadoc](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.21.x-neoforge/net/neoforged/neoforge/common/Tags.Items.html)
- [Common Tags Fix datapack](https://modrinth.com/datapack/common_tags_fix), [MCreator commonly-used tags](https://mcreator.net/wiki/commonly-used-tags-minecraft-mods)

**Bottom line:** don't chase dynamic block registration. Register one generic component/BE-driven crystal, discover materials from `c:` tags at `TagsUpdatedEvent`, drive yields with datapack JSON (registry or data map), and expose IMC + JSON hooks. That combination is what makes the mod gain content from whatever modpack it's in.
